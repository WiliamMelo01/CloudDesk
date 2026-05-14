package wiliammelo.clouddesk.session;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component
public class RefreshTokenHasher {

    private final String algorithm;

    public RefreshTokenHasher() {
        this("SHA-256");
    }

    RefreshTokenHasher(String algorithm) {
        this.algorithm = algorithm;
    }

    public String hash(String refreshToken) {
        try {
            byte[] digest = MessageDigest.getInstance(algorithm)
                    .digest(refreshToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Unable to hash refresh token.", exception);
        }
    }

    public boolean matches(String refreshToken, String refreshTokenHash) {
        return MessageDigest.isEqual(
                hash(refreshToken).getBytes(StandardCharsets.UTF_8),
                refreshTokenHash.getBytes(StandardCharsets.UTF_8)
        );
    }
}
