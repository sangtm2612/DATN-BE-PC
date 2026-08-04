package com.kinhduanpc.repository;

import com.kinhduanpc.entity.SearchHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SearchHistoryRepository extends JpaRepository<SearchHistory, Long> {
    List<SearchHistory> findByUserIdOrderBySearchedAtDesc(Long userId);

    List<SearchHistory> findTop10ByUserIdOrderBySearchedAtDesc(Long userId);
    List<SearchHistory> findTop10BySessionIdOrderBySearchedAtDesc(String sessionId);
}
