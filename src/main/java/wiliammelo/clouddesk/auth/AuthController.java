package wiliammelo.clouddesk.auth;

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
public class AuthController {

    public static final String REFRESH_TOKEN_COOKIE = "refresh_token";

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login/admin")
    public ResponseEntity<LoginResponse> loginAdmin(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpServletRequest
    ) {
        LoginResult result = authService.login(request, clientRequestInfo(httpServletRequest));
        ResponseCookie refreshTokenCookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE, result.refreshToken().value())
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .maxAge(Duration.between(Instant.now(), result.refreshToken().expiresAt()))
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
                .body(result.response());
    }

    private ClientRequestInfo clientRequestInfo(HttpServletRequest request) {
        return new ClientRequestInfo(request.getRemoteAddr(), request.getHeader("User-Agent"));
    }
}
