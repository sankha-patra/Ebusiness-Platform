export const environment = {
  production: false,
  // Traffic goes through API Gateway (8080). Monolith remains on 8081 behind it.
  apiUrl: 'http://localhost:8080/api/v1',
  razorpayKeyId: 'rzp_test_TKP6V8tEkHHnxe'
};
