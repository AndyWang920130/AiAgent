package com.example.myapp.repository;

import com.example.myapp.contants.enumeration.Role;
import com.example.myapp.domain.User;
import org.springframework.data.domain.Page;
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

    /**
     * Admin User Management listing: optional free-text match across login, names and
     * email, plus optional role and enabled/disabled filters. A null filter means "any",
     * which keeps one query serving every combination the list page offers.
     *
     * @param q        lower-cased term already wrapped in {@code %}, or {@code %} for all.
     * @param role     role to restrict to, or null for any.
     * @param disabled true for disabled accounts only, false for active only, null for any.
     */
    @Query("""
        select u from User u
        where (lower(u.login) like :q
            or lower(coalesce(u.realName, '')) like :q
            or lower(coalesce(u.nickName, '')) like :q
            or lower(coalesce(u.email, '')) like :q)
          and (:role is null or u.role = :role)
          and (:disabled is null or coalesce(u.deleted, false) = :disabled)
        """)
    Page<User> findForAdmin(
        @Param("q") String q,
        @Param("role") Role role,
        @Param("disabled") Boolean disabled,
        Pageable pageable
    );

    /** Active accounts holding a role, used to refuse removing the last administrator. */
    @Query("select count(u) from User u where u.role = :role and coalesce(u.deleted, false) = false")
    long countActiveByRole(@Param("role") Role role);
}
