package wiliammelo.clouddesk.auth;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import wiliammelo.clouddesk.security.JwtToken;
import wiliammelo.clouddesk.session.ClientRequestInfo;
import wiliammelo.clouddesk.user.UserRole;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthControllerTest {

    private final AuthService authService = mock(AuthService.class);
    private final AuthController authController = new AuthController(authService);

    @Test
    void logsInOwnerAndSetsHttpOnlyRefreshTokenCookie() {
        LoginRequest request = new LoginRequest("owner@cloud.test", "password123");
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.setRemoteAddr("127.0.0.1");
        servletRequest.addHeader("User-Agent", "JUnit");
        LoginResponse response = new LoginResponse(
                "access-token",
                "Bearer",
                Instant.parse("2030-05-14T19:00:00Z"),
                UUID.randomUUID(),
                "owner@cloud.test",
                UserRole.OWNER
        );
        when(authService.login(eq(request), eq(new ClientRequestInfo("127.0.0.1", "JUnit")))).thenReturn(new LoginResult(
                response,
                new JwtToken("refresh-token", Instant.now().plusSeconds(3600))
        ));

        var result = authController.loginOwner(request, servletRequest);

        assertThat(result.getBody()).isEqualTo(response);
        assertThat(result.getHeaders().getFirst(HttpHeaders.SET_COOKIE))
                .contains("refresh_token=refresh-token")
                .contains("HttpOnly")
                .contains("Secure")
                .contains("SameSite=Strict")
                .contains("Path=/");
    }
}
