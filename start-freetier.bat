@echo off
echo Starting EBusiness Platform (Free Tier Configuration)
echo =====================================================
echo.

REM Check Java
echo Checking Java installation...
java -version >nul 2>&1
if errorlevel 1 (
    echo ERROR: Java is not installed or not in PATH
    echo Please install Java 17 or higher
    pause
    exit /b 1
)
echo Java: OK
echo.

REM Check Maven
echo Checking Maven installation...
mvn -version >nul 2>&1
if errorlevel 1 (
    echo ERROR: Maven is not installed or not in PATH
    echo Please install Maven 3.8 or higher
    pause
    exit /b 1
)
echo Maven: OK
echo.

REM Check Node.js
echo Checking Node.js installation...
node --version >nul 2>&1
if errorlevel 1 (
    echo ERROR: Node.js is not installed or not in PATH
    echo Please install Node.js 18 or higher
    pause
    exit /b 1
)
echo Node.js: OK
echo.

REM Check PostgreSQL
echo Checking PostgreSQL installation...
where psql >nul 2>&1
if errorlevel 1 (
    echo WARNING: PostgreSQL command line tools not found in PATH
    echo Please add PostgreSQL bin directory to PATH
    echo Common paths: C:\Program Files\PostgreSQL\15\bin
    echo.
    echo Attempting to continue anyway...
    echo.
) else (
    echo PostgreSQL: OK
    echo.
)

REM Setup PostgreSQL
echo Setting up PostgreSQL database...
call setup-postgres.bat
if errorlevel 1 (
    echo WARNING: PostgreSQL setup had issues, but continuing...
    echo The application may fail if database is not accessible
    echo.
)

REM Check Redis
echo Checking Redis installation...
where redis-cli >nul 2>&1
if errorlevel 1 (
    echo WARNING: Redis not found in PATH
    echo Redis will be started in embedded mode if available
    echo Otherwise, caching will be disabled gracefully
    echo.
    echo To enable Redis caching:
    echo 1. Install Redis for Windows from GitHub releases
    echo 2. Add Redis to PATH
    echo 3. Start redis-server before starting this application
    echo.
) else (
    echo Redis: OK
    echo Checking if Redis is running...
    redis-cli ping >nul 2>&1
    if errorlevel 1 (
        echo Redis is not running. Starting embedded Redis...
    ) else (
        echo Redis is running: OK
    )
    echo.
)

REM Check Kafka
echo Checking Kafka installation...
if exist "C:\kafka\bin\windows\kafka-server-start.bat" (
    echo Kafka: Found at C:\kafka
    echo Checking if Kafka is running...
    powershell -Command "try { $response = Invoke-WebRequest -Uri 'http://localhost:9092' -TimeoutSec 2; exit 0 } catch { exit 1 }" >nul 2>&1
    if errorlevel 1 (
        echo Kafka is not running. Please start Kafka manually:
        echo 1. Start Zookeeper: C:\kafka\bin\windows\zookeeper-server-start.bat C:\kafka\config\zookeeper.properties
        echo 2. Start Kafka: C:\kafka\bin\windows\kafka-server-start.bat C:\kafka\config\server.properties
        echo.
        echo Or use Kafka Cloud free tier and set KAFKA_BOOTSTRAP_SERVERS environment variable
        echo.
        set /p CONTINUE="Continue without Kafka? (Y/N): "
        if /i not "%CONTINUE%"=="Y" (
            echo Please start Kafka and run this script again
            pause
            exit /b 1
        )
        set KAFKA_ENABLED=false
    ) else (
        echo Kafka is running: OK
        set KAFKA_ENABLED=true
    )
) else (
    echo WARNING: Kafka not found
    echo Kafka events will be logged but not published
    echo.
    echo To enable Kafka:
    echo 1. Install Apache Kafka
    echo 2. Extract to C:\kafka
    echo 3. Start Zookeeper and Kafka servers
    echo.
    echo Or use Kafka Cloud free tier:
    echo 1. Sign up at Confluent Cloud or Upstash
    echo 2. Set KAFKA_BOOTSTRAP_SERVERS environment variable
    echo.
    set /p CONTINUE="Continue without Kafka? (Y/N): "
    if /i not "%CONTINUE%"=="Y" (
        echo Please install Kafka and run this script again
        pause
        exit /b 1
    )
    set KAFKA_ENABLED=false
)
echo.

REM Start Backend
echo =====================================================
echo Starting Backend (Spring Boot - Free Tier Profile)
echo =====================================================
echo.
cd backend
echo Building application...
call mvn clean install -DskipTests
if errorlevel 1 (
    echo ERROR: Maven build failed
    pause
    exit /b 1
)
echo.
echo Starting backend server on http://localhost:8081
echo Profile: freetier
echo Press Ctrl+C to stop the server
echo.
start "EBusiness Backend" cmd /k "mvn spring-boot:run -Dspring-boot.run.profiles=freetier"

REM Wait for backend to start
echo Waiting for backend to start (30 seconds)...
timeout /t 30 /nobreak

REM Start Frontend
echo =====================================================
echo Starting Frontend (Angular)
echo =====================================================
echo.
cd ..\frontend
echo Installing dependencies...
call npm install
if errorlevel 1 (
    echo ERROR: npm install failed
    pause
    exit /b 1
)
echo.
echo Starting frontend server on http://localhost:4200
echo Press Ctrl+C to stop the server
echo.
start "EBusiness Frontend" cmd /k "npm start"

echo.
echo =====================================================
echo Application Started Successfully!
echo =====================================================
echo.
echo Configuration:
echo - PostgreSQL: Local (localhost:5432)
echo - Redis: %REDIS_HOST% (localhost if not set)
echo - Kafka: %KAFKA_ENABLED% (logging if disabled)
echo - Razorpay: Test Mode
echo.
echo Access Points:
echo - Backend: http://localhost:8081
echo - Frontend: http://localhost:4200
echo.
echo To test the application:
echo 1. Open http://localhost:4200 in your browser
echo 2. Test order status lookup (uses Redis caching)
echo 3. Test product catalog (uses Redis caching)
echo 4. Test payment order creation (uses Razorpay test mode)
echo 5. Test load testing: POST http://localhost:8081/api/v1/load-test/mixed-traffic?perType=10
echo.
echo To run tests:
echo cd backend
echo mvn test
echo.
echo To stop the application:
echo Close the backend and frontend command windows
echo.
echo For detailed setup instructions, see FREETIER_SETUP.md
echo.
pause
