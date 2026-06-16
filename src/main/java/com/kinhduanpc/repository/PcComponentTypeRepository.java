package com.kinhduanpc.repository;

import com.kinhduanpc.entity.PcComponentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PcComponentTypeRepository extends JpaRepository<PcComponentType, Long> {
    List<PcComponentType> findAllByOrderBySortOrderAsc();
}
