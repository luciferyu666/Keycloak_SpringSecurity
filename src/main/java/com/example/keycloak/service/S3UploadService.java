package com.example.keycloak.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * 使用 AWS SDK v2 上傳檔案至 S3。
 * 若改用 MinIO，流程類似，可使用 MinIO 官方的 Java SDK。
 */
@Service
public class S3UploadService {

    // 透過 Spring Boot 設定檔或環境變數讀取 S3 資訊
    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    @Value("${cloud.aws.s3.region}")
    private String region;

    @Value("${cloud.aws.s3.access-key}")
    private String accessKey;

    @Value("${cloud.aws.s3.secret-key}")
    private String secretKey;

    /**
     * 若使用 AWS 官方，就不一定需要 endpointOverride；
     * 若使用其他區域或 MinIO，需自訂 Endpoint。
     */
    @Value("${cloud.aws.s3.endpoint:}")
    private String endpoint;

    private S3Client s3Client;

    /**
     * 服務初始化時建立 S3Client 連線
     */
    @PostConstruct
    public void init() {
        // 建立 AWS 憑證物件
        AwsBasicCredentials awsCreds = AwsBasicCredentials.create(accessKey, secretKey);

        // 生成 S3Client Builder
        S3Client.Builder s3Builder = S3Client.builder()
                .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
                .region(Region.of(region));

        // 若有自訂 Endpoint（用於 MinIO 或其他非官方 AWS Endpoint）
        if (endpoint != null && !endpoint.isEmpty()) {
            s3Builder.endpointOverride(URI.create(endpoint));
        }

        this.s3Client = s3Builder.build();
    }

    /**
     * 上傳單檔案至 S3 Bucket，並回傳存放檔案的公開 URL（或 key）。
     */
    public String uploadFile(MultipartFile file) throws IOException {
        // 1. 取出原始檔名以抓副檔名
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        // 2. 使用 UUID 產生唯一檔名
        String uniqueFileName = UUID.randomUUID().toString() + extension;

        // 3. 建立 PutObjectRequest
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(uniqueFileName)
                .contentType(file.getContentType()) // 可選，方便在 S3 知道檔案 MIME
                .build();

        // 4. 執行上傳 (blocking)
        s3Client.putObject(putObjectRequest, RequestBody.fromBytes(file.getBytes()));

        // 5. 回傳檔案的 URL 或純粹回傳 uniqueFileName
        // 若使用官方 AWS Endpoints，URL 一般為: https://{bucket}.s3.{region}.amazonaws.com/{key}
        String fileUrl = generateFileUrl(uniqueFileName);

        return fileUrl;
    }

    /**
     * 產生檔案的存取URL（若Bucket設定公開讀取，前端可直接存取）
     * 若採用私有Bucket，則需使用 Pre-Signed URL 或經後端轉存。
     */
    private String generateFileUrl(String key) {
        // 以官方AWS為例: https://<bucket>.s3.<region>.amazonaws.com/<key>
        if (endpoint != null && !endpoint.isEmpty()) {
            // 表示可能是 MinIO, 其 URL 可能不同
            // https://<endpoint>/<bucket>/<key>
            // 依實際情境組合
            return String.format("%s/%s/%s", endpoint.replaceAll("/$", ""), bucketName, key);
        } else {
            return String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, key);
        }
    }

}
