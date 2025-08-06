package com.coubee.coubeebeproduct.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Component
@Profile("stg")
@RequiredArgsConstructor
public class S3Uploader implements FileUploader{

    private final S3Client s3Client;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    /**
     * 파일 업로드
     */
    public String upload(MultipartFile multipartFile, String dirName) {
        String originalFilename = multipartFile.getOriginalFilename();
        String ext = originalFilename.substring(originalFilename.lastIndexOf("."));
        String fileName = dirName + "/" + UUID.randomUUID() + ext;

        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(fileName)
                    .contentType(multipartFile.getContentType())
                    .build();

            s3Client.putObject(request, RequestBody.fromBytes(multipartFile.getBytes()));
            return "/"+fileName;

        } catch (IOException e) {
            throw new RuntimeException("S3 파일 업로드 실패", e);
        }
    }

    /**
     * 파일 삭제
     */
    public void delete(String fileUrl) {
        String key = extractKeyFromUrl(fileUrl);

        DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

        s3Client.deleteObject(deleteRequest);
    }

    /**
     * S3 저장 키로 URL 생성
     */
//    private String getUrl(String key) {
//        return "https://" + bucket + ".s3.amazonaws.com/" + key;
//    }
    @Value("${cloud.aws.cloudfront.domain}")  // 예: cdn.example.com
    private String cloudFrontDomain;

    private String getUrl(String path) {
        return "https://" + cloudFrontDomain + path;
    }
    /**
     * S3 URL에서 key 추출
     */
    private String extractKeyFromUrl(String url) {
        return url.substring(url.indexOf(".com/") + 5);
    }
}