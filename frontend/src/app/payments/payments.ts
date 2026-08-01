import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { environment } from '../../environments/environment';

declare var Razorpay: any;

interface PaymentStatusResponse {
  paymentId: string;
  razorpayPaymentId: string;
  razorpayOrderId: string;
  status: string;
  amount: number;
  currency: string;
  paymentMethod: string;
  createdAt: string;
  updatedAt: string;
}

interface RazorpayOrderResponse {
  razorpayOrderId: string;
  orderId?: string;
  paymentId?: string;
  currency: string;
  amount: string;
  receipt: string;
  mock?: string;
  message?: string;
}

interface VerifyResponse {
  status: string;
  outcome: string;
  paymentId: string;
  orderId: string;
  razorpayPaymentId: string;
  razorpayOrderId: string;
  message: string;
}

interface SelectedProduct {
  productId: string;
  name: string;
  price: number;
  currency: string;
}

@Component({
  selector: 'app-payments',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterLink,
    MatCardModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressBarModule,
    MatSnackBarModule
  ],
  templateUrl: './payments.html',
  styleUrl: './payments.scss'
})
export class PaymentsComponent implements OnInit {
  private readonly http = inject(HttpClient);
  private readonly snackBar = inject(MatSnackBar);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly apiUrl = environment.apiUrl;

  amount: number | null = null;
  receipt = '';
  razorpayOrderId = '';
  amountLocked = false;

  readonly selectedProduct = signal<SelectedProduct | null>(null);
  readonly paymentStatus = signal<PaymentStatusResponse | null>(null);
  readonly razorpayOrderResponse = signal<RazorpayOrderResponse | null>(null);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly confirmSuccess = signal(false);
  readonly confirmFailed = signal(false);
  readonly confirmTitle = signal('');
  readonly confirmMessage = signal('');
  readonly confirmDetails = signal<Record<string, string>>({});

  ngOnInit() {
    this.route.queryParamMap.subscribe((params) => {
      const productId = params.get('productId');
      const name = params.get('name');
      const price = Number(params.get('price'));
      const currency = params.get('currency') || 'INR';

      if (productId && name && !Number.isNaN(price) && price > 0) {
        this.selectedProduct.set({ productId, name, price, currency });
        this.amount = price;
        this.amountLocked = true;
        this.receipt = `buy-${productId}-${Date.now()}`;
      } else {
        this.selectedProduct.set(null);
        this.amountLocked = false;
        if (this.amount == null) {
          this.amount = 100;
        }
        if (!this.receipt) {
          this.receipt = 'rcpt-' + Date.now();
        }
      }
    });
  }

  clearProduct() {
    this.router.navigate(['/payments']);
  }

  createRazorpayOrder() {
    const product = this.selectedProduct();
    if (!product) {
      this.error.set('Select a product from Products and click Buy first.');
      return;
    }
    if (!this.amount || !this.receipt) {
      this.error.set('Enter amount and receipt ID');
      return;
    }

    this.clearConfirmations();
    this.loading.set(true);
    this.error.set(null);
    this.razorpayOrderResponse.set(null);
    this.paymentStatus.set(null);

    this.http.post<RazorpayOrderResponse>(
      `${this.apiUrl}/payments/create-order`,
      null,
      { params: { amount: this.amount.toString(), receipt: this.receipt } }
    ).subscribe({
      next: (response) => {
        this.razorpayOrderResponse.set(response);
        this.razorpayOrderId = response.razorpayOrderId;
        this.loading.set(false);

        if (response.mock === 'true' || response.razorpayOrderId?.startsWith('order_mock_')) {
          this.showFailed('Circuit breaker open', response.message || 'Razorpay unavailable.');
          return;
        }

        this.openRazorpayCheckout(response);
      },
      error: (err) => {
        this.loading.set(false);
        this.showFailed('Could not create payment order', err.error?.message || err.message);
      }
    });
  }

