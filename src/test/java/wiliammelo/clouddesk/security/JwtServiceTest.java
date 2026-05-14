package wiliammelo.clouddesk.security;

import com.auth0.jwt.algorithms.Algorithm;
import org.junit.jupiter.api.Test;
import wiliammelo.clouddesk.user.User;
import wiliammelo.clouddesk.user.UserRole;

import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private final Clock clock = Clock.fixed(Instant.parse("2030-05-14T18:00:00Z"), ZoneOffset.UTC);
    private final JwtService jwtService = new JwtService(clock, Algorithm.HMAC256("test-secret"), 3600, 7200);
    private final UUID sessionId = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Test
    void createsAndParsesAccessToken() {
        JwtToken token = jwtService.createAccessToken(admin(), sessionId);

        JwtClaims claims = jwtService.parseAccessToken(token.value()).orElseThrow();

        assertThat(token.expiresAt()).isEqualTo(Instant.parse("2030-05-14T19:00:00Z"));
        assertThat(claims.userId()).isEqualTo(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        assertThat(claims.email()).isEqualTo("admin@cloud.test");
        assertThat(claims.role()).isEqualTo(UserRole.ADMIN);
        assertThat(claims.sessionId()).isEqualTo(sessionId);
        assertThat(claims.tokenType()).isEqualTo(TokenType.ACCESS);
        assertThat(claims.expiresAt()).isEqualTo(token.expiresAt());
    }

    @Test
    void createsAndParsesRefreshToken() {
        JwtToken token = jwtService.createRefreshToken(admin(), sessionId);

        JwtClaims claims = jwtService.parseRefreshToken(token.value()).orElseThrow();

        assertThat(token.expiresAt()).isEqualTo(Instant.parse("2030-05-14T20:00:00Z"));
        assertThat(claims.tokenType()).isEqualTo(TokenType.REFRESH);
    }

    @Test
    void rejectsInvalidToken() {
        assertThat(jwtService.parseAccessToken("invalid-token")).isEmpty();
    }

    @Test
    void rejectsAccessTokenWhenRefreshTokenIsExpected() {
        JwtToken accessToken = jwtService.createAccessToken(admin(), sessionId);

        assertThat(jwtService.parseRefreshToken(accessToken.value())).isEmpty();
    }

    @Test
    void rejectsRefreshTokenWhenAccessTokenIsExpected() {
        JwtToken refreshToken = jwtService.createRefreshToken(admin(), sessionId);

        assertThat(jwtService.parseAccessToken(refreshToken.value())).isEmpty();
    }

    @Test
    void rejectsExpiredToken() {
        JwtService expiredService = new JwtService(
                Clock.fixed(Instant.parse("2020-05-14T18:00:00Z"), ZoneOffset.UTC),
                Algorithm.HMAC256("test-secret"),
                -1,
                -1
        );

        assertThat(expiredService.parseAccessToken(expiredService.createAccessToken(admin(), sessionId).value())).isEmpty();
    }

    @Test
    void rejectsTokenSignedWithAnotherSecret() {
        JwtService otherService = new JwtService(clock, Algorithm.HMAC256("other-secret"), 3600, 7200);

        assertThat(jwtService.parseAccessToken(otherService.createAccessToken(admin(), sessionId).value())).isEmpty();
    }

    @Test
    void createsServiceWithProductionConstructor() {
        JwtService service = new JwtService("test-secret", 3600, 7200);

        assertThat(service.createAccessToken(admin(), sessionId).value()).isNotBlank();
    }

    private User admin() {
        User user = new User("Admin", "admin@cloud.test", "hash", UserRole.ADMIN);
        try {
            Field field = User.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, UUID.fromString("11111111-1111-1111-1111-111111111111"));
            return user;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
