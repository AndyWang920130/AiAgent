package com.example.myapp.repository;

import com.example.myapp.domain.RoleConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoleConfigRepository extends JpaRepository<RoleConfig, Long> {

    List<RoleConfig> findAllByOrderBySortOrderAscIdAsc();

    List<RoleConfig> findByEnabledTrueOrderBySortOrderAscIdAsc();

    Optional<RoleConfig> findOneByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);
}
