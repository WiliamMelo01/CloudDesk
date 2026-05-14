package wiliammelo.clouddesk.storage;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class S3StorageConfigTest {

    private final S3StorageConfig config = new S3StorageConfig();

    @Test
    void createsS3ClientWithEndpointOverride() {
        try (var client = config.s3Client("us-east-1", "http://localhost:4566", "test", "test", true)) {
            assertThat(client).isNotNull();
        }
    }

    @Test
    void createsS3ClientWithoutEndpointOverride() {
        try (var client = config.s3Client("us-east-1", " ", "test", "test", false)) {
            assertThat(client).isNotNull();
        }
    }

    @Test
    void createsS3ClientWhenEndpointIsNull() {
        try (var client = config.s3Client("us-east-1", null, "test", "test", false)) {
            assertThat(client).isNotNull();
        }
    }
}
