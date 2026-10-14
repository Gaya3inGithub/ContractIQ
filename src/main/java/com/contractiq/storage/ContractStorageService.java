package com.contractiq.storage;

import com.contractiq.config.AppConfig;
import com.contractiq.exception.StorageException;
import com.contractiq.util.SecurityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

public class ContractStorageService {
    private static final Logger logger = LoggerFactory.getLogger(ContractStorageService.class);

    private final LocalFileStorage localStorage;
    private final S3CompatibleStorage s3Storage;

    public ContractStorageService() {
        this(new LocalFileStorage(), new S3CompatibleStorage());
    }

    public ContractStorageService(LocalFileStorage localStorage, S3CompatibleStorage s3Storage) {
        this.localStorage = localStorage;
        this.s3Storage = s3Storage;
    }

    public StoredFile saveUploadedFile(String originalFileName, InputStream fileStream) throws IOException {
        String safeOriginalName = SecurityUtil.sanitizeFileName(originalFileName);
        String storageKey = SecurityUtil.generateStorageFileName(safeOriginalName);

        if (AppConfig.isObjectStorageEnabled()) {
            if (!AppConfig.isObjectStorageConfigured()) {
                throw new StorageException("Object storage is enabled but missing required configuration: OBJECT_STORAGE_BUCKET, OBJECT_STORAGE_ENDPOINT, OBJECT_STORAGE_ACCESS_KEY_ID, and OBJECT_STORAGE_SECRET_ACCESS_KEY.");
            }
            return s3Storage.store(storageKey, safeOriginalName, fileStream);
        }

        return localStorage.store(storageKey, safeOriginalName, fileStream);
    }

    public byte[] readBytes(String storageKey) throws IOException {
        if (storageKey == null || storageKey.isBlank()) {
            throw new StorageException("No storage key was recorded for the uploaded PDF.");
        }

        if (AppConfig.isObjectStorageEnabled() && isRemoteStorageKey(storageKey)) {
            return s3Storage.readBytes(storageKey);
        }

        return Files.readAllBytes(Paths.get(storageKey));
    }

    public File materializeToTempFile(String storageKey, String originalFileName) throws IOException {
        if (storageKey == null || storageKey.isBlank()) {
            throw new StorageException("No storage key was recorded for the uploaded PDF.");
        }

        if (AppConfig.isObjectStorageEnabled() && isRemoteStorageKey(storageKey)) {
            return s3Storage.materializeToTempFile(storageKey, originalFileName);
        }

        File localFile = new File(storageKey);
        if (!localFile.exists()) {
            throw new StorageException("Local uploaded PDF is missing from disk: " + storageKey);
        }
        return localFile;
    }

    public void deleteIfExists(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            return;
        }

