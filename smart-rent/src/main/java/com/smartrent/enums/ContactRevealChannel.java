package com.smartrent.enums;

/**
 * The contact channel a viewer revealed for a seller. Persisted as a string on
 * {@code contact_reveal_log.channel} and echoed back on the reveal response.
 */
public enum ContactRevealChannel {
    PHONE,
    EMAIL,
    ZALO
}
