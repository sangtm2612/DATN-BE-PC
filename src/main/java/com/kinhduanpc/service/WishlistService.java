package com.kinhduanpc.service;

import com.kinhduanpc.dto.WishlistDTO;
import com.kinhduanpc.entity.Product;
import com.kinhduanpc.entity.User;
import com.kinhduanpc.entity.Wishlist;
import com.kinhduanpc.exception.AppException;
import com.kinhduanpc.repository.ProductRepository;
import com.kinhduanpc.repository.UserRepository;
import com.kinhduanpc.repository.WishlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class WishlistService {

    private final WishlistRepository wishlistRepo;
    private final UserRepository userRepo;
    private final ProductRepository productRepo;

    @Transactional(readOnly = true)
    public List<WishlistDTO> getWishlistByUser(Long userId) {
        return wishlistRepo.findByUserId(userId).stream()
            .map(this::toDTO)
            .toList();
    }

    public String toggleWishlist(Long userId, Long productId) {
        if (wishlistRepo.existsByUserIdAndProductId(userId, productId)) {
            wishlistRepo.deleteByUserIdAndProductId(userId, productId);
            return "Đã xóa khỏi yêu thích";
        } else {
            User user = userRepo.findById(userId)
                .orElseThrow(() -> AppException.notFound("Người dùng"));
            Product product = productRepo.findById(productId)
                .orElseThrow(() -> AppException.notFound("Sản phẩm"));

            Wishlist wishlist = Wishlist.builder()
                .user(user)
                .product(product)
                .build();
            wishlistRepo.save(wishlist);
            return "Đã thêm vào yêu thích";
        }
    }

    @Transactional(readOnly = true)
    public boolean checkInWishlist(Long userId, Long productId) {
        return wishlistRepo.existsByUserIdAndProductId(userId, productId);
    }

    // Mapping methods
    private WishlistDTO toDTO(Wishlist wishlist) {
        Product p = wishlist.getProduct();
        
        // Calculate discounted price if on sale
        BigDecimal discountedPrice = p.getIsOnSale() && p.getOriginalPrice() != null 
            ? p.getPrice() 
            : null;
        
        // Original price is stored in originalPrice if on sale, otherwise use price
        BigDecimal originalPrice = p.getOriginalPrice() != null 
            ? p.getOriginalPrice() 
            : p.getPrice();

        return WishlistDTO.builder()
            .productId(p.getId())
            .productName(p.getName())
            .slug(p.getSlug())
            .price(originalPrice)
            .discountedPrice(discountedPrice)
            .thumbnailUrl(p.getThumbnail())
            .inStock(p.getStockQty() > 0)
            .stockQuantity(p.getStockQty())
            .addedAt(wishlist.getAddedAt())
            .build();
    }
}
