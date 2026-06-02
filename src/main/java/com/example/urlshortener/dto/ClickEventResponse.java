package com.example.urlshortener.dto;

import com.example.urlshortener.model.ClickEvent;
import java.time.LocalDateTime;

public record ClickEventResponse(
        LocalDateTime clickedAt,
        String ipAddress,
        String userAgent,
        String referrer
) {
    public static ClickEventResponse from(ClickEvent event) {
        return new ClickEventResponse(
                event.getClickedAt(),
                event.getIpAddress(),
                event.getUserAgent(),
                event.getReferrer()
        );
    }
}
