package wiliammelo.clouddesk.session;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import wiliammelo.clouddesk.auth.AuthenticationException;
import wiliammelo.clouddesk.security.JwtClaims;
import wiliammelo.clouddesk.security.TokenType;
import wiliammelo.clouddesk.shared.ConflictException;
import wiliammelo.clouddesk.shared.ResourceNotFoundException;
import wiliammelo.clouddesk.user.User;
import wiliammelo.clouddesk.user.UserRole;

import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SessionServiceTest {

    private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
    private final ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
    private final SetOperations<String, String> setOperations = mock(SetOperations.class);
    private final RefreshTokenHasher refreshTokenHasher = new RefreshTokenHasher();
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final Clock clock = Clock.fixed(Instant.parse("2030-05-14T18:00:00Z"), ZoneOffset.UTC);
    private final SessionService sessionService = new SessionService(redisTemplate, objectMapper, refreshTokenHasher, clock);

    private final UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private final UUID sessionId = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private final UUID otherSessionId = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
    }

    @Test
    void createsNewSessionId() {
        assertThat(sessionService.newSessionId()).isNotNull();
    }

    @Test
    void storesSessionInRedisAndIndexesItByUser() {
        User user = user();

        sessionService.storeSession(
                user,
                sessionId,
                "refresh-token",
                Instant.parse("2030-05-14T20:00:00Z"),
                new ClientRequestInfo("127.0.0.1", "JUnit")
        );

        verify(valueOperations).set(eq("session:" + sessionId), anyString(), eq(Duration.ofHours(2)));
        verify(setOperations).add("user_sessions:" + userId, sessionId.toString());
        verify(redisTemplate).expire("user_sessions:" + userId, Duration.ofHours(2));
    }

    @Test
    void validatesRefreshSessionAndUpdatesLastUsedAt() throws Exception {
        SessionRecord session = session(sessionId, userId, refreshTokenHasher.hash("refresh-token"), Instant.parse("2030-05-14T17:00:00Z"));
        when(valueOperations.get("session:" + sessionId)).thenReturn(write(session));

        SessionRecord result = sessionService.validateRefreshSession(claims(sessionId, userId), "refresh-token");

        assertThat(result.sessionId()).isEqualTo(sessionId);
        assertThat(result.lastUsedAt()).isEqualTo(Instant.parse("2030-05-14T18:00:00Z"));
        verify(valueOperations).set(eq("session:" + sessionId), anyString(), eq(Duration.ofHours(2)));
    }

    @Test
    void rejectsRefreshSessionWhenSessionIsMissing() {
        when(valueOperations.get("session:" + sessionId)).thenReturn(null);

        assertThatThrownBy(() -> sessionService.validateRefreshSession(claims(sessionId, userId), "refresh-token"))
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("Invalid session.");
    }

    @Test
    void rejectsRefreshSessionWhenUserDoesNotMatch() throws Exception {
        SessionRecord session = session(sessionId, UUID.randomUUID(), "hashed-refresh-token", Instant.parse("2030-05-14T17:00:00Z"));
        when(valueOperations.get("session:" + sessionId)).thenReturn(write(session));

        assertThatThrownBy(() -> sessionService.validateRefreshSession(claims(sessionId, userId), "refresh-token"))
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("Invalid session.");
    }

    @Test
    void rejectsRefreshSessionWhenTokenHashDoesNotMatch() throws Exception {
        SessionRecord session = session(sessionId, userId, refreshTokenHasher.hash("other-refresh-token"), Instant.parse("2030-05-14T17:00:00Z"));
        when(valueOperations.get("session:" + sessionId)).thenReturn(write(session));

        assertThatThrownBy(() -> sessionService.validateRefreshSession(claims(sessionId, userId), "refresh-token"))
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("Invalid session.");
    }

    @Test
    void listsSessionsSortedByLastUsedAtAndMarksCurrent() throws Exception {
        SessionRecord current = session(sessionId, userId, "hash-a", Instant.parse("2030-05-14T17:00:00Z"));
        SessionRecord other = session(otherSessionId, userId, "hash-b", Instant.parse("2030-05-14T17:30:00Z"));
        when(setOperations.members("user_sessions:" + userId)).thenReturn(Set.of(sessionId.toString(), otherSessionId.toString()));
        when(valueOperations.get("session:" + sessionId)).thenReturn(write(current));
        when(valueOperations.get("session:" + otherSessionId)).thenReturn(write(other));

        var result = sessionService.listSessions(userId, sessionId);

        assertThat(result).hasSize(2);
        assertThat(result.getFirst().sessionId()).isEqualTo(otherSessionId);
        assertThat(result.getFirst().current()).isFalse();
        assertThat(result.get(1).current()).isTrue();
    }

    @Test
    void returnsEmptyListWhenUserHasNoSessionSet() {
        when(setOperations.members("user_sessions:" + userId)).thenReturn(null);

        assertThat(sessionService.listSessions(userId, sessionId)).isEmpty();
    }

    @Test
    void returnsEmptyListWhenUserSessionSetIsEmpty() {
        when(setOperations.members("user_sessions:" + userId)).thenReturn(Set.of());

        assertThat(sessionService.listSessions(userId, sessionId)).isEmpty();
    }

    @Test
    void ignoresStaleSessionIdsWhenListing() throws Exception {
        when(setOperations.members("user_sessions:" + userId)).thenReturn(Set.of(sessionId.toString()));
        when(valueOperations.get("session:" + sessionId)).thenReturn(null);

        assertThat(sessionService.listSessions(userId, sessionId)).isEmpty();
    }

    @Test
    void returnsTrueWhenSessionIsActiveForUser() throws Exception {
        when(valueOperations.get("session:" + sessionId))
                .thenReturn(write(session(sessionId, userId, "hash", Instant.parse("2030-05-14T17:00:00Z"))));

        assertThat(sessionService.isSessionActive(userId, sessionId)).isTrue();
    }

    @Test
    void returnsFalseWhenSessionIsMissing() {
        when(valueOperations.get("session:" + sessionId)).thenReturn(null);

        assertThat(sessionService.isSessionActive(userId, sessionId)).isFalse();
    }

    @Test
    void returnsFalseWhenSessionBelongsToAnotherUser() throws Exception {
        when(valueOperations.get("session:" + sessionId))
                .thenReturn(write(session(sessionId, UUID.randomUUID(), "hash", Instant.parse("2030-05-14T17:00:00Z"))));

        assertThat(sessionService.isSessionActive(userId, sessionId)).isFalse();
    }

    @Test
    void revokesCurrentSessionFromRefreshToken() throws Exception {
        SessionRecord session = session(sessionId, userId, refreshTokenHasher.hash("refresh-token"), Instant.parse("2030-05-14T17:00:00Z"));
        when(valueOperations.get("session:" + sessionId)).thenReturn(write(session));

        sessionService.revokeCurrentSession(claims(sessionId, userId), "refresh-token");

        verify(redisTemplate).delete("session:" + sessionId);
        verify(setOperations).remove("user_sessions:" + userId, sessionId.toString());
    }

    @Test
    void rejectsRevokingCurrentSessionThroughOtherSessionEndpoint() {
        assertThatThrownBy(() -> sessionService.revokeOtherSession(userId, sessionId, sessionId))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Use logout to revoke the current session.");
    }

    @Test
    void revokesOtherSession() throws Exception {
        SessionRecord other = session(otherSessionId, userId, "hash", Instant.parse("2030-05-14T17:00:00Z"));
        when(valueOperations.get("session:" + otherSessionId)).thenReturn(write(other));

        sessionService.revokeOtherSession(userId, sessionId, otherSessionId);

        verify(redisTemplate).delete("session:" + otherSessionId);
        verify(setOperations).remove("user_sessions:" + userId, otherSessionId.toString());
    }

    @Test
    void rejectsRevokingMissingSession() {
        when(valueOperations.get("session:" + otherSessionId)).thenReturn(null);

        assertThatThrownBy(() -> sessionService.revokeOtherSession(userId, sessionId, otherSessionId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Session not found.");
    }

    @Test
    void rejectsRevokingAnotherUsersSession() throws Exception {
        SessionRecord other = session(otherSessionId, UUID.randomUUID(), "hash", Instant.parse("2030-05-14T17:00:00Z"));
        when(valueOperations.get("session:" + otherSessionId)).thenReturn(write(other));

        assertThatThrownBy(() -> sessionService.revokeOtherSession(userId, sessionId, otherSessionId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Session not found.");
    }

    @Test
    void throwsWhenSessionCannotBeSerialized() throws Exception {
        ObjectMapper brokenMapper = mock(ObjectMapper.class);
        SessionService brokenService = new SessionService(redisTemplate, brokenMapper, refreshTokenHasher, clock);
        when(brokenMapper.writeValueAsString(any(SessionRecord.class))).thenThrow(new JsonProcessingException("broken") {
        });

        assertThatThrownBy(() -> brokenService.storeSession(
                user(),
                sessionId,
                "refresh-token",
                Instant.parse("2030-05-14T20:00:00Z"),
                new ClientRequestInfo("127.0.0.1", "JUnit")
        )).isInstanceOf(IllegalStateException.class)
                .hasMessage("Unable to serialize session.");
    }

    @Test
    void throwsWhenSessionCannotBeDeserialized() {
        when(setOperations.members("user_sessions:" + userId)).thenReturn(Set.of(sessionId.toString()));
        when(valueOperations.get("session:" + sessionId)).thenReturn("{");

        assertThatThrownBy(() -> sessionService.listSessions(userId, sessionId))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void productionConstructorCreatesService() {
        assertThat(new SessionService(redisTemplate, refreshTokenHasher).newSessionId()).isNotNull();
    }

    private JwtClaims claims(UUID id, UUID ownerId) {
        return new JwtClaims(
                ownerId,
                "owner@cloud.test",
                UserRole.OWNER,
                id,
                TokenType.REFRESH,
                Instant.parse("2030-05-14T20:00:00Z")
        );
    }

    private SessionRecord session(UUID id, UUID ownerId, String hash, Instant lastUsedAt) {
        return new SessionRecord(
                id,
                ownerId,
                "owner@cloud.test",
                UserRole.OWNER,
                hash,
                Instant.parse("2030-05-14T16:00:00Z"),
                Instant.parse("2030-05-14T20:00:00Z"),
                lastUsedAt,
                "127.0.0.1",
                "JUnit"
        );
    }

    private User user() {
        User user = new User("Owner", "owner@cloud.test", "hash", UserRole.OWNER);
        try {
            Field field = User.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, userId);
            return user;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private String write(SessionRecord session) throws Exception {
        return objectMapper.writeValueAsString(session);
    }
}
