package com.kinhduanpc.repository;

import com.kinhduanpc.entity.ReturnRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReturnRequestRepository extends JpaRepository<ReturnRequest, Long> {
    List<ReturnRequest> findByUserId(Long userId);
    List<ReturnRequest> findByOrderId(Long orderId);
    Optional<ReturnRequest> findByReturnCode(String returnCode);
}