        try {
            if (AppConfig.isObjectStorageEnabled() && isRemoteStorageKey(storageKey)) {
                s3Storage.delete(storageKey);
                return;
            }

            Path path = Paths.get(storageKey);
            Files.deleteIfExists(path);
        } catch (Exception e) {
            logger.warn("Could not clean up uploaded file '{}': {}", storageKey, e.getMessage());
        }
    }

    public static boolean isRemoteStorageKey(String storageKey) {
        if (storageKey == null) {
            return false;
        }
        String normalized = storageKey.trim();
        if (normalized.isEmpty()) {
            return false;
        }
        if (normalized.startsWith("/") || normalized.startsWith("\\\\") || normalized.matches("^[A-Za-z]:[\\/].*")) {
            return false;
        }
        Path asPath = Paths.get(normalized);
        if (asPath.isAbsolute()) {
            return false;
        }
        String lower = normalized.toLowerCase(Locale.ROOT);
        if (lower.startsWith("uploads/") || lower.startsWith("uploads\\")
                || lower.startsWith("tmp/") || lower.startsWith("tmp\\")
                || lower.startsWith("storage/") || lower.startsWith("storage\\")
                || lower.startsWith("data/") || lower.startsWith("data\\")
                || lower.startsWith("files/") || lower.startsWith("files\\")) {
            return false;
        }
        return true;
    }

    public static class LocalFileStorage {
        public StoredFile store(String storageKey, String originalFileName, InputStream fileStream) throws IOException {
            File uploadDir = new File(AppConfig.getUploadDir());
            if (!uploadDir.exists()) {
                if (!uploadDir.mkdirs()) {
                    throw new StorageException("Could not create local upload directory: " + uploadDir.getAbsolutePath());
                }
            }

            File targetFile = new File(uploadDir, storageKey);
            try (InputStream input = fileStream; java.io.OutputStream out = Files.newOutputStream(targetFile.toPath())) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = input.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
            }
            return new StoredFile(targetFile.getAbsolutePath(), targetFile.getAbsolutePath());
        }
    }

    public static class S3CompatibleStorage {
        public StoredFile store(String storageKey, String originalFileName, InputStream fileStream) throws IOException {
            byte[] bytes = fileStream.readAllBytes();
            String bucket = AppConfig.getObjectStorageBucket();
            String endpoint = AppConfig.getObjectStorageEndpoint();
            String region = AppConfig.getObjectStorageRegion();
            String accessKey = AppConfig.getObjectStorageAccessKeyId();
            String secretKey = AppConfig.getObjectStorageSecretAccessKey();

            if (bucket.isBlank() || endpoint.isBlank() || accessKey.isBlank() || secretKey.isBlank()) {
                throw new StorageException("Object storage is enabled but missing S3-compatible configuration: OBJECT_STORAGE_BUCKET, OBJECT_STORAGE_ENDPOINT, OBJECT_STORAGE_ACCESS_KEY_ID, and OBJECT_STORAGE_SECRET_ACCESS_KEY.");
            }

            try {
                software.amazon.awssdk.auth.credentials.AwsBasicCredentials credentials =
                        software.amazon.awssdk.auth.credentials.AwsBasicCredentials.create(accessKey, secretKey);
                software.amazon.awssdk.regions.Region awsRegion = software.amazon.awssdk.regions.Region.of(region);

                software.amazon.awssdk.services.s3.S3Client s3Client = software.amazon.awssdk.services.s3.S3Client.builder()
                        .region(awsRegion)
                        .credentialsProvider(() -> credentials)
                        .endpointOverride(new java.net.URI(endpoint))
                        .forcePathStyle(true)
                        .build();

                s3Client.putObject(
                        software.amazon.awssdk.services.s3.model.PutObjectRequest.builder()
                                .bucket(bucket)
                                .key(storageKey)
                                .contentType("application/pdf")
                                .contentLength((long) bytes.length)
                                .build(),
                        software.amazon.awssdk.core.sync.RequestBody.fromBytes(bytes));

                return new StoredFile(storageKey, endpoint + "/" + bucket + "/" + storageKey);
            } catch (Exception e) {
                throw new StorageException("Failed to upload contract PDF to configured object storage.", e);
            }
        }

        public byte[] readBytes(String storageKey) throws IOException {
            String bucket = AppConfig.getObjectStorageBucket();
            String endpoint = AppConfig.getObjectStorageEndpoint();
            String region = AppConfig.getObjectStorageRegion();
            String accessKey = AppConfig.getObjectStorageAccessKeyId();
            String secretKey = AppConfig.getObjectStorageSecretAccessKey();

            try {
                software.amazon.awssdk.auth.credentials.AwsBasicCredentials credentials =
                        software.amazon.awssdk.auth.credentials.AwsBasicCredentials.create(accessKey, secretKey);
                software.amazon.awssdk.regions.Region awsRegion = software.amazon.awssdk.regions.Region.of(region);

                try (software.amazon.awssdk.services.s3.S3Client s3Client = software.amazon.awssdk.services.s3.S3Client.builder()
                        .region(awsRegion)
                        .credentialsProvider(() -> credentials)
                        .endpointOverride(new java.net.URI(endpoint))
                        .forcePathStyle(true)
                        .build()) {
                    return s3Client.getObjectAsBytes(
                            software.amazon.awssdk.services.s3.model.GetObjectRequest.builder()
                                    .bucket(bucket)
                                    .key(storageKey)
                                   .build())
                            .asByteArray();
                }
            } catch (Exception e) {
                throw new StorageException("Failed to read the uploaded PDF from object storage.", e);
            }
        }

        public File materializeToTempFile(String storageKey, String originalFileName) throws IOException {
            byte[] bytes = readBytes(storageKey);
            File tempFile = File.createTempFile("contractiq-", "-" + SecurityUtil.sanitizeFileName(originalFileName));
            Files.write(tempFile.toPath(), bytes);
            return tempFile;
        }

        public void delete(String storageKey) {
            String bucket = AppConfig.getObjectStorageBucket();
            String endpoint = AppConfig.getObjectStorageEndpoint();
            String region = AppConfig.getObjectStorageRegion();
            String accessKey = AppConfig.getObjectStorageAccessKeyId();
            String secretKey = AppConfig.getObjectStorageSecretAccessKey();

            try {
                software.amazon.awssdk.auth.credentials.AwsBasicCredentials credentials =
                        software.amazon.awssdk.auth.credentials.AwsBasicCredentials.create(accessKey, secretKey);
                software.amazon.awssdk.regions.Region awsRegion = software.amazon.awssdk.regions.Region.of(region);

                try (software.amazon.awssdk.services.s3.S3Client s3Client = software.amazon.awssdk.services.s3.S3Client.builder()
                        .region(awsRegion)
                        .credentialsProvider(() -> credentials)
                        .endpointOverride(new java.net.URI(endpoint))
                        .forcePathStyle(true)
                        .build()) {
                    s3Client.deleteObject(software.amazon.awssdk.services.s3.model.DeleteObjectRequest.builder()
                            .bucket(bucket)
                            .key(storageKey)
                            .build());
                }
            } catch (Exception e) {
                logger.warn("Could not delete object '{}' from S3-compatible storage: {}", storageKey, e.getMessage());
            }
        }
    }
}
