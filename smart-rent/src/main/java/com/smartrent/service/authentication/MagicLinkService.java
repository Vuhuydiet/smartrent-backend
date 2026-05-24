package com.smartrent.service.authentication;

import com.smartrent.dto.request.MagicLinkRequest;
import com.smartrent.dto.request.MagicLinkVerifyRequest;
import com.smartrent.dto.response.MagicLinkResponse;
import com.smartrent.dto.response.MagicLinkVerifyResponse;

/**
 * Passwordless guest-login flow: customers submit only an email, receive a
 * one-time link, and exchange that link for a short-lived access token. No
 * user row is created or stored — the email travels inside the JWT itself.
 */
public interface MagicLinkService {

  /**
   * Generates a single-use magic-link JWT and emails the rendered link to the
   * supplied address. Returns the same email back to the caller without
   * revealing whether an account exists.
   */
  MagicLinkResponse requestLink(MagicLinkRequest request);

  /**
   * Verifies the magic-link token, invalidates it so it cannot be reused, and
   * issues a guest access token bound to the email embedded in the link.
   */
  MagicLinkVerifyResponse verifyLink(MagicLinkVerifyRequest request);
}
