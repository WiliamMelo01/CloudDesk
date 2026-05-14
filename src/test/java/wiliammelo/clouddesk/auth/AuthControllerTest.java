package wiliammelo.clouddesk.auth;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import wiliammelo.clouddesk.security.JwtToken;
import wiliammelo.clouddesk.user.UserRole;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthControllerTest {

    private final AuthService authService = mock(AuthService.class);
    private final AuthController authController = new AuthController(authService);

    @Test
    void logsInAdminAndSetsHttpOnlyRefreshTokenCookie() {
        LoginRequest request = new LoginRequest("admin@cloud.test", "password123");
        LoginResponse response = new LoginResponse(
                "access-token",
                "Bearer",
                Instant.parse("2026-05-14T19:00:00Z"),
                UUID.randomUUID(),
                "admin@cloud.test",
                UserRole.ADMIN
        );
        when(authService.login(request)).thenReturn(new LoginResult(
                response,
                new JwtToken("refresh-token", Instant.now().plusSeconds(3600))
        ));

        var result = authController.loginAdmin(request);

        assertThat(result.getBody()).isEqualTo(response);
        assertThat(result.getHeaders().getFirst(HttpHeaders.SET_COOKIE))
                .contains("refresh_token=refresh-token")
                .contains("HttpOnly")
                .contains("Secure")
                .contains("SameSite=Strict")
                .contains("Path=/");
    }
}
