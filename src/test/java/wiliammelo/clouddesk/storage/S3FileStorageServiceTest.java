package wiliammelo.clouddesk.storage;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.ByteArrayInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class S3FileStorageServiceTest {

    private final S3Client s3Client = mock(S3Client.class);

    @Test
    void uploadsObjectAndReturnsPublicUrl() {
        S3FileStorageService service = new S3FileStorageService(
                s3Client,
                "clouddesk-company-assets",
                "http://localhost:4566/"
        );

        String url = service.upload(
                "companies/id/logo/logo.png",
                new ByteArrayInputStream("logo".getBytes()),
                4,
                "image/png"
        );

        ArgumentCaptor<PutObjectRequest> request = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(request.capture(), any(RequestBody.class));
        assertThat(request.getValue().bucket()).isEqualTo("clouddesk-company-assets");
        assertThat(request.getValue().key()).isEqualTo("companies/id/logo/logo.png");
        assertThat(request.getValue().contentLength()).isEqualTo(4);
        assertThat(request.getValue().contentType()).isEqualTo("image/png");
        assertThat(url).isEqualTo("http://localhost:4566/clouddesk-company-assets/companies/id/logo/logo.png");
    }

    @Test
    void deletesObject() {
        S3FileStorageService service = new S3FileStorageService(
                s3Client,
                "clouddesk-company-assets",
                "http://localhost:4566"
        );

        service.delete("companies/id/logo/logo.png");

        ArgumentCaptor<DeleteObjectRequest> request = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client).deleteObject(request.capture());
        assertThat(request.getValue().bucket()).isEqualTo("clouddesk-company-assets");
        assertThat(request.getValue().key()).isEqualTo("companies/id/logo/logo.png");
    }
}
