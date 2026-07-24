package com.example.myapp.repository;

import com.example.myapp.contants.enumeration.AchievementType;
import com.example.myapp.domain.Achievement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AchievementRepository extends JpaRepository<Achievement, Long> {

    Optional<Achievement> findByLoginAndType(String login, AchievementType type);

    List<Achievement> findByLoginOrderByTypeAsc(String login);

    boolean existsByLogin(String login);
}
