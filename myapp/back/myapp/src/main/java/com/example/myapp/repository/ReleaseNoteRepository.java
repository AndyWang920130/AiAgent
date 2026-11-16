package com.example.myapp.repository;

import com.example.myapp.contants.enumeration.ReleaseNoteStatus;
import com.example.myapp.domain.ReleaseNote;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Derived queries throughout — the release-note list has no free-text search or multi-filter
 * combination to serve, so none of the {@code @Query} machinery {@link FeedbackRepository}
 * needs is warranted here.
 */
@Repository
public interface ReleaseNoteRepository extends JpaRepository<ReleaseNote, Long> {

    /**
     * The public list: published notes only, newest release first. Drafts are excluded here
     * rather than filtered out afterwards, so an unpublished note cannot reach a non-admin
     * caller even if a later change forgets to check.
     */
    Page<ReleaseNote> findByStatusOrderByReleasedDateDesc(ReleaseNoteStatus status, Pageable pageable);

    /** The administrator's list: drafts included, same ordering. */
    Page<ReleaseNote> findAllByOrderByReleasedDateDesc(Pageable pageable);

    /**
     * Newest published release, for the version the About page shows. Ignores drafts by the
     * same argument as above: a version that is still being written is not the current one.
     */
    Optional<ReleaseNote> findFirstByStatusOrderByReleasedDateDesc(ReleaseNoteStatus status);

    /** Duplicate-version guard on create. */
    boolean existsByVersion(String version);

    /** Duplicate-version guard on update, which has to allow the row keeping its own version. */
    Optional<ReleaseNote> findByVersion(String version);
}
