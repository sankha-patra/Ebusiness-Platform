@echo off
echo Starting EBusiness Platform (Local Testing Mode)
echo ============================================
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

REM Start Backend
echo ============================================
echo Starting Backend (Spring Boot)
echo ============================================
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
echo Press Ctrl+C to stop the server
echo.
start "EBusiness Backend" cmd /k "mvn spring-boot:run -Dspring-boot.run.profiles=local"

REM Wait for backend to start
echo Waiting for backend to start (30 seconds)...
timeout /t 30 /nobreak

REM Start Frontend
echo ============================================
echo Starting Frontend (Angular)
echo ============================================
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
echo ============================================
echo Application Started Successfully!
echo ============================================
echo.
echo Backend: http://localhost:8081
echo Frontend: http://localhost:4200
echo.
echo To test the application:
echo 1. Open http://localhost:4200 in your browser
echo 2. Test order status lookup
echo 3. Test product catalog
echo 4. Test payment order creation
echo.
echo To run tests:
echo cd backend
echo mvn test
echo.
echo To stop the application:
echo Close the backend and frontend command windows
echo.
pause
