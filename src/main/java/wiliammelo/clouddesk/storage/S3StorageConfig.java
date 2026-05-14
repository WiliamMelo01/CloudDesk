package wiliammelo.clouddesk.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;

@Configuration
public class S3StorageConfig {

    @Bean
    S3Client s3Client(
            @Value("${clouddesk.storage.s3.region}") String region,
            @Value("${clouddesk.storage.s3.endpoint:}") String endpoint,
            @Value("${clouddesk.storage.s3.access-key}") String accessKey,
            @Value("${clouddesk.storage.s3.secret-key}") String secretKey,
            @Value("${clouddesk.storage.s3.path-style-access-enabled:false}") boolean pathStyleAccessEnabled
    ) {
        var builder = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(pathStyleAccessEnabled)
                        .build());

        if (endpoint != null && !endpoint.isBlank()) {
            builder.endpointOverride(URI.create(endpoint));
        }

        return builder.build();
    }
}
