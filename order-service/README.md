# Order Service

Order HTTP API + `payment-confirmed` consumer.

- **Port:** `8082`
- **DB:** shared Postgres (`orders` / `order_items` / read `payments` for status)
- **Kafka group:** `order-service` (notifications stay on monolith `ebusiness-group`)
- **Gateway:** `/api/v1/orders/**` → `http://localhost:8082`

```bash
cd order-service
mvn spring-boot:run
```

Note: `payment-service` still creates the Order row on Razorpay create-order (shared DB) until a proper Order→Payment saga. Product joins on line items are read-only — prefer snapshots next.
