package com.kinhduanpc.service;

import com.kinhduanpc.dto.ShippingMethodDTO;
import com.kinhduanpc.dto.ShippingMethodRequest;
import com.kinhduanpc.entity.ShippingMethod;
import com.kinhduanpc.exception.AppException;
import com.kinhduanpc.repository.ShippingMethodRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ShippingService {

    private final ShippingMethodRepository shippingMethodRepo;

    @Transactional(readOnly = true)
    public List<ShippingMethodDTO> getActiveMethods() {
        return shippingMethodRepo.findByIsActiveTrue()
            .stream()
            .map(this::toDTO)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<ShippingMethodDTO> getAllMethods() {
        return shippingMethodRepo.findAll()
            .stream()
            .map(this::toDTO)
            .toList();
    }

    public ShippingMethodDTO create(ShippingMethodRequest request) {
        ShippingMethod method = ShippingMethod.builder()
            .name(request.getName())
            .description(request.getDescription())
            .baseFee(request.getBaseFee())
            .freeThreshold(request.getFreeThreshold())
            .estimatedDays(request.getEstimatedDays())
            .isActive(request.getIsActive() != null ? request.getIsActive() : true)
            .build();

        ShippingMethod saved = shippingMethodRepo.save(method);
        return toDTO(saved);
    }

    public ShippingMethodDTO update(Long id, ShippingMethodRequest request) {
        ShippingMethod method = shippingMethodRepo.findById(id)
            .orElseThrow(() -> AppException.notFound("Phương thức giao hàng"));

        method.setName(request.getName());
        method.setDescription(request.getDescription());
        method.setBaseFee(request.getBaseFee());
        method.setFreeThreshold(request.getFreeThreshold());
        method.setEstimatedDays(request.getEstimatedDays());
        
        if (request.getIsActive() != null) {
            method.setIsActive(request.getIsActive());
        }

        ShippingMethod updated = shippingMethodRepo.save(method);
        return toDTO(updated);
    }

    public void delete(Long id) {
        if (!shippingMethodRepo.existsById(id)) {
            throw AppException.notFound("Phương thức giao hàng");
        }
        shippingMethodRepo.deleteById(id);
    }

    // Mapping method
    private ShippingMethodDTO toDTO(ShippingMethod method) {
        return ShippingMethodDTO.builder()
            .id(method.getId())
            .name(method.getName())
            .description(method.getDescription())
            .baseFee(method.getBaseFee())
            .freeThreshold(method.getFreeThreshold())
            .estimatedDays(method.getEstimatedDays())
            .isActive(method.getIsActive())
            .build();
    }
}
