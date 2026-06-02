# URL Shortener

Spring Boot REST API with MySQL persistence, redirect click tracking, expiry dates, and a simple analytics dashboard.

## Requirements

- Java 17+
- Maven 3.9+
- MySQL 8+

## Configure MySQL

The default configuration uses:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/url_shortener?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=password
```

Update `src/main/resources/application.properties` if your MySQL credentials are different.

## Run

```bash
mvn spring-boot:run
```

Open the dashboard at:

```text
http://localhost:8080
```

## REST API

Create a short URL:

```http
POST /api/urls
Content-Type: application/json

{
  "originalUrl": "https://example.com/some/long/path",
  "expiresAt": "2026-12-31T23:59:00"
}
```

List URLs:

```http
GET /api/urls
```

View analytics for one short code:

```http
GET /api/urls/{shortCode}/analytics
```

Dashboard totals:

```http
GET /api/dashboard
```

Redirect and track a click:

```http
GET /{shortCode}
```
