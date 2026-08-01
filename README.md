# EBusiness Platform

A full-stack enterprise B2B platform with Redis caching, Kafka event streaming, PostgreSQL database, and Razorpay payment integration.

## Architecture

- **Backend**: Spring Boot 3 with Java 17
- **Frontend**: Angular 18
- **Database**: PostgreSQL 15 (Free tier compatible)
- **Cache**: Redis 7 (Free tier - single instance)
- **Message Broker**: Apache Kafka (Free tier - single broker)
- **Payment Gateway**: Razorpay (Free test mode)

## Features

- Multi-tenant order management with Redis caching
- Product catalog with category-based caching
- Payment processing with Razorpay integration
- Event-driven architecture with Kafka
- Circuit breaker pattern for external API resilience
- Comprehensive test coverage with Testcontainers
- Load testing capabilities for Kafka traffic handling

## Prerequisites

- Docker and Docker Compose
- Java 17+
- Node.js 18+
- Maven 3.8+
- Angular CLI

## Quick Start

### 1. Start Infrastructure Services (Free Tier)

```bash
# On Windows
start-infrastructure.bat

# Or manually
docker-compose up -d
```

This will start free-tier optimized services:
- PostgreSQL on port 5432 (512MB memory limit)
- Redis on port 6379 (256MB memory limit, LRU eviction)
- Kafka on port 9092 (1GB memory limit, 1-hour retention)
- Kafka UI on port 8088 (256MB memory limit)
- API Gateway on port 8080 (routes to monolith :8081)

### 2. Configure Razorpay (Free Test Mode)

See [RAZORPAY_SETUP.md](RAZORPAY_SETUP.md) for detailed setup instructions.

Create a `.env` file in the backend directory:

```env
RAZORPAY_KEY_ID=rzp_test_your_key_id
RAZORPAY_KEY_SECRET=your_test_secret
RAZORPAY_WEBHOOK_SECRET=webhook_secret_placeholder
REDIS_HOST=localhost
REDIS_PORT=6379
```

### 3. Run End-to-End Tests

```bash
# On Windows
run-e2e-tests.bat

# Or manually
cd backend
mvn test -Dtest=EndToEndTest
```

### 4. Start Backend

```bash
cd backend
mvn clean install
mvn spring-boot:run
```

The backend will start on `http://localhost:8081`

### 5. Start Frontend

```bash
cd frontend
npm install
npm start
```

The frontend will start on `http://localhost:4200`

## Redis Caching Strategy

### Cache Configuration

The platform uses a cache-aside pattern with per-cache TTL:

| Cache Name | TTL | Purpose |
|------------|-----|---------|
| orderStatus | 2 minutes | Order status queries |
| paymentStatus | 1 minute | Payment status (PENDING not cached) |
| productsAll | 30 minutes | Full product list (`GET /products`) |
| productsByCategory | 30 minutes | Per-category list (`GET /products/category/{id}`) |
| tenantConfig | 1 hour | Tenant configuration |

### Multi-Tenant Cache Keys

Cache keys are namespaced (Redis prefix `ebusiness:`) so catalog lists never collide:

```
ebusiness:orderStatus::order:{tenantId}:{orderId}
ebusiness:paymentStatus::payment:{razorpayOrderId}
ebusiness:productsAll::all
ebusiness:productsByCategory::{categoryId}
```

Category responses are never reused for "all products" — they live in separate cache regions.
### Cache Eviction

- Cache is evicted on data mutations
- Kafka consumers explicitly evict cache after processing events
- TTL ensures eventual consistency

## Kafka Topics

| Topic | Purpose |
|-------|---------|
| payment-webhooks | Razorpay webhook events |
| order-status-updates | Order status change events |
| payment-confirmed | Payment confirmation events |

## API Endpoints

### Orders

- `GET /api/v1/orders/{orderId}/status` - Get order status (cached)
- `PUT /api/v1/orders/{orderId}/status` - Update order status (evicts cache)

### Products

- `GET /api/v1/products` - Get all products (cached)
- `GET /api/v1/products/category/{categoryId}` - Get products by category (cached)
- `POST /api/v1/products` - Create product (evicts cache)
- `PUT /api/v1/products/{productId}` - Update product (evicts cache)

