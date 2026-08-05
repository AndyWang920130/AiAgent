package com.example.myapp.repository;

import com.example.myapp.domain.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for the MyAppUser entity.
 */
@SuppressWarnings("unused")
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findOneByLogin(String login);

    List<User> findByLoginIn(Collection<String> logins);

    /**
     * Case-insensitive partial match across login, real name and nickname, excluding
     * soft-deleted users. Result size is bounded by the supplied {@link Pageable}.
     */
    @Query("""
        select u from User u
        where (u.deleted is null or u.deleted = false)
          and (
            lower(u.login) like lower(concat('%', :q, '%'))
            or lower(u.realName) like lower(concat('%', :q, '%'))
            or lower(u.nickName) like lower(concat('%', :q, '%'))
          )
        order by u.login asc
        """)
    List<User> search(@Param("q") String q, Pageable pageable);

    Optional<User> findOneByEmailIgnoreCase(String email);

    boolean existsByLogin(String login);

    boolean existsByEmailIgnoreCase(String email);
}
