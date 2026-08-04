package com.kinhduanpc.service;

import com.kinhduanpc.entity.*;
import com.kinhduanpc.exception.AppException;
import com.kinhduanpc.repository.*;
import lombok.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CartService {

    private final CartRepository cartRepo;
    private final ProductRepository productRepo;
    private final UserRepository userRepo;
    private final PromotionService promotionService;

    public CartResponse getCart(Long userId, String sessionId) {
        Cart cart = findOrCreateCart(userId, sessionId);
        return toResponse(cart);
    }

    public CartResponse addItem(Long userId, String sessionId, Long productId, int quantity) {
        Cart cart = findOrCreateCart(userId, sessionId);
        Product product = productRepo.findById(productId)
            .orElseThrow(() -> AppException.notFound("Sản phẩm"));

        if (!product.getIsActive()) {
            throw AppException.badRequest("PRODUCT_INACTIVE", "Sản phẩm không còn hoạt động");
        }
        if (product.getStockQty() < quantity) {
            throw AppException.badRequest("INSUFFICIENT_STOCK",
                "Chỉ còn " + product.getStockQty() + " sản phẩm trong kho");
        }

        Optional<CartItem> existing = cart.getItems().stream()
            .filter(i -> i.getProduct().getId().equals(productId)).findFirst();

        if (existing.isPresent()) {
            CartItem item = existing.get();
            int newQty = item.getQuantity() + quantity;
            if (newQty > product.getStockQty()) {
                throw AppException.badRequest("INSUFFICIENT_STOCK",
                    "Chỉ còn " + product.getStockQty() + " sản phẩm trong kho");
            }
            item.setQuantity(newQty);
        } else {
            CartItem newItem = CartItem.builder()
                .cart(cart).product(product)
                .quantity(quantity).unitPrice(product.getPrice())
                .build();
            cart.getItems().add(newItem);
        }

        cart.setUpdatedAt(LocalDateTime.now());
        cartRepo.save(cart);
        return toResponse(cart);
    }

    public CartResponse updateItem(Long userId, String sessionId, Long productId, int quantity) {
        Cart cart = findOrCreateCart(userId, sessionId);

        if (quantity <= 0) {
            return removeItem(userId, sessionId, productId);
        }

        CartItem item = cart.getItems().stream()
            .filter(i -> i.getProduct().getId().equals(productId))
            .findFirst()
            .orElseThrow(() -> AppException.notFound("Sản phẩm trong giỏ hàng"));

        Product product = item.getProduct();
        if (quantity > product.getStockQty()) {
            throw AppException.badRequest("INSUFFICIENT_STOCK",
                "Chỉ còn " + product.getStockQty() + " sản phẩm");
        }

        item.setQuantity(quantity);
        cartRepo.save(cart);
        return toResponse(cart);
    }

    public CartResponse removeItem(Long userId, String sessionId, Long productId) {
        Cart cart = findOrCreateCart(userId, sessionId);
        cart.getItems().removeIf(i -> i.getProduct().getId().equals(productId));
        cartRepo.save(cart);
        return toResponse(cart);
    }

    public CartResponse clearCart(Long userId, String sessionId) {
        Cart cart = findOrCreateCart(userId, sessionId);
        cart.getItems().clear();
        cartRepo.save(cart);
        return toResponse(cart);
    }

    /** Merge giỏ hàng guest vào tài khoản sau khi đăng nhập */
    public void mergeCart(Long userId, String sessionId) {
        if (sessionId == null) return;

        Optional<Cart> guestCartOpt = cartRepo.findBySessionId(sessionId);
        if (guestCartOpt.isEmpty()) return;

        Cart guestCart = guestCartOpt.get();
        Cart userCart = cartRepo.findByUserId(userId).orElseGet(() -> {
            Cart c = Cart.builder()
                .user(userRepo.findById(userId).orElseThrow())
                .expiresAt(LocalDateTime.now().plusDays(30))
                .build();
            return cartRepo.save(c);
        });

        for (CartItem guestItem : guestCart.getItems()) {
            boolean exists = userCart.getItems().stream()
                .anyMatch(i -> i.getProduct().getId().equals(guestItem.getProduct().getId()));
            if (!exists) {
                CartItem newItem = CartItem.builder()
                    .cart(userCart)
                    .product(guestItem.getProduct())
                    .quantity(guestItem.getQuantity())
                    .unitPrice(guestItem.getUnitPrice())
                    .build();
                userCart.getItems().add(newItem);
            }
        }
        cartRepo.save(userCart);
        cartRepo.delete(guestCart);
    }

    private Cart findOrCreateCart(Long userId, String sessionId) {
        if (userId != null) {
            return cartRepo.findByUserId(userId).orElseGet(() ->
                cartRepo.save(Cart.builder()
                    .user(userRepo.findById(userId).orElseThrow())
                    .expiresAt(LocalDateTime.now().plusDays(30))
                    .build()));
        }
        return cartRepo.findBySessionId(sessionId).orElseGet(() ->
            cartRepo.save(Cart.builder()
                .sessionId(sessionId)
                .expiresAt(LocalDateTime.now().plusDays(30))
                .build()));
    }

    private CartResponse toResponse(Cart cart) {
        List<CartItemDto> items = cart.getItems().stream().map(i -> {
            Product p = i.getProduct();
            return CartItemDto.builder()
                .productId(p.getId()).productName(p.getName())
                .productSlug(p.getSlug()).thumbnail(p.getThumbnail())
                .sku(p.getSku()).unitPrice(i.getUnitPrice())
                .currentPrice(p.getPrice()).quantity(i.getQuantity())
                .stockQty(p.getStockQty())
                .subtotal(i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .build();
        }).collect(Collectors.toList());

        BigDecimal total = items.stream().map(CartItemDto::getSubtotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<PromotionService.LineItem> lineItems = cart.getItems().stream()
            .map(i -> new PromotionService.LineItem(i.getProduct(), i.getQuantity(), i.getUnitPrice()))
            .toList();
        // Xem truoc gio hang: chua biet don co phai tu Build PC hay khong nen khong ap dung
        // thuong CPU/cash bonus o day — chi tinh chinh xac luc dat hang (OrderService.createOrder).
        BigDecimal autoDiscount = promotionService.calculateDiscount(lineItems, total, null).getDiscountAmount();

        return CartResponse.builder().items(items)
            .totalItems(items.stream().mapToInt(CartItemDto::getQuantity).sum())
            .totalAmount(total).autoDiscount(autoDiscount).build();
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class CartResponse {
        private List<CartItemDto> items;
        private int totalItems;
        private BigDecimal totalAmount;
        private BigDecimal autoDiscount;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class CartItemDto {
        private Long productId;
        private String productName;
        private String productSlug;
        private String thumbnail;
        private String sku;
        private BigDecimal unitPrice;
        private BigDecimal currentPrice;
        private Integer quantity;
        private Integer stockQty;
        private BigDecimal subtotal;
    }
}
