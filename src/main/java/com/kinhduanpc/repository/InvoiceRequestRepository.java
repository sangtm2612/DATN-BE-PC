package com.kinhduanpc.repository;

import com.kinhduanpc.entity.InvoiceRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InvoiceRequestRepository extends JpaRepository<InvoiceRequest, Long> {
    Optional<InvoiceRequest> findByOrderId(Long orderId);
}
