package com.example.myapp.web.rest.vm;

/**
 * View model with site-wide blog aggregate stats shown on the homepage.
 */
public record BlogStats(long totalPosts, long totalViews, long totalLikes, long totalComments) {}
