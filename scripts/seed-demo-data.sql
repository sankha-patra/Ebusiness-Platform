-- Demo seed for local Docker. Safe to re-run (ON CONFLICT / WHERE NOT EXISTS).

INSERT INTO tenants (tenant_id, name, email, status, created_at, updated_at)
SELECT 'tenant-default', 'Default Tenant', 'default@ebusiness.local', 'ACTIVE', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM tenants WHERE tenant_id = 'tenant-default');

INSERT INTO categories (category_id, name, description, status, created_at, updated_at)
SELECT v.category_id, v.name, v.description, 'ACTIVE', NOW(), NOW()
FROM (VALUES
  ('electronics', 'Electronics', 'Phones, laptops, accessories'),
  ('clothing', 'Clothing', 'Apparel and wearables'),
  ('books', 'Books', 'Fiction and non-fiction'),
  ('home', 'Home & Garden', 'Home essentials')
) AS v(category_id, name, description)
WHERE NOT EXISTS (SELECT 1 FROM categories c WHERE c.category_id = v.category_id);

-- Replace the thin smoke-test row if present
DELETE FROM order_items WHERE product_id IN (SELECT id FROM products WHERE product_id = 'prod-demo-1');
DELETE FROM products WHERE product_id = 'prod-demo-1';

INSERT INTO products (product_id, name, description, price, currency, stock_quantity, status, category_id, created_at, updated_at)
SELECT v.product_id, v.name, v.description, v.price, 'INR', v.stock, 'ACTIVE', c.id, NOW(), NOW()
FROM (VALUES
  ('prod-laptop-14',  'Ultrabook 14"',     'Lightweight laptop for daily work',           54999.00, 12),
  ('prod-phone-pro',  'Phone Pro 128GB',   'Flagship phone with dual SIM',                39999.00, 25),
  ('prod-earbuds',    'Wireless Earbuds',  'ANC earbuds with 24h case',                    4999.00, 80),
  ('prod-tee-black',  'Classic Tee',       'Cotton crew-neck tee',                          799.00, 100),
  ('prod-novel',      'System Design 101', 'Practical guide to distributed systems',       1299.00, 40),
  ('prod-desk-lamp',  'LED Desk Lamp',     'Adjustable warmth desk lamp',                  1899.00, 35)
) AS v(product_id, name, description, price, stock)
JOIN categories c ON c.category_id = CASE
  WHEN v.product_id LIKE 'prod-laptop%' OR v.product_id LIKE 'prod-phone%' OR v.product_id LIKE 'prod-ear%' THEN 'electronics'
  WHEN v.product_id LIKE 'prod-tee%' THEN 'clothing'
  WHEN v.product_id LIKE 'prod-novel%' THEN 'books'
  ELSE 'home'
END
WHERE NOT EXISTS (SELECT 1 FROM products p WHERE p.product_id = v.product_id);

-- Sample orders so the Orders page is not empty
INSERT INTO orders (order_id, tenant_id, status, total_amount, currency, razorpay_order_id, notes, created_at, updated_at)
SELECT v.order_id, t.id, v.status, v.amount, 'INR', v.rzp, v.notes, NOW() - (v.hours || ' hours')::interval, NOW()
FROM (VALUES
  ('ord-demo-1001', 'PAID',            54999.00, 'order_demo_rzp_1001', 'Ultrabook purchase', 2),
  ('ord-demo-1002', 'PAYMENT_PENDING',  4999.00, 'order_demo_rzp_1002', 'Earbuds checkout',   1),
  ('ord-demo-1003', 'PAYMENT_FAILED',    799.00, 'order_demo_rzp_1003', 'Tee payment failed', 5)
) AS v(order_id, status, amount, rzp, notes, hours)
CROSS JOIN tenants t
WHERE t.tenant_id = 'tenant-default'
  AND NOT EXISTS (SELECT 1 FROM orders o WHERE o.order_id = v.order_id);

INSERT INTO order_items (order_id, product_id, quantity, unit_price, total_price)
SELECT o.id, p.id, 1, p.price, p.price
FROM orders o
JOIN products p ON (
  (o.order_id = 'ord-demo-1001' AND p.product_id = 'prod-laptop-14') OR
  (o.order_id = 'ord-demo-1002' AND p.product_id = 'prod-earbuds') OR
  (o.order_id = 'ord-demo-1003' AND p.product_id = 'prod-tee-black')
)
WHERE NOT EXISTS (
  SELECT 1 FROM order_items oi WHERE oi.order_id = o.id AND oi.product_id = p.id
);

INSERT INTO payments (payment_id, order_id, amount, currency, payment_method, status, razorpay_order_id, razorpay_payment_id, created_at, updated_at)
SELECT v.payment_id, o.id, o.total_amount, 'INR', 'card', v.status, o.razorpay_order_id, v.rzp_pay, o.created_at, NOW()
FROM (VALUES
  ('pay-demo-1001', 'ord-demo-1001', 'CAPTURED', 'pay_demo_rzp_1001'),
  ('pay-demo-1002', 'ord-demo-1002', 'CREATED',   NULL),
  ('pay-demo-1003', 'ord-demo-1003', 'FAILED',   NULL)
) AS v(payment_id, order_id, status, rzp_pay)
JOIN orders o ON o.order_id = v.order_id
WHERE NOT EXISTS (SELECT 1 FROM payments p WHERE p.payment_id = v.payment_id);
