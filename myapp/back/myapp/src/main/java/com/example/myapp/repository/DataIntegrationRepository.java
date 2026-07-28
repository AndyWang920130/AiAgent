package com.example.myapp.repository;

import com.example.myapp.domain.DataIntegration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for the DataIntegration entity.
 */
@Repository
public interface DataIntegrationRepository extends JpaRepository<DataIntegration, Long> {
    List<DataIntegration> findAllByOrderByIdAsc();

    boolean existsByNameIgnoreCase(String name);
}