  openRazorpayCheckout(order: RazorpayOrderResponse) {
    if (typeof Razorpay === 'undefined') {
      this.showFailed('Checkout unavailable', 'Razorpay script not loaded. Refresh the page.');
      return;
    }

    const product = this.selectedProduct();
    const description = product
      ? `Buy ${product.name} (${product.productId})`
      : `Payment for ${order.receipt}`;

    const rzp = new Razorpay({
      key: environment.razorpayKeyId,
      amount: order.amount,
      currency: order.currency,
      name: 'EBusiness Platform',
      description,
      order_id: order.razorpayOrderId,
      prefill: { name: 'Test User', email: 'test@example.com', contact: '9999999999' },
      theme: { color: '#1a1a1a' },
      handler: (response: any) => this.verifyPayment(response),
      modal: {
        ondismiss: () => this.showFailed(
          'Payment cancelled',
          'Checkout closed before completion. Order stays PAYMENT_PENDING.'
        )
      }
    });

    rzp.on('payment.failed', (response: any) => {
      this.showFailed(
        'Payment failed',
        response?.error?.description || 'Payment failed',
        { razorpayOrderId: order.razorpayOrderId }
      );
    });
    rzp.open();
  }

  verifyPayment(razorpayResponse: any) {
    this.loading.set(true);
    const product = this.selectedProduct();
    this.http.post<VerifyResponse>(`${this.apiUrl}/payments/verify`, {
      razorpay_order_id: razorpayResponse.razorpay_order_id,
      razorpay_payment_id: razorpayResponse.razorpay_payment_id,
      razorpay_signature: razorpayResponse.razorpay_signature
    }).subscribe({
      next: (res) => {
        this.loading.set(false);
        const details: Record<string, string> = {
          orderId: res.orderId,
          paymentId: res.paymentId,
          razorpayPaymentId: res.razorpayPaymentId
        };
        if (product) {
          details['productId'] = product.productId;
          details['product'] = product.name;
        }
        this.showSuccess('Payment confirmed', res.message || 'Payment verified.', details);
        this.snackBar.open(
          product
            ? `Paid for ${product.name}. Check Messages for SMS confirmation.`
            : 'Payment verified. Waiting for Kafka confirmation SMS…',
          'OK',
          { duration: 5000 }
        );
        this.razorpayOrderId = res.razorpayOrderId;
      },
      error: (err) => {
        this.loading.set(false);
        this.showFailed('Verification failed', err.error?.message || err.message, {
          razorpayPaymentId: razorpayResponse.razorpay_payment_id,
          razorpayOrderId: razorpayResponse.razorpay_order_id
        });
      }
    });
  }

  getPaymentStatus() {
    if (!this.razorpayOrderId) {
      this.error.set('Enter Razorpay Order ID');
      return;
    }

    this.loading.set(true);
    this.error.set(null);
    this.paymentStatus.set(null);

    this.http.get<PaymentStatusResponse>(
      `${this.apiUrl}/payments/${this.razorpayOrderId}/status`
    ).subscribe({
      next: (response) => {
        this.paymentStatus.set(response);
        this.loading.set(false);
        if (response.status === 'COMPLETED') {
          this.showSuccess('Payment status: COMPLETED', 'Payment is complete in local DB.');
        } else if (response.status === 'FAILED') {
          this.showFailed('Payment status: FAILED', 'Payment is marked failed in local DB.');
        }
      },
      error: (err) => {
        this.error.set(err.error?.message || err.message);
        this.loading.set(false);
      }
    });
  }

  dismissConfirmation() {
    this.clearConfirmations();
  }

  private showSuccess(title: string, message: string, details: Record<string, string> = {}) {
    this.confirmFailed.set(false);
    this.confirmSuccess.set(true);
    this.confirmTitle.set(title);
    this.confirmMessage.set(message);
    this.confirmDetails.set(details);
    this.error.set(null);
  }

  private showFailed(title: string, message: string, details: Record<string, string> = {}) {
    this.confirmSuccess.set(false);
    this.confirmFailed.set(true);
    this.confirmTitle.set(title);
    this.confirmMessage.set(message);
    this.confirmDetails.set(details);
  }

  private clearConfirmations() {
    this.confirmSuccess.set(false);
    this.confirmFailed.set(false);
    this.confirmTitle.set('');
    this.confirmMessage.set('');
    this.confirmDetails.set({});
  }
}
