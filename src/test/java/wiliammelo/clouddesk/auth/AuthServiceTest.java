package wiliammelo.clouddesk.auth;

import com.auth0.jwt.algorithms.Algorithm;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import wiliammelo.clouddesk.security.JwtService;
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
import static org.mockito.Mockito.when;

class AuthServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final JwtService jwtService = new JwtService(
            Clock.fixed(Instant.parse("2026-05-14T18:00:00Z"), ZoneOffset.UTC),
            Algorithm.HMAC256("test-secret"),
            3600,
            7200
    );
    private final AuthService authService = new AuthService(userRepository, passwordEncoder, jwtService);

    @Test
    void logsInActiveAdmin() {
        User admin = user(UserRole.ADMIN, passwordEncoder.encode("password123"));
        when(userRepository.findByEmailIgnoreCase("admin@cloud.test")).thenReturn(Optional.of(admin));

        LoginResult result = authService.login(new LoginRequest(" Admin@Cloud.Test ", "password123"));
        LoginResponse response = result.response();

        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresAt()).isEqualTo(Instant.parse("2026-05-14T19:00:00Z"));
        assertThat(response.userId()).isEqualTo(admin.getId());
        assertThat(response.email()).isEqualTo("admin@cloud.test");
        assertThat(response.role()).isEqualTo(UserRole.ADMIN);
        assertThat(result.refreshToken().value()).isNotBlank();
        assertThat(result.refreshToken().expiresAt()).isEqualTo(Instant.parse("2026-05-14T20:00:00Z"));
    }

    @Test
    void rejectsUnknownEmail() {
        when(userRepository.findByEmailIgnoreCase("missing@cloud.test")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("missing@cloud.test", "password123")))
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("Invalid credentials.");
    }

    @Test
    void rejectsInactiveAdmin() {
        User admin = user(UserRole.ADMIN, passwordEncoder.encode("password123"));
        admin.deactivate();
        when(userRepository.findByEmailIgnoreCase("admin@cloud.test")).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> authService.login(new LoginRequest("admin@cloud.test", "password123")))
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("Invalid credentials.");
    }

    @Test
    void rejectsNonAdminUser() {
        User agent = user(UserRole.AGENT, passwordEncoder.encode("password123"));
        when(userRepository.findByEmailIgnoreCase("admin@cloud.test")).thenReturn(Optional.of(agent));

        assertThatThrownBy(() -> authService.login(new LoginRequest("admin@cloud.test", "password123")))
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("Invalid credentials.");
    }

    @Test
    void rejectsWrongPassword() {
        User admin = user(UserRole.ADMIN, passwordEncoder.encode("password123"));
        when(userRepository.findByEmailIgnoreCase("admin@cloud.test")).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> authService.login(new LoginRequest("admin@cloud.test", "wrongPassword")))
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("Invalid credentials.");
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
}