### Payments

- `GET /api/v1/payments/{razorpayOrderId}/status` - Get payment status (cached)
- `POST /api/v1/payments/create-order` - Create Razorpay order
- `POST /api/v1/payments/webhook` - Handle Razorpay webhook

### Load Testing (For Kafka Traffic Testing)

- `GET /api/v1/load-test/kafka-stats` - Get load testing information
- `POST /api/v1/load-test/kafka/payment-webhooks?count=N` - Test payment webhook traffic
- `POST /api/v1/load-test/kafka/order-status?count=N` - Test order status traffic
- `POST /api/v1/load-test/kafka/payment-confirmed?count=N` - Test payment confirmed traffic
- `POST /api/v1/load-test/mixed-traffic?perType=N` - Test mixed event traffic

## Testing Kafka Traffic Handling

Since you don't have high traffic, you can simulate load using the built-in load testing endpoints:

### Small Load Test (10 messages per type)
```bash
curl -X POST "http://localhost:8081/api/v1/load-test/mixed-traffic?perType=10"
```

### Medium Load Test (50 messages per type)
```bash
curl -X POST "http://localhost:8081/api/v1/load-test/mixed-traffic?perType=50"
```

### High Load Test (100 messages per type)
```bash
curl -X POST "http://localhost:8081/api/v1/load-test/mixed-traffic?perType=100"
```

### Monitor in Kafka UI
1. Open http://localhost:8088
2. Navigate to Topics
3. View message rates and consumer lag
4. Monitor message throughput

## Testing

### Run All Tests

```bash
cd backend
mvn test
```

### Run Specific Test Class

```bash
mvn test -Dtest=OrderServiceCacheTest
```

### Test Coverage

The test suite includes:
- Redis caching behavior (miss, hit, eviction, multi-tenant isolation)
- Kafka message publishing and consumption
- Payment service with circuit breaker fallback
- Product catalog caching
- Concurrent request handling

## Monitoring

### Kafka UI

Access Kafka UI at `http://localhost:8088` to monitor:
- Topics and messages
- Consumer groups
- Broker status

### Redis CLI

```bash
docker exec -it ebusiness-redis redis-cli
```

### PostgreSQL Access

```bash
docker exec -it ebusiness-postgres psql -U ebusiness_user -d ebusiness
```

## Configuration

### Backend Configuration

Edit `backend/src/main/resources/application.yml` to configure:
- Database connection
- Redis connection
- Kafka bootstrap servers
- Razorpay credentials
- Cache TTL values
- Circuit breaker settings

### Frontend Configuration

Edit `frontend/src/environments/environment.ts` to configure:
- API base URL
- CORS settings

## Deployment

### Production Considerations

1. **Security**
   - Use environment variables for secrets
   - Enable HTTPS
   - Configure CORS properly
   - Implement authentication/authorization

2. **Scaling**
   - Use external Redis (AWS ElastiCache)
   - Use managed Kafka (AWS MSK/Confluent)
   - Use managed PostgreSQL (AWS RDS)
   - Configure connection pooling

3. **Monitoring**
   - Add application monitoring (Prometheus/Grafana)
   - Set up log aggregation (ELK)
   - Configure health checks
   - Monitor cache hit rates

4. **Performance**
   - Tune Redis memory and eviction policies
   - Configure Kafka partitioning for parallelism
   - Optimize database queries and indexes
   - Enable CDN for static assets

## Troubleshooting

### Backend Won't Start

1. Check if Docker services are running: `docker-compose ps`
2. Verify database connection in `application.yml`
3. Check if ports 8081, 5432, 6379, 9092 are available

### Cache Not Working

1. Verify Redis is running: `docker exec -it ebusiness-redis redis-cli ping`
2. Check Redis configuration in `application.yml`
3. Review cache logs for connection errors

### Kafka Issues

1. Check Kafka UI at `http://localhost:8088`
2. Verify Kafka is running: `docker-compose logs kafka`
3. Check topic creation in Kafka UI

### Payment Integration

1. Verify Razorpay credentials in environment variables
2. Ensure you're using test mode for development
3. Check Razorpay dashboard for API key status

## License

MIT

## Support

For issues and questions, please refer to the project documentation or contact the development team.
