package com.example.myapp.contants.enumeration;

/**
 * Whether a release note is public yet. A {@code DRAFT} is listed only for administrators, so
 * notes for a release can be written and reviewed before the release itself goes out;
 * publishing is what makes one visible to everybody.
 *
 * <p>Separate from {@code BlogStatus} on purpose. That enum carries {@code ARCHIVED}, which
 * means nothing for a release note — a release that happened cannot un-happen — and sharing
 * one enum would let a change made for the blog alter what this page shows.
 */
public enum ReleaseNoteStatus {
    DRAFT,
    PUBLISHED,
}
