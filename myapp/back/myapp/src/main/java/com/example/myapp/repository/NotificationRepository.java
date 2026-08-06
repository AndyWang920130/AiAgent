package com.example.myapp.repository;

import com.example.myapp.domain.Notification;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByRecipientOrderByCreatedDateDesc(String recipient, Pageable pageable);

    long countByRecipientAndReadFalse(String recipient);

    List<Notification> findByRecipientAndReadFalse(String recipient);

    List<Notification> findByRecipientAndReadTrue(String recipient);

    /** Ownership-scoped lookup so a user can only mutate their own notifications. */
    Optional<Notification> findByIdAndRecipient(Long id, String recipient);
}
