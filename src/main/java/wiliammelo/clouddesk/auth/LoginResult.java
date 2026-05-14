package wiliammelo.clouddesk.auth;

import wiliammelo.clouddesk.security.JwtToken;

public record LoginResult(
        LoginResponse response,
        JwtToken refreshToken
) {
}
