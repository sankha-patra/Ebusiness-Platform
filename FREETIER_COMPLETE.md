# Complete Free Tier Setup - Redis, Kafka, Razorpay with Local PostgreSQL

This is the complete setup for using Redis, Kafka, and Razorpay in free tier configurations with your local PostgreSQL, exactly as specified in your original requirements.

## 🎯 Architecture

```
┌─────────────────────────────────────────────────┐
│          EBusiness Platform                    │
│          (Spring Boot + Angular)                │
└─────────────────────────────────────────────────┘
                    ↓
    ┌───────────────┼───────────────┐
    ↓               ↓               ↓
┌─────────┐    ┌─────────┐    ┌──────────┐
│PostgreSQL│   │  Redis  │   │  Kafka   │
│  (Local) │   │ (Free)  │   │  (Free)  │
└─────────┘    └─────────┘    └──────────┘
                                   ↓
                            ┌──────────┐
                            │ Razorpay │
                            │ (Free)   │
                            └──────────┘
```

## 📋 Free Tier Configurations

### 1. PostgreSQL (Local) ✅
- **Status**: Already installed on your system
- **Database**: `ebusiness`
- **User**: `ebusiness_user`
- **Password**: `ebusiness_pass`
- **Cost**: FREE (no additional cost)
- **Setup**: Run `setup-postgres.bat`

### 2. Redis (Free Tier) ✅
**Option A: Local Redis (Recommended)**
- Download: https://github.com/microsoftarchive/redis/releases
- Install to: `C:\Redis`
- Add to PATH: `set PATH=%PATH%;C:\Redis`
- Start: `redis-server`
- **Cost**: FREE (local installation)

**Option B: Redis Cloud Free**
- Sign up: https://redis.com/try-free/
- Free tier: 30MB storage, 25 connections
- Configure: Set `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD`
- **Cost**: FREE (cloud free tier)

### 3. Kafka (Free Tier) ✅
**Option A: Local Kafka (Recommended)**
- Download: https://kafka.apache.org/downloads
- Install to: `C:\kafka`
- Start Zookeeper: `C:\kafka\bin\windows\zookeeper-server-start.bat C:\kafka\config\zookeeper.properties`
- Start Kafka: `C:\kafka\bin\windows\kafka-server-start.bat C:\kafka\config\server.properties`
- **Cost**: FREE (local installation)

**Option B: Confluent Cloud Free**
- Sign up: https://www.confluent.io/confluent-cloud/try-free/
- Free tier: 3 partitions, 5MB storage
- Configure: Set `KAFKA_BOOTSTRAP_SERVERS`
- **Cost**: FREE (cloud free tier)

### 4. Razorpay (Free Test Mode) ✅
- Sign up: https://razorpay.com/signup/
- Get test keys from Dashboard
- Test mode: All features available
- No credit card required
- **Cost**: FREE (test mode)

## 🚀 Quick Start

### Automated Setup
```powershell
start-freetier.bat
```

This script will:
1. Check all prerequisites (Java, Maven, Node.js, PostgreSQL)
2. Setup PostgreSQL database
3. Check Redis availability (start embedded if not available)
4. Check Kafka availability (log events if not available)
5. Start backend with freetier profile
6. Start frontend

### Manual Setup

#### 1. Setup PostgreSQL
```powershell
setup-postgres.bat
```

#### 2. Setup Redis (Local)
```powershell
# Download and install Redis
# Add to PATH
redis-server
```

#### 3. Setup Kafka (Local)
```powershell
# Start Zookeeper (Window 1)
C:\kafka\bin\windows\zookeeper-server-start.bat C:\kafka\config\zookeeper.properties

# Start Kafka (Window 2)
C:\kafka\bin\windows\kafka-server-start.bat C:\kafka\config\server.properties
```

#### 4. Configure Razorpay
```powershell
# Sign up at https://razorpay.com/signup/
# Get test API keys
# Create backend/.env file with:
RAZORPAY_KEY_ID=rzp_test_your_key_id
RAZORPAY_KEY_SECRET=your_test_secret
```

#### 5. Start Application
```powershell
cd backend
mvn clean install
mvn spring-boot:run -Dspring-boot.run.profiles=freetier
```

#### 6. Start Frontend
```powershell
cd frontend
npm install
npm start
```

## 🔧 Configuration Files

### application-freetier.yml
Free tier optimized configuration:
- PostgreSQL: Local connection
- Redis: Configurable (local/cloud)
- Kafka: Configurable (local/cloud)
- Razorpay: Test mode
- Optimizations: Free tier resource limits

### FreeTierRedisConfig.java
- Tries external Redis first
- Falls back to embedded Redis
- Graceful degradation if Redis unavailable
- CacheErrorHandler ensures fail-open behavior

### FreeTierKafkaConfig.java
- Configurable bootstrap servers
- Free tier optimizations
- Reduced resource usage
- Compression for efficiency

### KafkaProducerService.java
- Conditional Kafka publishing
- Success/failure callbacks
- Event logging when Kafka unavailable
- Load testing support

## 🧪 Testing

### Run All Tests
```powershell
cd backend
mvn test
```

