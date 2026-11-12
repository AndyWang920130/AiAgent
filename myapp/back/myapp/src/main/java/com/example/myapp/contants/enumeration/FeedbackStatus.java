package com.example.myapp.contants.enumeration;

/**
 * Where a submission stands. Only an administrator moves it along; the submitter sees the
 * status on their own list. {@code NEW} and {@code IN_PROGRESS} count as open, which is what
 * the per-user submission cap and the administrator's unresolved badge are counted over.
 */
public enum FeedbackStatus {
    NEW,
    IN_PROGRESS,
    RESOLVED,
}
