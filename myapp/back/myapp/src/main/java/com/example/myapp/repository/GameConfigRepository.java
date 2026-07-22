package com.example.myapp.repository;

import com.example.myapp.contants.enumeration.GameConfigType;
import com.example.myapp.domain.GameConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GameConfigRepository extends JpaRepository<GameConfig, Long> {
    List<GameConfig> findByTypeOrderBySortOrderAscIdAsc(GameConfigType type);

    boolean existsByTypeAndNameIgnoreCase(GameConfigType type, String name);
}
