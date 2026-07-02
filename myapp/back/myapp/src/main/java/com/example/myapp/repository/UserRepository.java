package com.example.myapp.repository;

import com.example.myapp.domain.User;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for the MyAppUser entity.
 */
@SuppressWarnings("unused")
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findOneByLogin(String login);

    boolean existsByLogin(String login);
}
