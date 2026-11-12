package com.example.myapp.repository;

import com.example.myapp.contants.enumeration.FeedbackCategory;
import com.example.myapp.contants.enumeration.FeedbackStatus;
import com.example.myapp.domain.Feedback;
import java.util.Collection;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    Page<Feedback> findByUsernameOrderByCreatedDateDesc(String username, Pageable pageable);

    /** Ownership-scoped lookup so a submitter can only withdraw their own submission. */
    Optional<Feedback> findByIdAndUsername(Long id, String username);

    /** Open submissions by one account, which the per-user submission cap is counted over. */
    long countByUsernameAndStatusIn(String username, Collection<FeedbackStatus> statuses);

    long countByStatusIn(Collection<FeedbackStatus> statuses);

    /**
     * Administrator triage listing: optional free-text match across title, body and submitter,
     * plus optional status and category filters. A null filter means "any", which keeps one
     * query serving every combination the list page offers — same shape as
     * {@link UserRepository#findForAdmin}.
     *
     * @param q        lower-cased term already wrapped in {@code %}, or {@code %} for all.
     * @param status   status to restrict to, or null for any.
     * @param category category to restrict to, or null for any.
     */
    @Query("""
        select f from Feedback f
        where (lower(f.title) like :q
            or lower(f.content) like :q
            or lower(f.username) like :q)
          and (:status is null or f.status = :status)
          and (:category is null or f.category = :category)
        """)
    Page<Feedback> findForAdmin(
        @Param("q") String q,
        @Param("status") FeedbackStatus status,
        @Param("category") FeedbackCategory category,
        Pageable pageable
    );
}
