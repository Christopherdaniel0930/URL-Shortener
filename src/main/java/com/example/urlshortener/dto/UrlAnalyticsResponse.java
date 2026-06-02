package com.example.urlshortener.dto;

import java.util.List;

public record UrlAnalyticsResponse(
        ShortUrlResponse url,
        List<ClickEventResponse> recentClicks
) {
}
