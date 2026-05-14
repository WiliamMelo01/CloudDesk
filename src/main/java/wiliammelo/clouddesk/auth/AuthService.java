package wiliammelo.clouddesk.auth;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import wiliammelo.clouddesk.security.JwtToken;
import wiliammelo.clouddesk.security.JwtService;
import wiliammelo.clouddesk.user.User;
import wiliammelo.clouddesk.user.UserRepository;
import wiliammelo.clouddesk.user.UserRole;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional(readOnly = true)
    public LoginResult login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();
        User user = userRepository.findByEmailIgnoreCase(email)
                .filter(User::isActive)
                .filter(foundUser -> foundUser.getRole() == UserRole.ADMIN)
                .orElseThrow(() -> new AuthenticationException("Invalid credentials."));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new AuthenticationException("Invalid credentials.");
        }

        JwtToken accessToken = jwtService.createAccessToken(user);
        JwtToken refreshToken = jwtService.createRefreshToken(user);
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
}
