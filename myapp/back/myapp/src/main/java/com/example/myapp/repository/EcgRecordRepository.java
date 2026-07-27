package com.example.myapp.repository;

import com.example.myapp.domain.EcgRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for the EcgRecord entity.
 */
@Repository
public interface EcgRecordRepository extends JpaRepository<EcgRecord, Long> {
    List<EcgRecord> findAllByOrderByIdAsc();
}
