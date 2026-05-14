package wiliammelo.clouddesk.session;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import wiliammelo.clouddesk.auth.AuthenticationException;
import wiliammelo.clouddesk.security.JwtClaims;
import wiliammelo.clouddesk.shared.ConflictException;
import wiliammelo.clouddesk.shared.ResourceNotFoundException;
import wiliammelo.clouddesk.user.User;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class SessionService {

    private static final String SESSION_KEY_PREFIX = "session:";
    private static final String USER_SESSIONS_KEY_PREFIX = "user_sessions:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    @Autowired
    public SessionService(StringRedisTemplate redisTemplate, PasswordEncoder passwordEncoder) {
        this(redisTemplate, new ObjectMapper().findAndRegisterModules(), passwordEncoder, Clock.systemUTC());
    }

    public SessionService(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            PasswordEncoder passwordEncoder,
            Clock clock
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    public UUID newSessionId() {
        return UUID.randomUUID();
    }

    public void storeSession(
            User user,
            UUID sessionId,
            String refreshToken,
            Instant expiresAt,
            ClientRequestInfo clientRequestInfo
    ) {
        Instant now = clock.instant();
        SessionRecord session = new SessionRecord(
                sessionId,
                user.getId(),
                user.getEmail(),
                user.getRole(),
                passwordEncoder.encode(refreshToken),
                now,
                expiresAt,
                now,
                clientRequestInfo.ipAddress(),
                clientRequestInfo.userAgent()
        );

        Duration ttl = Duration.between(now, expiresAt);
        redisTemplate.opsForValue().set(sessionKey(sessionId), write(session), ttl);
        redisTemplate.opsForSet().add(userSessionsKey(user.getId()), sessionId.toString());
        redisTemplate.expire(userSessionsKey(user.getId()), ttl);
    }

    public SessionRecord validateRefreshSession(JwtClaims claims, String refreshToken) {
        SessionRecord session = findById(claims.sessionId())
                .filter(found -> found.userId().equals(claims.userId()))
                .filter(found -> passwordEncoder.matches(refreshToken, found.refreshTokenHash()))
                .orElseThrow(() -> new AuthenticationException("Invalid session."));

        SessionRecord usedSession = session.markUsed(clock.instant());
        redisTemplate.opsForValue().set(
                sessionKey(usedSession.sessionId()),
                write(usedSession),
                Duration.between(clock.instant(), usedSession.expiresAt())
        );
        return usedSession;
    }

    public List<SessionResponse> listSessions(UUID userId, UUID currentSessionId) {
        Set<String> sessionIds = redisTemplate.opsForSet().members(userSessionsKey(userId));
        if (sessionIds == null || sessionIds.isEmpty()) {
            return List.of();
        }

        return sessionIds.stream()
                .map(UUID::fromString)
                .map(this::findById)
                .flatMap(Optional::stream)
                .sorted(Comparator.comparing(SessionRecord::lastUsedAt).reversed())
                .map(session -> SessionResponse.from(session, currentSessionId))
                .toList();
    }

    public void revokeCurrentSession(JwtClaims claims, String refreshToken) {
        SessionRecord session = validateRefreshSession(claims, refreshToken);
        delete(session);
    }

    public void revokeOtherSession(UUID userId, UUID currentSessionId, UUID sessionId) {
        if (sessionId.equals(currentSessionId)) {
            throw new ConflictException("Use logout to revoke the current session.");
        }

        SessionRecord session = findById(sessionId)
                .filter(found -> found.userId().equals(userId))
                .orElseThrow(() -> new ResourceNotFoundException("Session not found."));
        delete(session);
    }

    public void delete(SessionRecord session) {
        redisTemplate.delete(sessionKey(session.sessionId()));
        redisTemplate.opsForSet().remove(userSessionsKey(session.userId()), session.sessionId().toString());
    }

    private Optional<SessionRecord> findById(UUID sessionId) {
        String value = redisTemplate.opsForValue().get(sessionKey(sessionId));
        if (value == null) {
            return Optional.empty();
        }
        return Optional.of(read(value));
    }

    private String write(SessionRecord session) {
        try {
            return objectMapper.writeValueAsString(session);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize session.", exception);
        }
    }

    private SessionRecord read(String value) {
        try {
            return objectMapper.readValue(value, SessionRecord.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to deserialize session.", exception);
        }
    }

    private String sessionKey(UUID sessionId) {
        return SESSION_KEY_PREFIX + sessionId;
    }

    private String userSessionsKey(UUID userId) {
        return USER_SESSIONS_KEY_PREFIX + userId;
    }
}
