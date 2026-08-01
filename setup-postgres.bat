@echo off
echo Setting up PostgreSQL for EBusiness Platform...
echo.

REM Check if PostgreSQL is installed
where psql >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: PostgreSQL command line tools (psql) not found in PATH.
    echo Please add PostgreSQL bin directory to your PATH or use full path to psql.exe
    echo.
    echo Common PostgreSQL installation paths:
    echo - C:\Program Files\PostgreSQL\15\bin
    echo - C:\Program Files\PostgreSQL\14\bin
    echo - C:\PostgreSQL\15\bin
    echo.
    echo Example: set PATH=%PATH%;C:\Program Files\PostgreSQL\15\bin
    pause
    exit /b 1
)

echo PostgreSQL found in PATH.
echo.

REM Default PostgreSQL connection settings
set PGHOST=localhost
set PGPORT=5432
set PGUSER=postgres

echo Please enter your PostgreSQL password for user 'postgres':
set /p PGPASSWORD="Password: "

echo.
echo Creating database and user...
echo.

REM Create database
psql -h %PGHOST% -p %PGPORT% -U %PGUSER% -c "CREATE DATABASE ebusiness;" 2>nul
if %errorlevel% neq 0 (
    echo Note: Database may already exist or creation failed (continuing anyway)
)

REM Create user
psql -h %PGHOST% -p %PGPORT% -U %PGUSER% -d ebusiness -c "DO $$ BEGIN IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'ebusiness_user') THEN CREATE USER ebusiness_user WITH PASSWORD 'ebusiness_pass'; END IF; END $$;" 2>nul

REM Grant privileges
psql -h %PGHOST% -p %PGPORT% -U %PGUSER% -d ebusiness -c "GRANT ALL PRIVILEGES ON DATABASE ebusiness TO ebusiness_user;" 2>nul
psql -h %PGHOST% -p %PGPORT% -U %PGUSER% -d ebusiness -c "GRANT ALL ON SCHEMA public TO ebusiness_user;" 2>nul
psql -h %PGHOST% -p %PGPORT% -U %PGUSER% -d ebusiness -c "GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO ebusiness_user;" 2>nul
psql -h %PGHOST% -p %PGPORT% -U %PGUSER% -d ebusiness -c "GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO ebusiness_user;" 2>nul

echo.
echo ========================================
echo PostgreSQL Setup Complete!
echo ========================================
echo.
echo Database: ebusiness
echo User: ebusiness_user
echo Password: ebusiness_pass
echo Host: localhost
echo Port: 5432
echo.
echo You can now start the application with:
echo cd backend
echo mvn spring-boot:run -Dspring-boot.run.profiles=local
echo.
pause
