package com.smartrent.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClientIpResolverTest {

  @Mock HttpServletRequest request;

  @Test
  void resolve_prefersFirstEntryOfForwardedForList() {
    when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.10, 10.0.0.1, 10.0.0.2");

    assertThat(ClientIpResolver.resolve(request)).isEqualTo("203.0.113.10");
  }

  @Test
  void resolve_trimsWhitespaceAroundFirstEntry() {
    when(request.getHeader("X-Forwarded-For")).thenReturn(" 203.0.113.10 ,10.0.0.1");

    assertThat(ClientIpResolver.resolve(request)).isEqualTo("203.0.113.10");
  }

  @Test
  void resolve_fallsBackToRemoteAddrWhenHeaderMissing() {
    when(request.getHeader("X-Forwarded-For")).thenReturn(null);
    when(request.getRemoteAddr()).thenReturn("192.168.1.5");

    assertThat(ClientIpResolver.resolve(request)).isEqualTo("192.168.1.5");
  }

  @Test
  void resolve_fallsBackToRemoteAddrWhenHeaderBlank() {
    when(request.getHeader("X-Forwarded-For")).thenReturn("   ");
    when(request.getRemoteAddr()).thenReturn("192.168.1.5");

    assertThat(ClientIpResolver.resolve(request)).isEqualTo("192.168.1.5");
  }

  @Test
  void resolve_singleIpForwardedForIsReturnedAsIs() {
    when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.10");

    assertThat(ClientIpResolver.resolve(request)).isEqualTo("203.0.113.10");
  }
}
