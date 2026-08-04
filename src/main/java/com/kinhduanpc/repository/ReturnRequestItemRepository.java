package com.kinhduanpc.repository;

import com.kinhduanpc.entity.ReturnRequestItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReturnRequestItemRepository extends JpaRepository<ReturnRequestItem, Long> {
    List<ReturnRequestItem> findByReturnRequestId(Long returnRequestId);

    @Query("SELECT i FROM ReturnRequestItem i JOIN FETCH i.orderItem WHERE i.returnRequest.id IN :returnRequestIds")
    List<ReturnRequestItem> findByReturnRequestIdIn(List<Long> returnRequestIds);
}
