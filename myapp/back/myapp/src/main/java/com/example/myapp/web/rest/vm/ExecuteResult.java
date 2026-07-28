package com.example.myapp.web.rest.vm;

import java.util.Map;

/**
 * Result of executing a Data Integration: the upstream HTTP status, how long the
 * call took, the response headers and body, whether it was a 2xx success, and an
 * error message when the call could not be completed (connection/timeout).
 */
public record ExecuteResult(
    int status,
    long durationMs,
    Map<String, String> headers,
    String body,
    boolean success,
    String error
) {}
