package com.smartrent.enums;

/**
 * Lifecycle states for broker verification.
 * <p>
 * Transitions:
 * <pre>
 *   NONE  ──(user registers)──▶  PENDING
 *   PENDING ──(admin approves)──▶ APPROVED
 *   PENDING ──(admin rejects)──▶  REJECTED
 *   REJECTED ──(user re-registers)──▶ PENDING
 * </pre>
 */
public enum BrokerVerificationStatus {
    /** No registration submitted yet (default). */
    NONE,
    /** Registration submitted and awaiting admin review. */
    PENDING,
    /** Admin approved – user is a verified broker. */
    APPROVED,
    /** Admin rejected the registration. */
    REJECTED
}
