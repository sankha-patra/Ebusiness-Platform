# Free Tier Setup Guide

Complete guide to set up Redis, Kafka, and Razorpay in free tier configurations using your local PostgreSQL.

## Architecture Overview

```
┌─────────────────┐
│  PostgreSQL     │  ← Local Installation
│  (localhost:5432)│
└─────────────────┘
         ↓
┌─────────────────┐
│  Spring Boot    │
│  Application    │
└─────────────────┘
    ↓     ↓     ↓
┌──────┐ ┌──────┐ ┌─────────┐
│Redis │ │Kafka │ │Razorpay │
│Free │ │Free  │ │Free Test│
└──────┘ └──────┘ └─────────┘
```

## Free Tier Options

### 1. PostgreSQL (Local)
- **Your current setup**: Local PostgreSQL installation
- **Database**: `ebusiness`
- **User**: `ebusiness_user`
- **Cost**: Free (already installed)

### 2. Redis (Free Tier Options)

#### Option A: Local Redis Installation (Recommended)
```powershell
# Download Redis for Windows
# https://github.com/microsoftarchive/redis/releases

# Install and start
redis-server
```

#### Option B: Redis Cloud Free Tier
1. Sign up at https://redis.com/try-free/
2. Create a free Redis database
3. Get connection details
4. Set environment variables:
```powershell
set REDIS_HOST=your-redis-cloud-host
set REDIS_PORT=your-redis-cloud-port
set REDIS_PASSWORD=your-redis-cloud-password
```

#### Option C: Redis Labs Free Tier
1. Sign up at https://redislabs.com/try-free/
2. Create a free Redis Cloud database
3. Get connection string
4. Configure in application

### 3. Kafka (Free Tier Options)

#### Option A: Local Kafka Installation (Recommended)
```powershell
# Download Apache Kafka
# https://kafka.apache.org/downloads

# Extract and start Zookeeper
bin\windows\zookeeper-server-start.bat config\zookeeper.properties

# Start Kafka
bin\windows\kafka-server-start.bat config\server.properties
```

#### Option B: Confluent Cloud Free Tier
1. Sign up at https://www.confluent.io/confluent-cloud/try-free/
2. Create a free cluster
3. Get bootstrap servers
4. Set environment variable:
```powershell
set KAFKA_BOOTSTRAP_SERVERS=your-bootstrap-server
```

#### Option C: Upstash Kafka Free Tier
1. Sign up at https://upstash.com/
2. Create a free Kafka cluster
3. Get connection details
4. Configure in application

### 4. Razorpay (Free Test Mode)
1. Sign up at https://razorpay.com/signup/ (free)
2. Get test API keys from Dashboard
3. No credit card required
4. All payment features available in test mode

## Quick Setup (Recommended Path)

### Step 1: PostgreSQL Setup
```powershell
setup-postgres.bat
```

### Step 2: Redis Setup (Local)
```powershell
# Download Redis for Windows from GitHub releases
# Extract to C:\Redis
# Add to PATH: set PATH=%PATH%;C:\Redis
redis-server
```

### Step 3: Kafka Setup (Local)
```powershell
# Download Kafka from Apache website
# Extract to C:\kafka
# Start Zookeeper
C:\kafka\bin\windows\zookeeper-server-start.bat C:\kafka\config\zookeeper.properties

# Start Kafka (in new window)
C:\kafka\bin\windows\kafka-server-start.bat C:\kafka\config\server.properties
```

### Step 4: Razorpay Setup
1. Sign up at https://razorpay.com/signup/
2. Get test API keys
3. Create `.env` file:
```env
RAZORPAY_KEY_ID=rzp_test_your_key_id
RAZORPAY_KEY_SECRET=your_test_secret
RAZORPAY_WEBHOOK_SECRET=webhook_secret_placeholder
```

### Step 5: Start Application
```powershell
cd backend
mvn clean install
mvn spring-boot:run -Dspring-boot.run.profiles=freetier
```

## Cloud Free Tier Setup

### Redis Cloud Setup
```powershell
# 1. Sign up at https://redis.com/try-free/
# 2. Create free database (30MB)
# 3. Get connection details

# Set environment variables
set REDIS_HOST=redis-12345.c1.us-east-1.redislabs.com
set REDIS_PORT=12345
set REDIS_PASSWORD=your_password

# Start application
mvn spring-boot:run -Dspring-boot.run.profiles=freetier
```

### Confluent Cloud Setup
```powershell
# 1. Sign up at https://www.confluent.io/confluent-cloud/try-free/
# 2. Create free cluster (Basic tier, 3 partitions)
# 3. Create topics: payment-webhooks, order-status-updates, payment-confirmed
# 4. Get bootstrap servers

# Set environment variable
set KAFKA_BOOTSTRAP_SERVERS=pkc-xyz.us-east-1.aws.confluent.cloud:9092

# Start application
mvn spring-boot:run -Dspring-boot.run.profiles=freetier
```

## Configuration Profiles

