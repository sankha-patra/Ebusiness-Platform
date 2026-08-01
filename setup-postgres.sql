-- PostgreSQL Setup Script for EBusiness Platform
-- Run this to create the database and user

-- Create database
CREATE DATABASE IF NOT EXISTS ebusiness;

-- Connect to the database
\c ebusiness

-- Create user if not exists
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'ebusiness_user') THEN
        CREATE USER ebusiness_user WITH PASSWORD 'ebusiness_pass';
    END IF;
END
$$;

-- Grant privileges
GRANT ALL PRIVILEGES ON DATABASE ebusiness TO ebusiness_user;

-- Connect to ebusiness database
\c ebusiness

-- Grant schema privileges
GRANT ALL ON SCHEMA public TO ebusiness_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO ebusiness_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO ebusiness_user;

-- Set default privileges
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO ebusiness_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO ebusiness_user;

-- Display success message
SELECT 'PostgreSQL setup completed successfully!' AS status;
