package com.kinhduanpc.repository;

import com.kinhduanpc.entity.ServiceRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceRequestRepository extends JpaRepository<ServiceRequest, Long> {
    List<ServiceRequest> findByUserId(Long userId);
    Optional<ServiceRequest> findByServiceCode(String serviceCode);

    boolean existsByWarrantyIdAndStatusNot(Long warrantyId, ServiceRequest.ServiceStatus status);

    @Query("SELECT MAX(sr.serviceCode) FROM ServiceRequest sr WHERE sr.serviceCode LIKE :prefix%")
    Optional<String> findMaxServiceCodeByPrefix(@Param("prefix") String prefix);

    @Query("SELECT sr FROM ServiceRequest sr JOIN FETCH sr.user ORDER BY sr.createdAt DESC")
    List<ServiceRequest> findAllByOrderByCreatedAtDesc();

    @Query("SELECT sr FROM ServiceRequest sr JOIN FETCH sr.user WHERE sr.status = :status ORDER BY sr.createdAt DESC")
    List<ServiceRequest> findByStatusOrderByCreatedAtDesc(ServiceRequest.ServiceStatus status);

    @Query(value = "SELECT sr FROM ServiceRequest sr JOIN FETCH sr.user ORDER BY sr.createdAt DESC",
           countQuery = "SELECT COUNT(sr) FROM ServiceRequest sr")
    Page<ServiceRequest> findAllPaged(Pageable pageable);

    @Query(value = "SELECT sr FROM ServiceRequest sr JOIN FETCH sr.user WHERE sr.status = :status ORDER BY sr.createdAt DESC",
           countQuery = "SELECT COUNT(sr) FROM ServiceRequest sr WHERE sr.status = :status")
    Page<ServiceRequest> findByStatusPaged(@Param("status") ServiceRequest.ServiceStatus status, Pageable pageable);
}
