package com.example.urlshortener.dto;

import com.example.urlshortener.model.ShortUrl;
import java.time.LocalDateTime;

public record ShortUrlResponse(
        Long id,
        String originalUrl,
        String shortCode,
        String shortUrl,
        LocalDateTime createdAt,
        LocalDateTime expiresAt,
        long clickCount,
        boolean expired
) {
    public static ShortUrlResponse from(ShortUrl shortUrl, String baseUrl) {
        return new ShortUrlResponse(
                shortUrl.getId(),
                shortUrl.getOriginalUrl(),
                shortUrl.getShortCode(),
                baseUrl + "/" + shortUrl.getShortCode(),
                shortUrl.getCreatedAt(),
                shortUrl.getExpiresAt(),
                shortUrl.getClickCount(),
                shortUrl.isExpired()
        );
    }
}
