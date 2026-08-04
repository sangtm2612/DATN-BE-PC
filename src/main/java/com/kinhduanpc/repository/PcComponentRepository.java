package com.kinhduanpc.repository;

import com.kinhduanpc.entity.PcComponent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PcComponentRepository extends JpaRepository<PcComponent, Long> {
    List<PcComponent> findByComponentTypeId(Long componentTypeId);
    List<PcComponent> findByProductIdIn(List<Long> productIds);
}
