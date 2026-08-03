package com.kinhduanpc.repository;

import com.kinhduanpc.entity.ServiceMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceMediaRepository extends JpaRepository<ServiceMedia, Long> {
    List<ServiceMedia> findByServiceRequestIdOrderBySortOrderAsc(Long serviceRequestId);
}
