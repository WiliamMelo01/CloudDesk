package wiliammelo.clouddesk.session;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import wiliammelo.clouddesk.auth.AuthController;
import wiliammelo.clouddesk.auth.AuthService;
import wiliammelo.clouddesk.auth.LoginResponse;
import wiliammelo.clouddesk.auth.LoginResult;
import wiliammelo.clouddesk.security.JwtPrincipal;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
public class SessionController {

    private final AuthService authService;
    private final SessionService sessionService;

    public SessionController(AuthService authService, SessionService sessionService) {
        this.authService = authService;
        this.sessionService = sessionService;
    }

    @PostMapping("/sessions/refresh")
    public ResponseEntity<LoginResponse> refresh(
            @CookieValue(name = AuthController.REFRESH_TOKEN_COOKIE, required = false) String refreshToken,
            HttpServletRequest request
    ) {
        LoginResult result = authService.refresh(refreshToken, clientRequestInfo(request));
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie(result))
                .body(result.response());
    }

    @PostMapping("/sessions/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = AuthController.REFRESH_TOKEN_COOKIE, required = false) String refreshToken
    ) {
        authService.logout(refreshToken);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, clearRefreshTokenCookie())
                .build();
    }

    @GetMapping("/sessions")
    public List<SessionResponse> list(@AuthenticationPrincipal JwtPrincipal principal) {
        return sessionService.listSessions(principal.userId(), principal.sessionId());
    }

    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<Void> revoke(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID sessionId
    ) {
        sessionService.revokeOtherSession(principal.userId(), principal.sessionId(), sessionId);
        return ResponseEntity.noContent().build();
    }

    private String refreshTokenCookie(LoginResult result) {
        return ResponseCookie.from(AuthController.REFRESH_TOKEN_COOKIE, result.refreshToken().value())
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .maxAge(Duration.between(Instant.now(), result.refreshToken().expiresAt()))
                .build()
                .toString();
    }

    private String clearRefreshTokenCookie() {
        return ResponseCookie.from(AuthController.REFRESH_TOKEN_COOKIE, "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .maxAge(0)
                .build()
                .toString();
    }

    private ClientRequestInfo clientRequestInfo(HttpServletRequest request) {
        return new ClientRequestInfo(request.getRemoteAddr(), request.getHeader("User-Agent"));
    }
}
