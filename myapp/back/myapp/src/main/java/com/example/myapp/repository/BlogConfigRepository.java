package com.example.myapp.repository;

import com.example.myapp.contants.enumeration.BlogConfigType;
import com.example.myapp.domain.BlogConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BlogConfigRepository extends JpaRepository<BlogConfig, Long> {
    List<BlogConfig> findByTypeOrderBySortOrderAscIdAsc(BlogConfigType type);

    boolean existsByTypeAndNameIgnoreCase(BlogConfigType type, String name);
}
