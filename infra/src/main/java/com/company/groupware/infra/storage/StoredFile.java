package com.company.groupware.infra.storage;

public record StoredFile(
        String key,
        String originalName,
        String contentType,
        long size,
        String url
) {
}
