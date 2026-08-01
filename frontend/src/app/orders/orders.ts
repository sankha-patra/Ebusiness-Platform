import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { environment } from '../../environments/environment';

interface OrderSummary {
  orderId: string;
  tenantId: string;
  status: string;
  totalAmount: number;
  currency: string;
  paymentStatus: string;
  razorpayOrderId: string;
  createdAt: string;
  updatedAt: string;
}

interface PageResponse {
  content: OrderSummary[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

@Component({
  selector: 'app-orders',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatTableModule,
    MatPaginatorModule,
    MatProgressBarModule
  ],
  templateUrl: './orders.html',
  styleUrl: './orders.scss'
})
export class OrdersComponent implements OnInit {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiUrl;

  readonly displayedColumns = ['orderId', 'status', 'paymentStatus', 'totalAmount', 'razorpayOrderId', 'createdAt'];
  readonly orders = signal<OrderSummary[]>([]);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly page = signal(0);
  readonly size = signal(10);
  readonly totalElements = signal(0);

  ngOnInit() {
    this.loadOrders();
  }

  loadOrders() {
    this.loading.set(true);
    this.error.set(null);

    this.http.get<PageResponse>(`${this.apiUrl}/orders`, {
      params: {
        page: this.page().toString(),
        size: this.size().toString()
      }
    }).subscribe({
      next: (res) => {
        this.orders.set(res.content || []);
        this.page.set(res.page);
        this.size.set(res.size);
        this.totalElements.set(res.totalElements);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set('Failed to load orders: ' + (err.error?.message || err.message));
        this.orders.set([]);
        this.loading.set(false);
      }
    });
  }

  onPage(event: PageEvent) {
    this.page.set(event.pageIndex);
    this.size.set(event.pageSize);
    this.loadOrders();
  }
}
