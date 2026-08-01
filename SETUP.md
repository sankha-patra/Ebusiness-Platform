# Setup Guide

## Step-by-Step Installation

### 1. Clone the Project

```bash
cd C:\Users\sankh\ebusiness-platform
```

### 2. Start Docker Services

```bash
docker-compose up -d
```

Verify services are running:
```bash
docker-compose ps
```

### 3. Configure Razorpay

1. Sign up for a Razorpay account (free tier)
2. Get your API keys from the Razorpay dashboard
3. Copy `backend/.env.example` to `backend/.env`
4. Update the file with your credentials:

```env
RAZORPAY_KEY_ID=rzp_test_your_key_id
RAZORPAY_KEY_SECRET=your_secret_key
RAZORPAY_WEBHOOK_SECRET=your_webhook_secret
```

### 4. Build and Run Backend

```bash
cd backend
mvn clean install
mvn spring-boot:run
```

The backend will start on `http://localhost:8081`

### 5. Build and Run Frontend

```bash
cd frontend
npm install
npm start
```

The frontend will start on `http://localhost:4200`

### 6. Verify Setup

1. Access the frontend: `http://localhost:4200`
2. Navigate to different sections (Orders, Products, Payments)
3. Check Kafka UI: `http://localhost:8080`
4. Test API endpoints using the frontend or tools like Postman

## Testing the Application

### Test Redis Caching

1. Make a request to get order status
2. Make the same request again - it should be faster (cache hit)
3. Update the order status
4. Make the request again - it should fetch from database (cache eviction)

### Test Kafka Events

1. Create a Razorpay order
2. Check Kafka UI for the payment-confirmed topic
3. Update order status
4. Check Kafka UI for the order-status-updates topic

### Test Product Catalog

1. Get products by category
2. Make the same request again - should be cached
3. Update a product
4. Get products again - should fetch fresh data

## Troubleshooting

### Port Conflicts

If ports are already in use, modify `docker-compose.yml`:

```yaml
ports:
  - "5433:5432"  # Change PostgreSQL port
  - "6380:6379"  # Change Redis port
  - "9093:9092"  # Change Kafka port
```

Update corresponding configuration in `application.yml`.

### Database Connection Issues

1. Verify PostgreSQL is running: `docker-compose logs postgres`
2. Check connection settings in `application.yml`
3. Ensure database exists: `docker exec -it ebusiness-postgres psql -U ebusiness_user -d ebusiness`

### Redis Connection Issues

1. Verify Redis is running: `docker exec -it ebusiness-redis redis-cli ping`
2. Check connection settings in `application.yml`
3. Test Redis CLI: `docker exec -it ebusiness-redis redis-cli`

### Kafka Issues

1. Check Kafka logs: `docker-compose logs kafka`
2. Verify topics are created in Kafka UI
3. Check consumer group status in Kafka UI

## Development Tips

### Running Tests

```bash
cd backend
mvn test
```

### Code Quality

```bash
mvn checkstyle:check
mvn spotbugs:check
```

### Building for Production

```bash
cd backend
mvn clean package -Pprod

cd frontend
npm run build
```

## Next Steps

1. Implement authentication and authorization
2. Add comprehensive logging
3. Set up monitoring (Prometheus/Grafana)
4. Configure CI/CD pipeline
5. Add API documentation (Swagger/OpenAPI)
6. Implement rate limiting
7. Add input validation
8. Set up backup strategies
