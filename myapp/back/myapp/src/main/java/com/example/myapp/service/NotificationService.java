package com.example.myapp.service;

import com.example.myapp.contants.enumeration.NotificationType;
import com.example.myapp.domain.Notification;
import com.example.myapp.repository.NotificationRepository;
import com.example.myapp.service.dto.NotificationDTO;
import com.example.myapp.utils.SecurityUtil;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Per-user notifications. Other services call the producer {@link #notify} (fire-and-forget,
 * mirroring {@link AchievementService#award}); the controller calls the recipient-scoped
 * consumer methods, all keyed to the current authenticated user.
 */
@Service
@Transactional
public class NotificationService {

    private static final Logger LOG = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    // ----- Producer ------------------------------------------------------------------------

    /**
     * Create a notification for {@code recipient}. No-op for anonymous / system recipients.
     * Safe to call from other services — never throws for a missing/blank recipient.
     */
    public void notify(String recipient, NotificationType type, String title, String content, String link) {
        if (recipient == null || recipient.isBlank() || "system".equalsIgnoreCase(recipient)) {
            return;
        }
        LOG.debug("Notifying {} : {}", recipient, title);
        Notification n = new Notification();
        n.setRecipient(recipient);
        n.setType(type);
        n.setTitle(title);
        n.setContent(content);
        n.setLink(link);
        n.setRead(false);
        String actor = SecurityUtil.getCurrentUsername();
        n.setCreatedBy(actor);
        n.setLastModifiedBy(actor);
        notificationRepository.save(n);
    }

    // ----- Consumer (current user only) ----------------------------------------------------

    @Transactional(readOnly = true)
    public Page<NotificationDTO> list(Pageable pageable) {
        String me = SecurityUtil.getCurrentUsername();
        return notificationRepository.findByRecipientOrderByCreatedDateDesc(me, pageable).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public long unreadCount() {
        return notificationRepository.countByRecipientAndReadFalse(SecurityUtil.getCurrentUsername());
    }

    public void markRead(Long id) {
        String me = SecurityUtil.getCurrentUsername();
        notificationRepository.findByIdAndRecipient(id, me).ifPresent(n -> {
            n.setRead(true);
            notificationRepository.save(n);
        });
    }

    public void markAllRead() {
        String me = SecurityUtil.getCurrentUsername();
        List<Notification> unread = notificationRepository.findByRecipientAndReadFalse(me);
        unread.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(unread);
    }

    public void delete(Long id) {
        String me = SecurityUtil.getCurrentUsername();
        notificationRepository.findByIdAndRecipient(id, me).ifPresent(notificationRepository::delete);
    }

    public void clearRead() {
        String me = SecurityUtil.getCurrentUsername();
        notificationRepository.deleteAll(notificationRepository.findByRecipientAndReadTrue(me));
    }

    private NotificationDTO toDto(Notification n) {
        return new NotificationDTO(
            n.getId(),
            n.getType().name().toLowerCase(),
            n.getTitle(),
            n.getContent(),
            n.getLink(),
            n.isRead(),
            n.getCreatedDate()
        );
    }
}
