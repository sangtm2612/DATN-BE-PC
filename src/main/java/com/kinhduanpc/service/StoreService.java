package com.kinhduanpc.service;

import com.kinhduanpc.dto.ProductStockByStoreDTO;
import com.kinhduanpc.dto.StoreDTO;
import com.kinhduanpc.dto.StoreRequest;
import com.kinhduanpc.entity.Product;
import com.kinhduanpc.entity.ProductStockByStore;
import com.kinhduanpc.entity.ProductStockByStoreId;
import com.kinhduanpc.entity.Store;
import com.kinhduanpc.exception.AppException;
import com.kinhduanpc.repository.ProductRepository;
import com.kinhduanpc.repository.ProductStockByStoreRepository;
import com.kinhduanpc.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class StoreService {

    private final StoreRepository storeRepo;
    private final ProductStockByStoreRepository stockRepo;
    private final ProductRepository productRepo;

    @Transactional(readOnly = true)
    public List<StoreDTO> findAll(String province) {
        List<Store> stores = province != null
            ? storeRepo.findByProvinceAndIsActiveTrue(province)
            : storeRepo.findByIsActiveTrueOrderByProvinceAscNameAsc();
        return stores.stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public StoreDTO findBySlug(String slug) {
        Store store = storeRepo.findBySlug(slug)
            .orElseThrow(() -> AppException.notFound("Cửa hàng"));
        return toDTO(store);
    }

    public StoreDTO create(StoreRequest request) {
        // Validate unique slug if needed
        String slug = generateSlug(request.getName());
        if (storeRepo.findBySlug(slug).isPresent()) {
            throw AppException.conflict("STORE_EXISTS", "Cửa hàng với tên này đã tồn tại");
        }

        Store store = Store.builder()
            .name(request.getName())
            .slug(slug)
            .address(request.getAddress())
            .province(request.getProvince())
            .district(request.getDistrict())
            .phone(request.getPhone())
            .email(request.getEmail())
            .openHours(request.getOpenHours())
            .googleMapsUrl(request.getGoogleMapsUrl())
            .isActive(true)
            .build();

        Store saved = storeRepo.save(store);
        return toDTO(saved);
    }

    public StoreDTO update(Long id, StoreRequest request) {
        Store store = storeRepo.findById(id)
            .orElseThrow(() -> AppException.notFound("Cửa hàng"));

        store.setName(request.getName());
        store.setAddress(request.getAddress());
        store.setProvince(request.getProvince());
        store.setDistrict(request.getDistrict());
        store.setPhone(request.getPhone());
        store.setEmail(request.getEmail());
        store.setOpenHours(request.getOpenHours());
        store.setGoogleMapsUrl(request.getGoogleMapsUrl());

        Store updated = storeRepo.save(store);
        return toDTO(updated);
    }

    @Transactional(readOnly = true)
    public List<ProductStockByStoreDTO> getStockByStore(Long storeId) {
        if (!storeRepo.existsById(storeId)) {
            throw AppException.notFound("Cửa hàng");
        }

        List<ProductStockByStore> stocks = stockRepo.findByStoreId(storeId);
        return stocks.stream().map(this::toStockDTO).toList();
    }

    public ProductStockByStoreDTO updateStock(Long storeId, Long productId, Integer quantity) {
        Store store = storeRepo.findById(storeId)
            .orElseThrow(() -> AppException.notFound("Cửa hàng"));
        Product product = productRepo.findById(productId)
            .orElseThrow(() -> AppException.notFound("Sản phẩm"));

        if (quantity < 0) {
            throw AppException.badRequest("INVALID_QUANTITY", "Số lượng phải >= 0");
        }

        ProductStockByStoreId key = new ProductStockByStoreId(productId, storeId);
        ProductStockByStore stock = stockRepo.findById(key)
            .orElse(ProductStockByStore.builder()
                .product(product)
                .store(store)
                .build());

        stock.setStockQty(quantity);
        ProductStockByStore saved = stockRepo.save(stock);
        return toStockDTO(saved);
    }

    // Mapping methods
    private StoreDTO toDTO(Store store) {
        return StoreDTO.builder()
            .id(store.getId())
            .name(store.getName())
            .slug(store.getSlug())
            .address(store.getAddress())
            .province(store.getProvince())
            .district(store.getDistrict())
            .phone(store.getPhone())
            .email(store.getEmail())
            .openHours(store.getOpenHours())
            .googleMapsUrl(store.getGoogleMapsUrl())
            .isActive(store.getIsActive())
            .build();
    }

    private ProductStockByStoreDTO toStockDTO(ProductStockByStore stock) {
        return ProductStockByStoreDTO.builder()
            .productId(stock.getProduct().getId())
            .productName(stock.getProduct().getName())
            .productSlug(stock.getProduct().getSlug())
            .thumbnailUrl(stock.getProduct().getThumbnail())
            .storeId(stock.getStore().getId())
            .storeName(stock.getStore().getName())
            .stockQty(stock.getStockQty())
            .updatedAt(stock.getUpdatedAt())
            .build();
    }

    private String generateSlug(String name) {
        return name.toLowerCase()
            .replaceAll("[àáạảãâầấậẩẫăằắặẳẵ]", "a")
            .replaceAll("[èéẹẻẽêềếệểễ]", "e")
            .replaceAll("[ìíịỉĩ]", "i")
            .replaceAll("[òóọỏõôồốộổỗơờớợởỡ]", "o")
            .replaceAll("[ùúụủũưừứựửữ]", "u")
            .replaceAll("[ỳýỵỷỹ]", "y")
            .replaceAll("[đ]", "d")
            .replaceAll("[^a-z0-9\\s-]", "")
            .replaceAll("\\s+", "-")
            .replaceAll("-+", "-")
            .trim();
    }
}
