package com.kinhduanpc.repository;

import com.kinhduanpc.entity.UserVoucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserVoucherRepository extends JpaRepository<UserVoucher, Long> {

    /**
     * Tìm voucher của user theo voucher code
     */
    @Query("SELECT uv FROM UserVoucher uv " +
           "JOIN FETCH uv.voucher v " +
           "WHERE uv.user.id = :userId AND v.code = :voucherCode")
    Optional<UserVoucher> findByUserIdAndVoucherCode(Long userId, String voucherCode);

    /**
     * Lấy tất cả voucher của user
     */
    @Query("SELECT uv FROM UserVoucher uv " +
           "JOIN FETCH uv.voucher v " +
           "WHERE uv.user.id = :userId " +
           "ORDER BY uv.assignedAt DESC")
    List<UserVoucher> findAllByUserId(Long userId);

    /**
     * Lấy voucher available của user
     */
    @Query("SELECT uv FROM UserVoucher uv " +
           "JOIN FETCH uv.voucher v " +
           "WHERE uv.user.id = :userId AND uv.status = 'AVAILABLE' " +
           "ORDER BY uv.assignedAt DESC")
    List<UserVoucher> findAvailableByUserId(Long userId);

    /**
     * Đếm số lần user đã dùng voucher này
     */
    @Query("SELECT COUNT(uv) FROM UserVoucher uv " +
           "WHERE uv.user.id = :userId AND uv.voucher.id = :voucherId AND uv.status = 'USED'")
    int countUsedByUserAndVoucher(Long userId, Long voucherId);

    /**
     * Check user đã có voucher này chưa
     */
    boolean existsByUserIdAndVoucherId(Long userId, Long voucherId);

    long countByVoucherId(Long voucherId);
}
