package wiliammelo.clouddesk.session;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import wiliammelo.clouddesk.auth.AuthController;
import wiliammelo.clouddesk.auth.AuthService;
import wiliammelo.clouddesk.auth.LoginResponse;
import wiliammelo.clouddesk.auth.LoginResult;
import wiliammelo.clouddesk.security.JwtPrincipal;
import wiliammelo.clouddesk.security.JwtToken;
import wiliammelo.clouddesk.user.UserRole;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SessionControllerTest {

    private final AuthService authService = mock(AuthService.class);
    private final SessionService sessionService = mock(SessionService.class);
    private final SessionController controller = new SessionController(authService, sessionService);

    @Test
    void refreshesSessionAndSetsCookie() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("User-Agent", "JUnit");
        LoginResponse response = loginResponse();
        when(authService.refresh("refresh-token", new ClientRequestInfo("127.0.0.1", "JUnit")))
                .thenReturn(new LoginResult(response, new JwtToken("new-refresh-token", Instant.now().plusSeconds(3600))));

        var result = controller.refresh("refresh-token", request);

        assertThat(result.getBody()).isEqualTo(response);
        assertThat(result.getHeaders().getFirst(HttpHeaders.SET_COOKIE))
                .contains(AuthController.REFRESH_TOKEN_COOKIE + "=new-refresh-token")
                .contains("HttpOnly")
                .contains("Secure")
                .contains("SameSite=Strict");
    }

    @Test
    void logsOutCurrentSessionAndClearsCookie() {
        var result = controller.logout("refresh-token");

        verify(authService).logout("refresh-token");
        assertThat(result.getStatusCode().value()).isEqualTo(204);
        assertThat(result.getHeaders().getFirst(HttpHeaders.SET_COOKIE))
                .contains(AuthController.REFRESH_TOKEN_COOKIE + "=")
                .contains("Max-Age=0");
    }

    @Test
    void listsCurrentUserSessions() {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        JwtPrincipal principal = new JwtPrincipal(userId, "owner@cloud.test", UserRole.OWNER, sessionId);
        List<SessionResponse> sessions = List.of(new SessionResponse(
                sessionId,
                UserRole.OWNER,
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Instant.now(),
                "127.0.0.1",
                "JUnit",
                true
        ));
        when(sessionService.listSessions(userId, sessionId)).thenReturn(sessions);

        assertThat(controller.list(principal)).isEqualTo(sessions);
    }

    @Test
    void revokesOtherSession() {
        UUID userId = UUID.randomUUID();
        UUID currentSessionId = UUID.randomUUID();
        UUID revokedSessionId = UUID.randomUUID();
        JwtPrincipal principal = new JwtPrincipal(userId, "owner@cloud.test", UserRole.OWNER, currentSessionId);

        var result = controller.revoke(principal, revokedSessionId);

        verify(sessionService).revokeOtherSession(userId, currentSessionId, revokedSessionId);
        assertThat(result.getStatusCode().value()).isEqualTo(204);
    }

    private LoginResponse loginResponse() {
        return new LoginResponse(
                "access-token",
                "Bearer",
                Instant.now().plusSeconds(900),
                UUID.randomUUID(),
                "owner@cloud.test",
                UserRole.OWNER
        );
    }
}