### Free Tier Profile (`application-freetier.yml`)
- **PostgreSQL**: Local instance
- **Redis**: Local or cloud (configurable via env vars)
- **Kafka**: Local or cloud (configurable via env vars)
- **Razorpay**: Test mode
- **Optimizations**: Free tier resource limits

### Environment Variables
```powershell
# Redis Configuration
set REDIS_HOST=localhost
set REDIS_PORT=6379
set REDIS_PASSWORD=

# Kafka Configuration
set KAFKA_ENABLED=true
set KAFKA_BOOTSTRAP_SERVERS=localhost:9092

# Razorpay Configuration
set RAZORPAY_KEY_ID=rzp_test_demo_key_id
set RAZORPAY_KEY_SECRET=demo_key_secret
set RAZORPAY_WEBHOOK_SECRET=webhook_secret
```

## Testing

### 1. Start All Services
```powershell
# PostgreSQL (should be running)
# Redis
redis-server

# Kafka (requires two windows)
# Window 1: Zookeeper
C:\kafka\bin\windows\zookeeper-server-start.bat C:\kafka\config\zookeeper.properties

# Window 2: Kafka
C:\kafka\bin\windows\kafka-server-start.bat C:\kafka\config\server.properties
```

### 2. Start Application
```powershell
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=freetier
```

### 3. Run Tests
```powershell
cd backend
mvn test
```

### 4. Test Endpoints
```powershell
# Test caching (Redis)
curl -X GET "http://localhost:8081/api/v1/orders/order-001/status" -H "X-Tenant-ID: tenant-001"

# Test Kafka events
curl -X POST "http://localhost:8081/api/v1/load-test/mixed-traffic?perType=10"

# Test Razorpay
curl -X POST "http://localhost:8081/api/v1/payments/create-order?amount=1000&receipt=test-001"
```

## Free Tier Limitations

### Redis Free Tier
- **Local**: No limitations (recommended)
- **Redis Cloud**: 30MB storage, 25 connections
- **Redis Labs**: 30MB storage, limited operations

### Kafka Free Tier
- **Local**: No limitations (recommended)
- **Confluent Cloud**: 3 partitions, 5MB storage, limited throughput
- **Upstash**: 10K messages/day, 3 partitions

### Razorpay Test Mode
- No real money transactions
- Test data cleared periodically
- All payment features available
- No cost at all

## Troubleshooting

### Redis Connection Issues
```powershell
# Check if Redis is running
redis-cli ping

# Expected response: PONG

# If not running:
redis-server
```

### Kafka Connection Issues
```powershell
# Check if Kafka is running
# Check Zookeeper first
# Then check Kafka

# If Kafka fails to start:
# Check if port 9092 is available
# Check Java version (requires Java 8+)
```

### Razorpay Issues
```powershell
# Ensure using test mode keys
# Keys should start with "rzp_test_"
# Check Razorpay dashboard for key status
```

## Performance Expectations (Free Tier)

### Local Setup
- **PostgreSQL**: <50ms query time
- **Redis**: <5ms cache operations
- **Kafka**: <10ms message publishing
- **Overall API**: <100ms response time

### Cloud Free Tier
- **PostgreSQL**: <50ms query time
- **Redis Cloud**: <20ms cache operations
- **Confluent Cloud**: <50ms message publishing
- **Overall API**: <150ms response time

## Monitoring

### Redis Monitoring
```powershell
redis-cli
> INFO
> CLIENT LIST
> KEYS *
```

### Kafka Monitoring
```powershell
# List topics
C:\kafka\bin\windows\kafka-topics.bat --list --bootstrap-server localhost:9092

# Describe topic
C:\kafka\bin\windows\kafka-topics.bat --describe --topic payment-webhooks --bootstrap-server localhost:9092

# Consume messages
C:\kafka\bin\windows\kafka-console-consumer.bat --topic payment-webhooks --from-beginning --bootstrap-server localhost:9092
```

### Application Monitoring
Check application logs for:
- Cache hit/miss rates
- Kafka message publishing success
- Payment processing status
- Error messages

## Cost Summary

### Local Setup (Recommended)
- **PostgreSQL**: Free (already installed)
- **Redis**: Free (local installation)
- **Kafka**: Free (local installation)
- **Razorpay**: Free (test mode)
- **Total Cost**: $0/month

### Cloud Free Tier
- **PostgreSQL**: Free (local)
- **Redis Cloud**: Free (30MB limit)
- **Confluent Cloud**: Free (limited tier)
- **Razorpay**: Free (test mode)
- **Total Cost**: $0/month

## Next Steps

1. **Choose your setup path** (local vs cloud)
2. **Install required services** (Redis, Kafka)
3. **Configure environment variables**
4. **Start application with freetier profile**
5. **Run comprehensive tests**
6. **Monitor performance**
7. **Scale to paid tiers when needed**

## Support

- **Redis**: https://redis.io/docs/
- **Kafka**: https://kafka.apache.org/documentation/
- **Razorpay**: https://razorpay.com/docs/
- **Spring Boot**: https://spring.io/projects/spring-boot

This setup gives you a complete enterprise-grade platform with all free tier services, exactly as specified in your original requirements.
