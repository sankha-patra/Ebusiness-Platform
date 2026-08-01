# Payment Service

Razorpay payments + transactional outbox, extracted from the monolith.

- **Port:** `8084`
- **DB:** same Postgres for now (`payments`, `orders` FK write on create, `outbox_messages`, `processed_webhooks`)
- **Outbox:** sole publisher for `payment-confirmed` / `order-status-updates` from capture path
- **Gateway:** `/api/v1/payments/**` → `http://localhost:8084`
- **Consumers:** notifications still on monolith (`:8081`) listening to Kafka

```bash
cd payment-service
# set RAZORPAY_* env vars (same as backend)
mvn spring-boot:run
```

Thin events only — no `common-dto` JAR. Order table writes here are temporary until `order-service` extract.
