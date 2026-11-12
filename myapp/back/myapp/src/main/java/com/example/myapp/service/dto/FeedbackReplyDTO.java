package com.example.myapp.service.dto;

import jakarta.validation.constraints.Size;
import java.io.Serializable;

/**
 * An administrator's answer and/or status move. Both fields are optional so the triage page
 * can change the status without writing a reply, or write a reply without naming a status —
 * the service refuses a request that carries neither.
 */
public record FeedbackReplyDTO(
    @Size(max = 2000)
    String reply,

    String status
) implements Serializable {
}
