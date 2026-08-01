@echo off
echo Running EBusiness Platform End-to-End Tests...
echo.

echo 1. Checking if infrastructure is running...
docker-compose ps >nul 2>&1
if errorlevel 1 (
    echo ERROR: Docker services are not running. Please run start-infrastructure.bat first.
    exit /b 1
)

echo Infrastructure is running.
echo.

echo 2. Running backend tests...
cd backend
call mvn clean test -Dtest=EndToEndTest

if errorlevel 1 (
    echo ERROR: Tests failed. Check the logs above.
    exit /b 1
)

echo.
echo ========================================
echo All End-to-End Tests Passed!
echo ========================================
echo.
echo Test coverage:
echo ✅ Complete order flow
echo ✅ Multi-tenant isolation
echo ✅ Cache performance
echo ✅ Product catalog caching
echo ✅ Resilience patterns
echo.
echo You can now:
echo 1. Start the application: mvn spring-boot:run
echo 2. Test Kafka load testing: POST http://localhost:8081/api/v1/load-test/kafka-stats
echo 3. Access the frontend: http://localhost:4200
echo.
