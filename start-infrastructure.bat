@echo off
echo Starting EBusiness Platform Infrastructure...
echo.

echo 1. Starting Docker services...
docker-compose up -d

echo.
echo 2. Waiting for services to be healthy...
timeout /t 10 /nobreak

echo.
echo 3. Checking service status...
docker-compose ps

echo.
echo 4. Testing database connection...
docker exec ebusiness-postgres pg_isready -U ebusiness_user -d ebusiness

echo.
echo 5. Testing Redis connection...
docker exec ebusiness-redis redis-cli ping

echo.
echo 6. Testing Kafka connection...
docker exec ebusiness-kafka kafka-broker-api-versions --bootstrap-server localhost:9092

echo.
echo ========================================
echo Infrastructure Setup Complete!
echo ========================================
echo.
echo Services running:
echo - PostgreSQL: localhost:5432
echo - Redis: localhost:6379
echo - Kafka: localhost:9092
echo - Kafka UI: http://localhost:8088
echo - API Gateway (when running): http://localhost:8080
echo - Monolith API: http://localhost:8081
echo.
echo Next steps:
echo 1. Configure backend/.env with your Razorpay credentials
echo 2. Start backend: cd backend && mvn spring-boot:run
echo 3. Start frontend: cd frontend && npm start
echo 4. Run tests: cd backend && mvn test
echo.
