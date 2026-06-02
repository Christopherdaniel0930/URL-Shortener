package com.example.urlshortener.service;

import com.example.urlshortener.dto.ClickEventResponse;
import com.example.urlshortener.dto.CreateShortUrlRequest;
import com.example.urlshortener.dto.DashboardStatsResponse;
import com.example.urlshortener.dto.ShortUrlResponse;
import com.example.urlshortener.dto.UrlAnalyticsResponse;
import com.example.urlshortener.exception.ExpiredUrlException;
import com.example.urlshortener.exception.UrlNotFoundException;
import com.example.urlshortener.model.ClickEvent;
import com.example.urlshortener.model.ShortUrl;
import com.example.urlshortener.repository.ClickEventRepository;
import com.example.urlshortener.repository.ShortUrlRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UrlShortenerService {

    private final ShortUrlRepository shortUrlRepository;
    private final ClickEventRepository clickEventRepository;
    private final Base62CodeGenerator codeGenerator;
    private final String baseUrl;

    public UrlShortenerService(
            ShortUrlRepository shortUrlRepository,
            ClickEventRepository clickEventRepository,
            Base62CodeGenerator codeGenerator,
            @Value("${app.base-url}") String baseUrl
    ) {
        this.shortUrlRepository = shortUrlRepository;
        this.clickEventRepository = clickEventRepository;
        this.codeGenerator = codeGenerator;
        this.baseUrl = baseUrl.replaceAll("/+$", "");
    }

    @Transactional
    public ShortUrlResponse create(CreateShortUrlRequest request) {
        ShortUrl shortUrl = new ShortUrl();
        shortUrl.setOriginalUrl(request.getOriginalUrl());
        shortUrl.setShortCode(uniqueCode());
        shortUrl.setCreatedAt(LocalDateTime.now());
        shortUrl.setExpiresAt(request.getExpiresAt());
        shortUrl.setClickCount(0);
        return ShortUrlResponse.from(shortUrlRepository.save(shortUrl), baseUrl);
    }

    @Transactional
    public String resolveAndTrack(String shortCode, HttpServletRequest request) {
        ShortUrl shortUrl = shortUrlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException("Short URL not found"));

        if (shortUrl.isExpired()) {
            throw new ExpiredUrlException("Short URL has expired");
        }

        shortUrl.setClickCount(shortUrl.getClickCount() + 1);

        ClickEvent clickEvent = new ClickEvent();
        clickEvent.setShortUrl(shortUrl);
        clickEvent.setClickedAt(LocalDateTime.now());
        clickEvent.setIpAddress(clientIp(request));
        clickEvent.setUserAgent(trim(request.getHeader("User-Agent"), 512));
        clickEvent.setReferrer(trim(request.getHeader("Referer"), 255));
        clickEventRepository.save(clickEvent);

        return shortUrl.getOriginalUrl();
    }

    @Transactional(readOnly = true)
    public Page<ShortUrlResponse> list(Pageable pageable) {
        return shortUrlRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(shortUrl -> ShortUrlResponse.from(shortUrl, baseUrl));
    }

    @Transactional(readOnly = true)
    public UrlAnalyticsResponse analytics(String shortCode) {
        ShortUrl shortUrl = shortUrlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException("Short URL not found"));
        List<ClickEventResponse> recentClicks = clickEventRepository.findTop25ByShortUrlOrderByClickedAtDesc(shortUrl)
                .stream()
                .map(ClickEventResponse::from)
                .toList();
        return new UrlAnalyticsResponse(ShortUrlResponse.from(shortUrl, baseUrl), recentClicks);
    }

    @Transactional(readOnly = true)
    public DashboardStatsResponse dashboardStats() {
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        return new DashboardStatsResponse(
                shortUrlRepository.count(),
                clickEventRepository.totalClicks(),
                clickEventRepository.countByClickedAtAfter(startOfToday)
        );
    }

    private String uniqueCode() {
        for (int attempt = 0; attempt < 10; attempt++) {
            String code = codeGenerator.generate();
            if (!shortUrlRepository.existsByShortCode(code)) {
                return code;
            }
        }
        throw new IllegalStateException("Could not generate a unique short code");
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String trim(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
