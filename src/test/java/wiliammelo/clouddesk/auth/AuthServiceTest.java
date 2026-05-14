package wiliammelo.clouddesk.auth;

import com.auth0.jwt.algorithms.Algorithm;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import wiliammelo.clouddesk.security.JwtClaims;
import wiliammelo.clouddesk.security.JwtService;
import wiliammelo.clouddesk.security.TokenType;
import wiliammelo.clouddesk.session.ClientRequestInfo;
import wiliammelo.clouddesk.session.SessionRecord;
import wiliammelo.clouddesk.session.SessionService;
import wiliammelo.clouddesk.user.User;
import wiliammelo.clouddesk.user.UserRepository;
import wiliammelo.clouddesk.user.UserRole;

import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final SessionService sessionService = mock(SessionService.class);
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final JwtService jwtService = new JwtService(
            Clock.fixed(Instant.parse("2030-05-14T18:00:00Z"), ZoneOffset.UTC),
            Algorithm.HMAC256("test-secret"),
            3600,
            7200
    );
    private final AuthService authService = new AuthService(userRepository, passwordEncoder, jwtService, sessionService);
    private final ClientRequestInfo clientRequestInfo = new ClientRequestInfo("127.0.0.1", "JUnit");
    private final UUID sessionId = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Test
    void logsInActiveAdmin() {
        User admin = user(UserRole.ADMIN, passwordEncoder.encode("password123"));
        when(userRepository.findByEmailIgnoreCase("admin@cloud.test")).thenReturn(Optional.of(admin));
        when(sessionService.newSessionId()).thenReturn(sessionId);

        LoginResult result = authService.login(new LoginRequest(" Admin@Cloud.Test ", "password123"), clientRequestInfo);
        LoginResponse response = result.response();

        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresAt()).isEqualTo(Instant.parse("2030-05-14T19:00:00Z"));
        assertThat(response.userId()).isEqualTo(admin.getId());
        assertThat(response.email()).isEqualTo("admin@cloud.test");
        assertThat(response.role()).isEqualTo(UserRole.ADMIN);
        assertThat(result.refreshToken().value()).isNotBlank();
        assertThat(result.refreshToken().expiresAt()).isEqualTo(Instant.parse("2030-05-14T20:00:00Z"));
        verify(sessionService).storeSession(admin, sessionId, result.refreshToken().value(), result.refreshToken().expiresAt(), clientRequestInfo);
    }

    @Test
    void rejectsUnknownEmail() {
        when(userRepository.findByEmailIgnoreCase("missing@cloud.test")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("missing@cloud.test", "password123"), clientRequestInfo))
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("Invalid credentials.");
    }

    @Test
    void rejectsInactiveAdmin() {
        User admin = user(UserRole.ADMIN, passwordEncoder.encode("password123"));
        admin.deactivate();
        when(userRepository.findByEmailIgnoreCase("admin@cloud.test")).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> authService.login(new LoginRequest("admin@cloud.test", "password123"), clientRequestInfo))
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("Invalid credentials.");
    }

    @Test
    void rejectsNonAdminUser() {
        User agent = user(UserRole.AGENT, passwordEncoder.encode("password123"));
        when(userRepository.findByEmailIgnoreCase("admin@cloud.test")).thenReturn(Optional.of(agent));

        assertThatThrownBy(() -> authService.login(new LoginRequest("admin@cloud.test", "password123"), clientRequestInfo))
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("Invalid credentials.");
    }

    @Test
    void rejectsWrongPassword() {
        User admin = user(UserRole.ADMIN, passwordEncoder.encode("password123"));
        when(userRepository.findByEmailIgnoreCase("admin@cloud.test")).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> authService.login(new LoginRequest("admin@cloud.test", "wrongPassword"), clientRequestInfo))
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("Invalid credentials.");
    }

    @Test
    void refreshesSessionWithRotation() {
        User admin = user(UserRole.ADMIN, passwordEncoder.encode("password123"));
        String refreshToken = jwtService.createRefreshToken(admin, sessionId).value();
        SessionRecord oldSession = session(admin, sessionId, refreshToken);
        UUID newSessionId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        when(sessionService.validateRefreshSession(jwtService.parseRefreshToken(refreshToken).orElseThrow(), refreshToken))
                .thenReturn(oldSession);
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(sessionService.newSessionId()).thenReturn(newSessionId);

        LoginResult result = authService.refresh(refreshToken, clientRequestInfo);

        assertThat(result.response().accessToken()).isNotBlank();
        assertThat(jwtService.parseAccessToken(result.response().accessToken()).orElseThrow().sessionId()).isEqualTo(newSessionId);
        verify(sessionService).delete(oldSession);
        verify(sessionService).storeSession(admin, newSessionId, result.refreshToken().value(), result.refreshToken().expiresAt(), clientRequestInfo);
    }

    @Test
    void rejectsRefreshWhenCookieIsMissing() {
        assertThatThrownBy(() -> authService.refresh(null, clientRequestInfo))
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("Refresh token is required.");
    }

    @Test
    void rejectsRefreshWhenCookieIsBlank() {
        assertThatThrownBy(() -> authService.refresh(" ", clientRequestInfo))
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("Refresh token is required.");
    }

    @Test
    void rejectsInvalidRefreshToken() {
        assertThatThrownBy(() -> authService.refresh("invalid", clientRequestInfo))
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("Invalid refresh token.");
    }

    @Test
    void rejectsRefreshWhenUserIsInactive() {
        User admin = user(UserRole.ADMIN, passwordEncoder.encode("password123"));
        String refreshToken = jwtService.createRefreshToken(admin, sessionId).value();
        SessionRecord oldSession = session(admin, sessionId, refreshToken);
        admin.deactivate();
        JwtClaims claims = jwtService.parseRefreshToken(refreshToken).orElseThrow();
        when(sessionService.validateRefreshSession(claims, refreshToken)).thenReturn(oldSession);
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> authService.refresh(refreshToken, clientRequestInfo))
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("Invalid session.");
    }

    @Test
    void logsOutCurrentSession() {
        User admin = user(UserRole.ADMIN, passwordEncoder.encode("password123"));
        String refreshToken = jwtService.createRefreshToken(admin, sessionId).value();
        JwtClaims claims = jwtService.parseRefreshToken(refreshToken).orElseThrow();

        authService.logout(refreshToken);

        verify(sessionService).revokeCurrentSession(claims, refreshToken);
    }

    private User user(UserRole role, String passwordHash) {
        User user = new User("Admin", "admin@cloud.test", passwordHash, role);
        setId(user, UUID.fromString("11111111-1111-1111-1111-111111111111"));
        return user;
    }

    private void setId(User user, UUID id) {
        try {
            Field field = User.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, id);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private SessionRecord session(User user, UUID id, String refreshToken) {
        return new SessionRecord(
                id,
                user.getId(),
                user.getEmail(),
                user.getRole(),
                "hash:" + refreshToken,
                Instant.parse("2030-05-14T18:00:00Z"),
                Instant.parse("2030-05-14T20:00:00Z"),
                Instant.parse("2030-05-14T18:00:00Z"),
                "127.0.0.1",
                "JUnit"
        );
    }
}
