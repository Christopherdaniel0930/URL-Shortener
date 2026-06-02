package com.example.urlshortener.dto;

public record DashboardStatsResponse(
        long totalUrls,
        long totalClicks,
        long clicksToday
) {
}
