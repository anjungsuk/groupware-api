package com.company.groupware.common.infrastructure.storage;

import com.company.groupware.common.exception.BusinessException;
import com.company.groupware.common.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@ConditionalOnProperty(prefix = "app.storage", name = "type", havingValue = "local", matchIfMissing = true)
public class LocalFileStorageService implements FileStorageService {

    private final Path rootLocation;
    private final String baseUrl;

    public LocalFileStorageService(
            @Value("${app.storage.local.path:./storage}") String location,
            @Value("${app.storage.local.base-url:http://localhost:8080/files}") String baseUrl) {
        this.rootLocation = Paths.get(location).toAbsolutePath().normalize();
        this.baseUrl = baseUrl;
        try {
            Files.createDirectories(this.rootLocation);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED, e);
        }
    }

    @Override
    public StoredFile store(String directory, MultipartFile file) {
        try {
            String key = directory + "/" + UUID.randomUUID() + "-" + file.getOriginalFilename();
            Path target = rootLocation.resolve(key).normalize();
            Files.createDirectories(target.getParent());
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return new StoredFile(key, file.getOriginalFilename(),
                    file.getContentType(), file.getSize(), baseUrl + "/" + key);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED, e);
        }
    }

    @Override
    public InputStream load(String key) {
        try {
            return Files.newInputStream(rootLocation.resolve(key).normalize());
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.FILE_NOT_FOUND, e);
        }
    }

    @Override
    public void delete(String key) {
        try {
            Files.deleteIfExists(rootLocation.resolve(key).normalize());
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED, e);
        }
    }

    @Override
    public String generatePresignedUrl(String key, long expirationSeconds) {
        return baseUrl + "/" + key;
    }
}
