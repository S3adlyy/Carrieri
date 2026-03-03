package utils.guser;

import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.core.sync.ResponseTransformer;

import java.nio.file.Path;
import java.io.File;
import java.time.Duration;

public class S3StorageService implements AutoCloseable {

    private final String bucket;
    private final Region region;

    private final S3Client s3;
    private final S3Presigner presigner;

    public S3StorageService(String bucket, String region) {
        this.bucket = bucket;
        this.region = Region.of(region);

        // Uses default credentials provider chain (env vars, ~/.aws/credentials, etc.)
        this.s3 = S3Client.builder()
                .region(this.region)
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();

        this.presigner = S3Presigner.builder()
                .region(this.region)
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    public String uploadFile(File file, String key, String contentType) {
        PutObjectRequest req = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType)
                .build();

        s3.putObject(req, RequestBody.fromFile(file));
        return key;
    }

    public String presignedGetUrl(String key, Duration validFor) {
        GetObjectRequest getReq = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

        GetObjectPresignRequest presignReq = GetObjectPresignRequest.builder()
                .signatureDuration(validFor)
                .getObjectRequest(getReq)
                .build();

        PresignedGetObjectRequest presigned = presigner.presignGetObject(presignReq);
        return presigned.url().toExternalForm();
    }
    public void downloadToFile(String key, Path targetPath) {
        GetObjectRequest getReq = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

        s3.getObject(getReq, ResponseTransformer.toFile(targetPath));
    }

    @Override
    public void close() {
        try { presigner.close(); } catch (Exception ignored) {}
        try { s3.close(); } catch (Exception ignored) {}
    }
}
