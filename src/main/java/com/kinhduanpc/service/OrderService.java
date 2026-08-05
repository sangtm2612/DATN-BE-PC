package com.kinhduanpc.service;

import com.kinhduanpc.dto.order.*;
import com.kinhduanpc.entity.*;
import com.kinhduanpc.exception.AppException;
import com.kinhduanpc.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OrderService {

    private final OrderRepository orderRepo;
    private final CartRepository cartRepo;
    private final ProductRepository productRepo;
    private final UserRepository userRepo;
    private final VoucherRepository voucherRepo;
    private final PcBuildRepository pcBuildRepo;
    private final PromotionService promotionService;
    private final ShippingMethodRepository shippingMethodRepo;
    private final OrderShippingRepository orderShippingRepo;
    private final ProductStockByStoreRepository stockByStoreRepo;
    private final StoreRepository storeRepo;
    private final EmailService emailService;
    private final NotificationService notificationService;

    private static final BigDecimal DEFAULT_SHIPPING_FEE = BigDecimal.valueOf(30000);

    public OrderResponse createOrder(Long userId, CreateOrderRequest req) {
        User user = userRepo.findById(userId)
            .orElseThrow(() -> AppException.notFound("Người dùng"));

        Cart cart = cartRepo.findByUserId(userId)
            .orElseThrow(() -> AppException.badRequest("EMPTY_CART", "Giỏ hàng trống"));

        if (cart.getItems().isEmpty()) {
            throw AppException.badRequest("EMPTY_CART", "Giỏ hàng không có sản phẩm");
        }

        // Calculate subtotal
        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItem item : cart.getItems()) {
            Product p = item.getProduct();
            if (p.getStockQty() < item.getQuantity()) {
                throw AppException.badRequest("INSUFFICIENT_STOCK",
                    "Sản phẩm " + p.getName() + " không đủ hàng (còn " + p.getStockQty() + ")");
            }
            subtotal = subtotal.add(p.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        }

        // Build PC (nếu đặt hàng từ cấu hình đã lưu) — resolve trước để PromotionService biết
        // co ap dung uu dai build_pc (CPU %/cash bonus) hay khong.
        PcBuild build = null;
        if (req.getBuildId() != null) {
            build = pcBuildRepo.findById(req.getBuildId())
                .orElseThrow(() -> AppException.notFound("Cấu hình PC"));
            if (!build.getUserId().equals(userId)) {
                throw AppException.forbidden("Bạn không có quyền dùng cấu hình PC này");
            }
        }

        // Voucher (nhập tay)
        BigDecimal voucherDiscount = BigDecimal.ZERO;
        Voucher voucher = null;
        if (req.getVoucherCode() != null && !req.getVoucherCode().isBlank()) {
            voucher = voucherRepo.findValidByCode(req.getVoucherCode(), LocalDateTime.now())
                .orElseThrow(() -> AppException.badRequest("INVALID_VOUCHER", "Mã voucher không hợp lệ hoặc đã hết hạn"));

            if (subtotal.compareTo(voucher.getMinOrderValue()) < 0) {
                throw AppException.badRequest("VOUCHER_MIN_ORDER",
                    "Đơn hàng cần tối thiểu " + voucher.getMinOrderValue() + "đ để dùng voucher này");
            }

            if (voucher.getDiscountType() == Voucher.DiscountType.percent) {
                voucherDiscount = subtotal.multiply(voucher.getDiscountValue()).divide(BigDecimal.valueOf(100));
                if (voucher.getMaxDiscount() != null && voucherDiscount.compareTo(voucher.getMaxDiscount()) > 0) {
                    voucherDiscount = voucher.getMaxDiscount();
                }
            } else if (voucher.getDiscountType() == Voucher.DiscountType.fixed_amount) {
                voucherDiscount = voucher.getDiscountValue();
            }

            voucher.setUsedCount(voucher.getUsedCount() + 1);
            voucherRepo.save(voucher);
        }

        // Khuyến mãi tự động (Promotion) — snapshot tại thời điểm đặt hàng, không dùng lại
        // giá trị FE gửi lên để tránh khách sửa giá qua request hoặc promotion đã hết hạn.
        List<PromotionService.LineItem> lineItems = cart.getItems().stream()
            .map(i -> new PromotionService.LineItem(i.getProduct(), i.getQuantity(), i.getProduct().getPrice()))
            .toList();
        PromotionService.PromotionCalcResult promoResult =
            promotionService.calculateDiscount(lineItems, subtotal, build);

        // Voucher + Promotion cộng dồn, không vượt quá subtotal đơn hàng
        BigDecimal discountAmount = voucherDiscount
            .add(promoResult.getDiscountAmount())
            .add(promoResult.getCashBonus());
        if (discountAmount.compareTo(subtotal) > 0) discountAmount = subtotal;

        // Shipping fee
        ShippingMethod shippingMethod = null;
        BigDecimal shippingFee;
        if (req.getShippingMethodId() != null) {
            shippingMethod = shippingMethodRepo.findById(req.getShippingMethodId())
                .orElseThrow(() -> AppException.notFound("Phương thức giao hàng"));
            shippingFee = shippingMethod.getBaseFee();
            if (shippingMethod.getFreeThreshold() != null
                    && subtotal.compareTo(shippingMethod.getFreeThreshold()) >= 0) {
                shippingFee = BigDecimal.ZERO;
            }
        } else {
            shippingFee = DEFAULT_SHIPPING_FEE;
        }
        if (voucher != null && voucher.getDiscountType() == Voucher.DiscountType.free_shipping) {
            shippingFee = BigDecimal.ZERO;
        }
        if (req.getPickupStoreId() != null) {
            Store pickupStore = storeRepo.findById(req.getPickupStoreId())
                .orElseThrow(() -> AppException.notFound("Cửa hàng"));
            if (!Boolean.TRUE.equals(pickupStore.getIsActive())) {
                throw AppException.badRequest("STORE_INACTIVE", "Cửa hàng này hiện không nhận đơn tại quầy");
            }
            shippingFee = BigDecimal.ZERO; // Nhận tại showroom — không phát sinh phí ship
        }

        BigDecimal totalAmount = subtotal.subtract(discountAmount).add(shippingFee);
        if (totalAmount.compareTo(BigDecimal.ZERO) < 0) totalAmount = BigDecimal.ZERO;

        // Create order
        Order order = Order.builder()
            .orderCode(generateOrderCode())
            .user(user)
            .status(Order.OrderStatus.pending)
            .paymentMethod(Order.PaymentMethod.valueOf(req.getPaymentMethod()))
            .paymentStatus(Order.PaymentStatus.pending)
            .shippingName(req.getShippingName())
            .shippingPhone(req.getShippingPhone())
            .shippingProvince(req.getShippingProvince())
            .shippingDistrict(req.getShippingDistrict())
            .shippingWard(req.getShippingWard())
            .shippingAddress(req.getShippingAddress())
            .subtotal(subtotal)
            .shippingFee(shippingFee)
            .discountAmount(discountAmount)
            .totalAmount(totalAmount)
            .voucher(voucher)
            .voucherCode(req.getVoucherCode())
            .pickupStoreId(req.getPickupStoreId())
            .note(req.getNote())
            .build(build)
            .build();

        // COD auto-cancel after 48h
        if (Order.PaymentMethod.cod == order.getPaymentMethod()) {
            order.setAutoCancelAt(LocalDateTime.now().plusHours(48));
        }

        // Create order items + deduct stock
        for (CartItem cartItem : cart.getItems()) {
            Product p = cartItem.getProduct();
            OrderItem oi = OrderItem.builder()
                .order(order).product(p)
                .productName(p.getName()).productSku(p.getSku())
                .productImage(p.getThumbnail())
                .warrantyMonths(p.getWarrantyMonths())
                .quantity(cartItem.getQuantity())
                .unitPrice(p.getPrice())
                .discountPrice(BigDecimal.ZERO)
                .totalPrice(p.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())))
                .build();
            order.getItems().add(oi);

            // Deduct stock
            p.setStockQty(p.getStockQty() - cartItem.getQuantity());
            p.setSoldQty(p.getSoldQty() + cartItem.getQuantity());
            productRepo.save(p);
        }

        orderRepo.save(order);

        // Ghi nhận phương thức/phí giao hàng cho đơn
        orderShippingRepo.save(OrderShipping.builder()
            .order(order)
            .method(shippingMethod)
            .pickupStoreId(req.getPickupStoreId())
            .shippingFee(shippingFee)
            .status("pending")
            .build());

        // Nhận tại showroom: trừ tồn kho riêng của showroom đó (lớp bổ sung, không đụng
        // products.stock_qty tổng — showroom có thể chưa được seed tồn kho riêng nên bỏ qua
        // nếu không có bản ghi, tránh chặn luồng đặt hàng vì thiếu dữ liệu bổ trợ).
        if (req.getPickupStoreId() != null) {
            for (OrderItem item : order.getItems()) {
                ProductStockByStoreId key = new ProductStockByStoreId(item.getProduct().getId(), req.getPickupStoreId());
                stockByStoreRepo.findById(key).ifPresent(stock -> {
                    stock.setStockQty(Math.max(0, stock.getStockQty() - item.getQuantity()));
                    stockByStoreRepo.save(stock);
                });
            }
        }

        // Clear cart
        cart.getItems().clear();
        cartRepo.save(cart);

        // Send email
        if (user.getEmail() != null) {
            emailService.sendOrderConfirmation(user.getEmail(), user.getFullName(),
                order.getOrderCode(), totalAmount.toPlainString());
        }

        notificationService.createNotification(userId, "order_update",
            "Đặt hàng thành công", "Đơn hàng " + order.getOrderCode() + " đã được tạo",
            "order", order.getId());

        log.info("Order created: {} for user {}", order.getOrderCode(), userId);
        return toResponse(order);
    }

    public OrderResponse trackOrder(String orderCode, String phone) {
        Order order = orderRepo.findByOrderCodeAndPhone(orderCode, phone)
            .orElseThrow(() -> AppException.notFound("Đơn hàng"));
        return toResponse(order);
    }

    public Page<OrderResponse> getUserOrders(Long userId, String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Order> orders;
        if (userId == null) {
            // Admin: lấy tất cả
            orders = status != null
                ? orderRepo.findAllByStatusOrderByCreatedAtDesc(Order.OrderStatus.valueOf(status), pageable)
                : orderRepo.findAllByOrderByCreatedAtDesc(pageable);
        } else {
            orders = status != null
                ? orderRepo.findByUserIdAndStatus(userId, Order.OrderStatus.valueOf(status), pageable)
                : orderRepo.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        }
        return orders.map(this::toResponse);
    }

    public OrderResponse getOrderDetail(Long orderId, Long userId) {
        Order order = orderRepo.findById(orderId)
            .orElseThrow(() -> AppException.notFound("Đơn hàng"));
        if (userId != null && !order.getUser().getId().equals(userId)) {
            throw AppException.forbidden("Bạn không có quyền xem đơn hàng này");
        }
        return toResponse(order);
    }

    public OrderResponse cancelOrder(Long orderId, Long userId, String reason) {
        Order order = orderRepo.findById(orderId)
            .orElseThrow(() -> AppException.notFound("Đơn hàng"));

        if (!order.getUser().getId().equals(userId)) {
            throw AppException.forbidden("Bạn không có quyền hủy đơn hàng này");
        }
        if (order.getStatus() != Order.OrderStatus.pending) {
            throw AppException.badRequest("CANNOT_CANCEL",
                "Chỉ có thể hủy đơn hàng ở trạng thái chờ xác nhận");
        }

        order.setStatus(Order.OrderStatus.cancelled);
        order.setCancelledAt(LocalDateTime.now());
        order.setCancelledReason(reason);

        // Hoàn lại tồn kho
        for (OrderItem item : order.getItems()) {
            Product p = item.getProduct();
            p.setStockQty(p.getStockQty() + item.getQuantity());
            p.setSoldQty(Math.max(0, p.getSoldQty() - item.getQuantity()));
            productRepo.save(p);
        }

        orderRepo.save(order);

        if (order.getUser().getEmail() != null) {
            emailService.sendOrderStatusUpdate(order.getUser().getEmail(),
                order.getUser().getFullName(), order.getOrderCode(), "Đã hủy");
        }

        return toResponse(order);
    }

    public OrderResponse updateOrderStatus(Long orderId, String status, String staffNote) {
        Order order = orderRepo.findById(orderId)
            .orElseThrow(() -> AppException.notFound("Đơn hàng"));

        Order.OrderStatus newStatus = Order.OrderStatus.valueOf(status);
        order.setStatus(newStatus);
        if (staffNote != null) order.setStaffNote(staffNote);

        switch (newStatus) {
            case confirmed   -> order.setConfirmedAt(LocalDateTime.now());
            case processing  -> order.setProcessingAt(LocalDateTime.now());
            case shipping    -> order.setShippedAt(LocalDateTime.now());
            case delivered   -> order.setDeliveredAt(LocalDateTime.now());
            case completed   -> order.setCompletedAt(LocalDateTime.now());
            case cancelled   -> order.setCancelledAt(LocalDateTime.now());
            default -> {}
        }

        orderRepo.save(order);

        if (order.getUser() != null && order.getUser().getEmail() != null) {
            emailService.sendOrderStatusUpdate(order.getUser().getEmail(),
                order.getUser().getFullName(), order.getOrderCode(), status);
        }

        return toResponse(order);
    }

    private String generateOrderCode() {
        String year = String.valueOf(LocalDateTime.now().getYear());
        
        // Query last order code in current year
        String prefix = "HC-" + year + "-";
        List<Order> lastOrders = orderRepo.findTop1ByOrderCodeStartingWithOrderByOrderCodeDesc(prefix);
        
        int nextNumber = 1;
        if (!lastOrders.isEmpty()) {
            String lastCode = lastOrders.get(0).getOrderCode();
            // Extract number from "HC-2026-000001"
            String numberPart = lastCode.substring(lastCode.lastIndexOf('-') + 1);
            nextNumber = Integer.parseInt(numberPart) + 1;
        }
        
        return prefix + String.format("%06d", nextNumber);
    }

    public OrderResponse toResponse(Order o) {
        return OrderResponse.builder()
            .id(o.getId()).orderCode(o.getOrderCode())
            .status(o.getStatus().name()).paymentMethod(o.getPaymentMethod().name())
            .paymentStatus(o.getPaymentStatus().name())
            .shippingName(o.getShippingName()).shippingPhone(o.getShippingPhone())
            .shippingProvince(o.getShippingProvince()).shippingDistrict(o.getShippingDistrict())
            .shippingWard(o.getShippingWard()).shippingAddress(o.getShippingAddress())
            .subtotal(o.getSubtotal()).shippingFee(o.getShippingFee())
            .discountAmount(o.getDiscountAmount()).totalAmount(o.getTotalAmount())
            .voucherCode(o.getVoucherCode()).note(o.getNote())
            .cancelledReason(o.getCancelledReason())
            .buildId(o.getBuild() != null ? o.getBuild().getId() : null)
            .buildName(o.getBuild() != null ? o.getBuild().getName() : null)
            .createdAt(o.getCreatedAt()).confirmedAt(o.getConfirmedAt())
            .shippedAt(o.getShippedAt()).deliveredAt(o.getDeliveredAt())
            .completedAt(o.getCompletedAt()).cancelledAt(o.getCancelledAt())
            .items(o.getItems().stream().map(i -> OrderResponse.OrderItemResponse.builder()
                .id(i.getId()).productId(i.getProduct().getId())
                .productName(i.getProductName()).productSku(i.getProductSku())
                .productImage(i.getProductImage()).quantity(i.getQuantity())
                .unitPrice(i.getUnitPrice()).totalPrice(i.getTotalPrice())
                .warrantyMonths(i.getWarrantyMonths()).build()).toList())
            .build();
    }
}