### Manual API Testing
```powershell
# Test Redis caching
curl -X GET "http://localhost:8081/api/v1/orders/order-001/status" -H "X-Tenant-ID: tenant-001"

# Test Kafka events
curl -X POST "http://localhost:8081/api/v1/load-test/mixed-traffic?perType=10"

# Test Razorpay
curl -X POST "http://localhost:8081/api/v1/payments/create-order?amount=1000&receipt=test-001"
```

### End-to-End Testing
```powershell
cd backend
mvn test -Dtest=EndToEndTest
```

## 📊 Features with Free Tier

### ✅ Redis Caching (Free)
- Multi-tenant cache keys
- Per-cache TTL configuration
- Cache eviction on mutations
- Fail-open behavior
- Hit/miss logging

### ✅ Kafka Event Streaming (Free)
- Three event topics
- Event serialization
- Success/failure callbacks
- Load testing support
- Graceful degradation

### ✅ Razorpay Payments (Free)
- Test mode all features
- Payment order creation
- Status checking
- Webhook handling
- Circuit breaker fallback

### ✅ PostgreSQL Database (Local)
- Full CRUD operations
- Entity relationships
- Transaction management
- Data persistence

## 🎛️ Environment Variables

```powershell
# Redis Configuration
set REDIS_HOST=localhost
set REDIS_PORT=6379
set REDIS_PASSWORD=

# Kafka Configuration
set KAFKA_ENABLED=true
set KAFKA_BOOTSTRAP_SERVERS=localhost:9092

# Razorpay Configuration
set RAZORPAY_KEY_ID=rzp_test_your_key_id
set RAZORPAY_KEY_SECRET=your_test_secret
set RAZORPAY_WEBHOOK_SECRET=webhook_secret
```

## 📈 Performance (Free Tier)

### Expected Performance
- **Database queries**: <50ms
- **Cache operations**: <5ms (if Redis available)
- **Kafka publishing**: <10ms (if Kafka available)
- **API response time**: <100ms (with caching)
- **Load testing**: 100+ events/second

### Resource Usage
- **PostgreSQL**: <200MB memory
- **Redis**: <100MB memory (if local)
- **Kafka**: <400MB memory (if local)
- **Backend**: <512MB memory
- **Frontend**: <200MB memory

## 🔍 Monitoring

### Redis Monitoring
```powershell
redis-cli
> INFO
> KEYS *
> GET order:tenant-001:order-001
```

### Kafka Monitoring
```powershell
# List topics
C:\kafka\bin\windows\kafka-topics.bat --list --bootstrap-server localhost:9092

# Monitor messages
C:\kafka\bin\windows\kafka-console-consumer.bat --topic payment-webhooks --from-beginning --bootstrap-server localhost:9092
```

### Application Monitoring
- Check application logs
- Monitor cache hit/miss rates
- Verify Kafka event publishing
- Check payment processing status

## 🆚 Comparison: Local vs Cloud Free Tier

| Service | Local | Cloud Free | Recommended |
|---------|-------|------------|-------------|
| PostgreSQL | Local only | N/A | Local ✅ |
| Redis | Full features | 30MB limit | Local ✅ |
| Kafka | Full features | Limited partitions | Local ✅ |
| Razorpay | Test mode | Test mode | Same ✅ |

## 💰 Cost Summary

### Local Setup (Recommended)
- PostgreSQL: FREE (local)
- Redis: FREE (local installation)
- Kafka: FREE (local installation)
- Razorpay: FREE (test mode)
- **Total: $0/month**

### Cloud Free Tier
- PostgreSQL: FREE (local)
- Redis Cloud: FREE (30MB limit)
- Confluent Cloud: FREE (limited tier)
- Razorpay: FREE (test mode)
- **Total: $0/month**

## 📚 Documentation

- **FREETIER_SETUP.md** - Detailed free tier setup guide
- **RAZORPAY_SETUP.md** - Razorpay test mode configuration
- **application-freetier.yml** - Free tier configuration
- **.env.freetier** - Environment variables template
- **start-freetier.bat** - Automated startup script

## 🎯 Original Requirements Met

✅ **PostgreSQL**: Local installation (as specified)
✅ **Redis**: Free tier configuration (local or cloud)
✅ **Kafka**: Free tier configuration (local or cloud)
✅ **Razorpay**: Free test mode (as specified)
✅ **Same architecture**: As in original PDF/MD documents
✅ **All features**: Caching, event streaming, payments

## 🚦 Status

All components are configured for free tier usage:
- ✅ PostgreSQL: Local (already available)
- ✅ Redis: Configured for local/cloud free tier
- ✅ Kafka: Configured for local/cloud free tier
- ✅ Razorpay: Configured for free test mode
- ✅ Caching: Multi-tenant with TTL
- ✅ Events: Kafka topics configured
- ✅ Payments: Razorpay integration

## 🎉 Ready to Use

The system is now configured exactly as you requested:
- **PostgreSQL**: Local installation
- **Redis**: Free tier (local/cloud options)
- **Kafka**: Free tier (local/cloud options)
- **Razorpay**: Free test mode

Run `start-freetier.bat` to start the complete system with all free tier services!