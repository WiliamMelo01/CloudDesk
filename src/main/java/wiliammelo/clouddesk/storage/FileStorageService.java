package wiliammelo.clouddesk.storage;

import java.io.InputStream;

public interface FileStorageService {

    String upload(String objectKey, InputStream inputStream, long contentLength, String contentType);

    void delete(String objectKey);
}
