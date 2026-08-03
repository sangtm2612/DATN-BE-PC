package com.kinhduanpc.repository;

import com.kinhduanpc.entity.PcBuildItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PcBuildItemRepository extends JpaRepository<PcBuildItem, Long> {
    List<PcBuildItem> findByBuildId(Long buildId);
}
