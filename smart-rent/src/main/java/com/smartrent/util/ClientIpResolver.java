package com.smartrent.util;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Best-effort client IP resolution behind the Caddy reverse proxy: plain
 * {@code request.getRemoteAddr()} resolves to Caddy's own address, not the
 * caller's, so anything that rate-limits or audits by IP needs to read
 * {@code X-Forwarded-For} first. Mirrors the semantics of
 * {@code ListingSearchController#extractClientIp} — prefer the first entry
 * of a comma-separated X-Forwarded-For, else fall back to getRemoteAddr().
 *
 * <p>Several controllers (OtpController, ViewController,
 * PhoneClickDetailController, ListingSearchController) already hand-roll this
 * same logic with minor variations. This class is for new call sites only;
 * unifying those existing copies is a separate, out-of-scope change since
 * they are live.
 */
public final class ClientIpResolver {

  private ClientIpResolver() {
  }

  public static String resolve(HttpServletRequest request) {
    String forwarded = request.getHeader("X-Forwarded-For");
    if (forwarded != null && !forwarded.isBlank()) {
      // X-Forwarded-For may contain a comma-separated list; first entry is the client
      return forwarded.split(",")[0].trim();
    }
    return request.getRemoteAddr();
  }
}
