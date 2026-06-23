package com.example.myapp.repository;

import com.example.myapp.domain.MyAppUser;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the MyAppUser entity.
 */
@SuppressWarnings("unused")
@Repository
public interface MyAppUserRepository extends JpaRepository<MyAppUser, Long> {}
