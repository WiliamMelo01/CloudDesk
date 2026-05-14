package wiliammelo.clouddesk.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import wiliammelo.clouddesk.user.User;
import wiliammelo.clouddesk.user.UserRole;

import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Service
public class JwtService {

    private static final String ISSUER = "clouddesk";
    private static final String USER_ID_CLAIM = "userId";
    private static final String ROLE_CLAIM = "role";
    private static final String TOKEN_TYPE_CLAIM = "tokenType";

    private final Clock clock;
    private final Algorithm algorithm;
    private final long accessExpirationSeconds;
    private final long refreshExpirationSeconds;

    @Autowired
    public JwtService(
            @Value("${clouddesk.security.jwt.secret:change-this-development-secret}") String secret,
            @Value("${clouddesk.security.jwt.access-expiration-seconds:900}") long accessExpirationSeconds,
            @Value("${clouddesk.security.jwt.refresh-expiration-seconds:604800}") long refreshExpirationSeconds
    ) {
        this(Clock.systemUTC(), Algorithm.HMAC256(secret), accessExpirationSeconds, refreshExpirationSeconds);
    }

    public JwtService(
            Clock clock,
            Algorithm algorithm,
            long accessExpirationSeconds,
            long refreshExpirationSeconds
    ) {
        this.clock = clock;
        this.algorithm = algorithm;
        this.accessExpirationSeconds = accessExpirationSeconds;
        this.refreshExpirationSeconds = refreshExpirationSeconds;
    }

    public JwtToken createAccessToken(User user) {
        return createToken(user, TokenType.ACCESS, accessExpirationSeconds);
    }

    public JwtToken createRefreshToken(User user) {
        return createToken(user, TokenType.REFRESH, refreshExpirationSeconds);
    }

    public Optional<JwtClaims> parseAccessToken(String token) {
        return parse(token, TokenType.ACCESS);
    }

    public Optional<JwtClaims> parseRefreshToken(String token) {
        return parse(token, TokenType.REFRESH);
    }

    private JwtToken createToken(User user, TokenType tokenType, long expirationSeconds) {
        Instant expiresAt = clock.instant().plusSeconds(expirationSeconds);
        String token = JWT.create()
                .withIssuer(ISSUER)
                .withSubject(user.getEmail())
                .withClaim(USER_ID_CLAIM, user.getId().toString())
                .withClaim(ROLE_CLAIM, user.getRole().name())
                .withClaim(TOKEN_TYPE_CLAIM, tokenType.name())
                .withExpiresAt(Date.from(expiresAt))
                .sign(algorithm);
        return new JwtToken(token, expiresAt);
    }

    private Optional<JwtClaims> parse(String token, TokenType expectedType) {
        try {
            JWTVerifier verifier = JWT.require(algorithm)
                    .withIssuer(ISSUER)
                    .withClaim(TOKEN_TYPE_CLAIM, expectedType.name())
                    .build();
            DecodedJWT jwt = verifier.verify(token);
            return Optional.of(new JwtClaims(
                    UUID.fromString(jwt.getClaim(USER_ID_CLAIM).asString()),
                    jwt.getSubject(),
                    UserRole.valueOf(jwt.getClaim(ROLE_CLAIM).asString()),
                    TokenType.valueOf(jwt.getClaim(TOKEN_TYPE_CLAIM).asString()),
                    jwt.getExpiresAtAsInstant()
            ));
        } catch (IllegalArgumentException | JWTVerificationException exception) {
            return Optional.empty();
        }
    }
}
