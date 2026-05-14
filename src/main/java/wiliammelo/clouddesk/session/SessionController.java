package wiliammelo.clouddesk.session;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Sessions", description = "Active session management")
public class SessionController {

    private final AuthService authService;
    private final SessionService sessionService;

    public SessionController(AuthService authService, SessionService sessionService) {
        this.authService = authService;
        this.sessionService = sessionService;
    }

    @PostMapping("/api/sessions/refresh")
    @Operation(
            summary = "Refresh session",
            description = "Rotates the refresh session using the HttpOnly refresh_token cookie and returns a new access token."
    )
    @ApiResponse(responseCode = "200", description = "Session refreshed")
    @ApiResponse(responseCode = "401", description = "Missing or invalid refresh token")
    public ResponseEntity<LoginResponse> refresh(
            @Parameter(
                    name = AuthController.REFRESH_TOKEN_COOKIE,
                    in = ParameterIn.COOKIE,
                    description = "HttpOnly refresh token cookie set by /api/login/admin"
            )
            @CookieValue(name = AuthController.REFRESH_TOKEN_COOKIE, required = false) String refreshToken,
            HttpServletRequest request
    ) {
        LoginResult result = authService.refresh(refreshToken, clientRequestInfo(request));
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie(result))
                .body(result.response());
    }

    @PostMapping("/api/sessions/logout")
    @Operation(summary = "Logout current session", description = "Revokes the current session using the refresh_token cookie.")
    @ApiResponse(responseCode = "204", description = "Current session revoked")
    @ApiResponse(responseCode = "401", description = "Missing or invalid refresh token")
    public ResponseEntity<Void> logout(
            @Parameter(
                    name = AuthController.REFRESH_TOKEN_COOKIE,
                    in = ParameterIn.COOKIE,
                    description = "HttpOnly refresh token cookie set by /api/login/admin"
            )
            @CookieValue(name = AuthController.REFRESH_TOKEN_COOKIE, required = false) String refreshToken
    ) {
        authService.logout(refreshToken);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, clearRefreshTokenCookie())
                .build();
    }

    @GetMapping("/api/sessions")
    @Operation(summary = "List active sessions", description = "Lists active sessions for the authenticated user.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponse(responseCode = "200", description = "Sessions returned")
    @ApiResponse(responseCode = "401", description = "Missing, invalid, or revoked access token")
    public List<SessionResponse> list(@AuthenticationPrincipal JwtPrincipal principal) {
        return sessionService.listSessions(principal.userId(), principal.sessionId());
    }

    @DeleteMapping("/api/sessions/{sessionId}")
    @Operation(summary = "Revoke another session", description = "Revokes another active session owned by the authenticated user.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponse(responseCode = "204", description = "Session revoked")
    @ApiResponse(responseCode = "401", description = "Missing, invalid, or revoked access token")
    @ApiResponse(responseCode = "404", description = "Session not found")
    @ApiResponse(responseCode = "409", description = "Cannot revoke current session with this endpoint")
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
