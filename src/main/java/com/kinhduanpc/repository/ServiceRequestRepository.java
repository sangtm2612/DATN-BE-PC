package com.kinhduanpc.repository;

import com.kinhduanpc.entity.ServiceRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceRequestRepository extends JpaRepository<ServiceRequest, Long> {
    List<ServiceRequest> findByUserId(Long userId);
    Optional<ServiceRequest> findByServiceCode(String serviceCode);

    @Query("SELECT sr FROM ServiceRequest sr JOIN FETCH sr.user ORDER BY sr.createdAt DESC")
    List<ServiceRequest> findAllByOrderByCreatedAtDesc();

    @Query("SELECT sr FROM ServiceRequest sr JOIN FETCH sr.user WHERE sr.status = :status ORDER BY sr.createdAt DESC")
    List<ServiceRequest> findByStatusOrderByCreatedAtDesc(ServiceRequest.ServiceStatus status);
}
