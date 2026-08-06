package com.kinhduanpc.service;

import com.kinhduanpc.entity.SearchHistory;
import com.kinhduanpc.repository.SearchHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SearchService {

    private final SearchHistoryRepository searchHistoryRepo;

    public List<String> getSuggestions(Long userId, String sessionId) {
        List<SearchHistory> history;
        
        if (userId != null) {
            history = searchHistoryRepo.findTop10ByUserIdOrderBySearchedAtDesc(userId);
        } else if (sessionId != null) {
            history = searchHistoryRepo.findTop10BySessionIdOrderBySearchedAtDesc(sessionId);
        } else {
            return List.of();
        }

        // Dedupe keywords (keep most recent), limit to 10
        return new LinkedHashSet<>(
                history.stream()
                        .map(SearchHistory::getKeyword)
                        .toList()
        ).stream().limit(10).toList();
    }
}
