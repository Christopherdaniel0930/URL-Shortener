package com.example.urlshortener.repository;

import com.example.urlshortener.model.ClickEvent;
import com.example.urlshortener.model.ShortUrl;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ClickEventRepository extends JpaRepository<ClickEvent, Long> {

    List<ClickEvent> findTop25ByShortUrlOrderByClickedAtDesc(ShortUrl shortUrl);

    long countByClickedAtAfter(LocalDateTime clickedAt);

    @Query("select coalesce(sum(u.clickCount), 0) from ShortUrl u")
    long totalClicks();
}
