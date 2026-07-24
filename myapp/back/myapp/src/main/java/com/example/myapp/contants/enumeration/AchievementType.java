package com.example.myapp.contants.enumeration;

/**
 * The AchievementType enumeration. Each type carries the default number of
 * points a single occurrence of the action awards.
 */
public enum AchievementType {
    REGISTRATION(100),
    PUBLISH_ARTICLE(20),
    RECEIVE_LIKE(5);

    private final long defaultPoints;

    AchievementType(long defaultPoints) {
        this.defaultPoints = defaultPoints;
    }

    public long getDefaultPoints() {
        return defaultPoints;
    }
}
