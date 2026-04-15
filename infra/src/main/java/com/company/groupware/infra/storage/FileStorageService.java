package com.company.groupware.infra.storage;

import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

/**
 * Local ↔ AWS S3 전환이 용이하도록 추상화한 스토리지 인터페이스.
 * 운영 환경에서는 S3 구현체(추후 추가), 로컬에서는 LocalFileStorageService 사용.
 */
public interface FileStorageService {

    StoredFile store(String directory, MultipartFile file);

    InputStream load(String key);

    void delete(String key);

    String generatePresignedUrl(String key, long expirationSeconds);
}
