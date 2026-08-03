package com.kinhduanpc.repository;

import com.kinhduanpc.entity.PcBuild;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PcBuildRepository extends JpaRepository<PcBuild, Long> {
    List<PcBuild> findByUserId(Long userId);
}
