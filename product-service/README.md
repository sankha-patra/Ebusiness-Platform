# Product Service

Catalog microservice extracted from the monolith.

- **Port:** `8083`
- **DB:** same Postgres (`ebusiness`) — owns `products` / `categories` tables for reads/writes via HTTP
- **Cache:** Redis (`product-service:` prefix), fail-open if Redis down
- **Gateway:** `/api/v1/products/**` → `http://localhost:8083`

```bash
cd product-service
mvn spring-boot:run
```

No shared `common-dto` JAR — API contract is the JSON shape of `ProductResponse` only.
