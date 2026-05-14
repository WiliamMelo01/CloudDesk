package wiliammelo.clouddesk.auth;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import wiliammelo.clouddesk.security.JwtClaims;
import wiliammelo.clouddesk.security.JwtToken;
import wiliammelo.clouddesk.security.JwtService;
import wiliammelo.clouddesk.session.ClientRequestInfo;
import wiliammelo.clouddesk.session.SessionRecord;
import wiliammelo.clouddesk.session.SessionService;
import wiliammelo.clouddesk.user.User;
import wiliammelo.clouddesk.user.UserRepository;
import wiliammelo.clouddesk.user.UserRole;

import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final SessionService sessionService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            SessionService sessionService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.sessionService = sessionService;
    }

    @Transactional(readOnly = true)
    public LoginResult login(LoginRequest request, ClientRequestInfo clientRequestInfo) {
        String email = request.email().trim().toLowerCase();
        User user = userRepository.findByEmailIgnoreCase(email)
                .filter(User::isActive)
                .filter(foundUser -> foundUser.getRole() == UserRole.OWNER)
                .orElseThrow(() -> new AuthenticationException("Invalid credentials."));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new AuthenticationException("Invalid credentials.");
        }

        return createLoginResult(user, sessionService.newSessionId(), clientRequestInfo);
    }

    @Transactional(readOnly = true)
    public LoginResult refresh(String refreshToken, ClientRequestInfo clientRequestInfo) {
        JwtClaims claims = parseRefreshToken(refreshToken);
        SessionRecord oldSession = sessionService.validateRefreshSession(claims, refreshToken);
        User user = userRepository.findById(oldSession.userId())
                .filter(User::isActive)
                .orElseThrow(() -> new AuthenticationException("Invalid session."));

        sessionService.delete(oldSession);
        return createLoginResult(user, sessionService.newSessionId(), clientRequestInfo);
    }

    public void logout(String refreshToken) {
        JwtClaims claims = parseRefreshToken(refreshToken);
        sessionService.revokeCurrentSession(claims, refreshToken);
    }

    private LoginResult createLoginResult(User user, UUID sessionId, ClientRequestInfo clientRequestInfo) {
        JwtToken accessToken = jwtService.createAccessToken(user, sessionId);
        JwtToken refreshToken = jwtService.createRefreshToken(user, sessionId);
        sessionService.storeSession(user, sessionId, refreshToken.value(), refreshToken.expiresAt(), clientRequestInfo);
        LoginResponse response = new LoginResponse(
                accessToken.value(),
                "Bearer",
                accessToken.expiresAt(),
                user.getId(),
                user.getEmail(),
                user.getRole()
        );
        return new LoginResult(response, refreshToken);
    }

    private JwtClaims parseRefreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new AuthenticationException("Refresh token is required.");
        }
        return jwtService.parseRefreshToken(refreshToken)
                .orElseThrow(() -> new AuthenticationException("Invalid refresh token."));
    }
}
