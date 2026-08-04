package com.kinhduanpc.repository;

import com.kinhduanpc.entity.ReturnRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReturnRequestRepository extends JpaRepository<ReturnRequest, Long> {
    List<ReturnRequest> findByUserId(Long userId);
    List<ReturnRequest> findByOrderId(Long orderId);
    Optional<ReturnRequest> findByReturnCode(String returnCode);

    @Query("SELECT rr FROM ReturnRequest rr JOIN FETCH rr.user ORDER BY rr.createdAt DESC")
    List<ReturnRequest> findAllByOrderByCreatedAtDesc();

    @Query("SELECT rr FROM ReturnRequest rr JOIN FETCH rr.user WHERE rr.status = :status ORDER BY rr.createdAt DESC")
    List<ReturnRequest> findByStatusOrderByCreatedAtDesc(ReturnRequest.ReturnStatus status);

    /**
     * Chuyen trang thai approved -> completed 1 cach nguyen tu (dieu kien ngay trong WHERE),
     * tranh race condition khi 2 request hoan tat gan nhu dong thoi cho cung 1 yeu cau.
     * Tra ve so dong bi anh huong: 0 nghia la da hoan tat truoc do hoac khong con o trang thai approved.
     */
    @Modifying(clearAutomatically = true)
    @Query("""
        UPDATE ReturnRequest rr SET rr.status = :toStatus, rr.completedAt = :completedAt
        WHERE rr.id = :id AND rr.status = :fromStatus AND rr.completedAt IS NULL
        """)
    int markCompletedIfApproved(
        @Param("id") Long id,
        @Param("completedAt") LocalDateTime completedAt,
        @Param("fromStatus") ReturnRequest.ReturnStatus fromStatus,
        @Param("toStatus") ReturnRequest.ReturnStatus toStatus);
}
