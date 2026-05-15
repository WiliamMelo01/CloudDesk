package wiliammelo.clouddesk.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import wiliammelo.clouddesk.session.ClientRequestInfo;

import java.time.Duration;
import java.time.Instant;

@RestController
@Tag(name = "Authentication", description = "Login endpoints")
public class AuthController {

    public static final String REFRESH_TOKEN_COOKIE = "refresh_token";

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/api/login/owner")
    @Operation(
            summary = "Owner login",
            description = "Authenticates an owner, returns an access token in the response body, and sets a HttpOnly refresh_token cookie."
    )
    @ApiResponse(responseCode = "200", description = "Authenticated")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @ApiResponse(responseCode = "401", description = "Invalid credentials")
    public ResponseEntity<LoginResponse> loginOwner(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpServletRequest
    ) {
        LoginResult result = authService.login(request, clientRequestInfo(httpServletRequest));
        ResponseCookie refreshTokenCookie = refreshTokenCookie(result);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
                .body(result.response());
    }

    @PostMapping("/api/login/agent")
    @Operation(
            summary = "Agent login",
            description = "Authenticates an agent, returns an access token in the response body, and sets a HttpOnly refresh_token cookie."
    )
    @ApiResponse(responseCode = "200", description = "Authenticated")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @ApiResponse(responseCode = "401", description = "Invalid credentials")
    public ResponseEntity<LoginResponse> loginAgent(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpServletRequest
    ) {
        LoginResult result = authService.login(request, clientRequestInfo(httpServletRequest), wiliammelo.clouddesk.user.UserRole.AGENT);
        ResponseCookie refreshTokenCookie = refreshTokenCookie(result);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
                .body(result.response());
    }

    private ResponseCookie refreshTokenCookie(LoginResult result) {
        ResponseCookie refreshTokenCookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE, result.refreshToken().value())
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .maxAge(Duration.between(Instant.now(), result.refreshToken().expiresAt()))
                .build();
        return refreshTokenCookie;
    }

    private ClientRequestInfo clientRequestInfo(HttpServletRequest request) {
        return new ClientRequestInfo(request.getRemoteAddr(), request.getHeader("User-Agent"));
    }
}
