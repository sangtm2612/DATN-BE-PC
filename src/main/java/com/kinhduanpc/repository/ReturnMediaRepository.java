package com.kinhduanpc.repository;

import com.kinhduanpc.entity.ReturnMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReturnMediaRepository extends JpaRepository<ReturnMedia, Long> {
    List<ReturnMedia> findByReturnRequestIdOrderBySortOrderAsc(Long returnRequestId);
}
