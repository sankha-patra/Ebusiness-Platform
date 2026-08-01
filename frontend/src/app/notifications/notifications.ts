import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatChipsModule } from '@angular/material/chips';
import { environment } from '../../environments/environment';

interface NotificationItem {
  notificationId: string;
  tenantId: string;
  orderId: string;
  paymentId: string;
  channel: string;
  recipient: string;
  message: string;
  status: string;
  read: boolean;
  createdAt: string;
}

interface PageResponse {
  content: NotificationItem[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

@Component({
  selector: 'app-notifications',
  standalone: true,
  imports: [CommonModule, MatCardModule, MatButtonModule, MatIconModule, MatListModule, MatChipsModule],
  templateUrl: './notifications.html',
  styleUrl: './notifications.scss'
})
export class NotificationsComponent implements OnInit {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiUrl;

  readonly items = signal<NotificationItem[]>([]);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  ngOnInit() {
    this.load();
  }

  load() {
    this.loading.set(true);
    this.error.set(null);
    this.http.get<PageResponse>(`${this.apiUrl}/notifications`, {
      params: { page: '0', size: '50' }
    }).subscribe({
      next: (res) => {
        this.items.set(res.content || []);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(err.error?.message || err.message);
        this.loading.set(false);
      }
    });
  }

  markAllRead() {
    this.http.post(`${this.apiUrl}/notifications/read-all`, {}).subscribe({
      next: () => this.load()
    });
  }

  markRead(id: string) {
    this.http.post(`${this.apiUrl}/notifications/${id}/read`, {}).subscribe({
      next: () => this.load()
    });
  }
}
