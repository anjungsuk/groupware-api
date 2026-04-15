package com.company.groupware.common.infrastructure.storage;

public record StoredFile(
        String key,
        String originalName,
        String contentType,
        long size,
        String url
) {
}
