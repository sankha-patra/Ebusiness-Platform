# Razorpay Free Tier Setup Guide

## Getting Razorpay Test Credentials (Free)

1. **Sign up for Razorpay Test Account**
   - Go to https://razorpay.com/signup/
   - Sign up with your email address
   - No credit card required for test mode
   - Test mode is completely free

2. **Get Your Test API Keys**
   - Log in to Razorpay Dashboard
   - Navigate to Settings → API Keys
   - You'll see Test Mode keys (Key ID and Key Secret)
   - Copy these keys for your application

3. **Configure Application**
   - Copy `backend/.env.example` to `backend/.env`
   - Add your test credentials:
   ```env
   RAZORPAY_KEY_ID=rzp_test_XXXXXXXXXXXXX
   RAZORPAY_KEY_SECRET=YYYYYYYYYYYYYYYY
   RAZORPAY_WEBHOOK_SECRET=webhook_secret_placeholder
   ```

## Test Mode Features

### What's Available in Test Mode
- ✅ Create orders
- ✅ Process payments
- ✅ Test webhooks
- ✅ Refund transactions
- ✅ View all transaction history
- ✅ Use test payment methods

### Test Payment Methods
Razorpay provides several test payment methods:
- **Card**: Success, Failure, International cards
- **Netbanking**: Success, Failure scenarios
- **UPI**: Success, Failure scenarios
- **Wallet**: Paytm, PhonePe, etc.
- **EMI**: Various EMI options

### Test Card Numbers (Indian / domestic only)

Use **domestic** cards. International BINs often fail on INR checkouts.

| Network | Type | Number |
|---|---|---|
| Mastercard | Domestic success | `5267 3181 8797 5449` |
| Visa | Domestic success | `4111 1111 1111 1111` |
| RuPay | Domestic success | `6070 1000 2000 0004` |

- Expiry: any future date (e.g. `12/30`)
- CVV: any 3 digits (e.g. `123`)
- OTP: any 4–10 digits (e.g. `1234`) → Submit for success

**Do not use** international test cards for this app (INR):
- `5555 5555 5555 4444`
- `5105 1051 0510 5100`
- `5104 0600 0000 0008`

### Easier Indian alternatives
- **UPI**: `success@razorpay` (success) / `failure@razorpay` (fail)
- **Netbanking**: pick any bank → mock page → Success / Failure

## Webhook Testing in Test Mode

### Setting Up Webhooks
1. In Razorpay Dashboard, go to Settings → Webhooks
2. Add a new webhook
3. Use ngrok or similar tool for local testing:
   ```bash
   ngrok http 8081
   ```
4. Use the ngrok URL as your webhook endpoint:
   ```
   https://your-ngrok-url.ngrok-free.app/api/v1/payments/webhook
   ```

### Webhook Events
Common webhook events to test:
- `payment.captured` - Payment successful
- `payment.failed` - Payment failed
- `refund.processed` - Refund processed

## Integration Testing

### Test Payment Flow
1. Create a Razorpay order via API
2. Initialize payment on frontend
3. Complete payment using test card
4. Verify payment status via API
5. Check webhook reception

### Code Example
```java
// Create order
Map<String, String> order = paymentService.createRazorpayOrder(
    new BigDecimal("100.00"), 
    "order_receipt_001"
);

// Get payment status
PaymentStatusResponse status = paymentService.getPaymentStatus(
    order.get("razorpayOrderId")
);
```

## Free Tier Limitations

### Test Mode Limitations
- No real money transactions
- Test data cleared periodically
- Limited to test payment methods
- Webhooks work only with public URLs or tunneling

### Production Upgrade
When ready for production:
1. Complete KYC verification
2. Add bank account for settlements
3. Switch to live API keys
4. Update webhook endpoints
5. Implement proper security

## Best Practices

### Security
- Never commit API keys to version control
- Use environment variables for credentials
- Rotate test keys periodically
- Validate webhook signatures

### Testing
- Test success and failure scenarios
- Test webhook handling
- Verify cache eviction on payment updates
- Test circuit breaker fallback

### Monitoring
- Monitor payment success rates
- Track webhook delivery
- Log payment status changes
- Set up alerts for failures

## Troubleshooting

### Common Issues
1. **Invalid API Keys**: Verify you're using test keys, not live keys
2. **Webhook Not Received**: Check ngrok tunnel and firewall settings
3. **Payment Failed**: Use correct test card numbers
4. **Circuit Breaker Active**: Check if Razorpay service is available

### Debug Mode
Enable debug logging in `application.yml`:
```yaml
logging:
  level:
    com.razorpay: DEBUG
    com.ebusiness.platform.service.PaymentService: DEBUG
```

## Resources
- Razorpay Documentation: https://razorpay.com/docs/
- Test Mode Guide: https://razorpay.com/docs/payment-gateway/test-mode/
- API Reference: https://razorpay.com/docs/api/
