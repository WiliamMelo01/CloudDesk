package wiliammelo.clouddesk.session;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RefreshTokenHasherTest {

    @Test
    void hashesRefreshTokenWithSha256() {
        RefreshTokenHasher hasher = new RefreshTokenHasher();

        String hash = hasher.hash("refresh-token");

        assertThat(hash)
                .isEqualTo("0eb17643d4e9261163783a420859c92c7d212fa9624106a12b510afbec266120")
                .hasSize(64);
    }

    @Test
    void matchesRefreshTokenAgainstHash() {
        RefreshTokenHasher hasher = new RefreshTokenHasher();
        String hash = hasher.hash("refresh-token");

        assertThat(hasher.matches("refresh-token", hash)).isTrue();
        assertThat(hasher.matches("other-refresh-token", hash)).isFalse();
    }

    @Test
    void throwsWhenHashAlgorithmIsUnavailable() {
        RefreshTokenHasher hasher = new RefreshTokenHasher("missing-algorithm");

        assertThatThrownBy(() -> hasher.hash("refresh-token"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Unable to hash refresh token.");
    }
}
