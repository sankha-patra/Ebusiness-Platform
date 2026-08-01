# API Gateway

Public entry point on **port 8080**. Routes `/api/v1/**` to the monolith on **8081** today.

Later routes will target separate product / order / payment services without changing the frontend URL.

## Run

```bash
cd api-gateway
mvn spring-boot:run
```

Kafka UI is on **8088** so it no longer conflicts with the gateway.
