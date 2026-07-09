package com.gwuy.sba301.trafficdetectionbackend.service.impls;

import com.gwuy.sba301.trafficdetectionbackend.configuration.R2Condition;
import com.gwuy.sba301.trafficdetectionbackend.configuration.R2Properties;
import com.gwuy.sba301.trafficdetectionbackend.service.interfaces.ImageStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Conditional(R2Condition.class)
public class R2ImageStorageServiceImpl implements ImageStorageService {

    private final S3Client s3Client;
    private final R2Properties r2Properties;

    @Override
    public String upload(MultipartFile file) {
        String bucketName = r2Properties.getBucketName();
        String key = generateKey(file);

        try {
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(putRequest, software.amazon.awssdk.core.sync.RequestBody.fromInputStream(
                    file.getInputStream(), file.getSize()));

            String url = r2Properties.getPublicBaseUrl() + "/" + key;

            log.info("Uploaded frame to R2: {}", url);
            return url;
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload file to R2", e);
        }
    }

    private String generateKey(MultipartFile file) {
        LocalDate now = LocalDate.now();
        String year = now.format(DateTimeFormatter.ofPattern("yyyy"));
        String month = now.format(DateTimeFormatter.ofPattern("MM"));
        String uuid = UUID.randomUUID().toString();
        String extension = getExtension(file.getOriginalFilename());

        return String.format("frames/%s/%s/%s.%s", year, month, uuid, extension);
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "jpg";
        }
        return filename.substring(filename.lastIndexOf('.') + 1);
    }
}
