import { Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatBadgeModule } from '@angular/material/badge';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { HttpClient } from '@angular/common/http';
import { environment } from '../environments/environment';

interface NotificationItem {
  notificationId: string;
  orderId: string;
  paymentId: string;
  message: string;
  channel: string;
  read: boolean;
  createdAt: string;
}

@Component({
  selector: 'app-root',
  imports: [
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    MatToolbarModule,
    MatButtonModule,
    MatIconModule,
    MatBadgeModule,
    MatSnackBarModule
  ],
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class App implements OnInit, OnDestroy {
  private readonly http = inject(HttpClient);
  private readonly snackBar = inject(MatSnackBar);
  private readonly apiUrl = environment.apiUrl;
  private pollTimer: ReturnType<typeof setInterval> | null = null;
  private seenIds = new Set<string>();

  readonly unreadCount = signal(0);

  ngOnInit() {
    this.pollNotifications();
    this.pollTimer = setInterval(() => this.pollNotifications(), 4000);
  }

  ngOnDestroy() {
    if (this.pollTimer) {
      clearInterval(this.pollTimer);
    }
  }

  private pollNotifications() {
    this.http.get<NotificationItem[]>(`${this.apiUrl}/notifications/unread`).subscribe({
      next: (items) => {
        this.unreadCount.set(items.length);
        for (const item of items) {
          if (!this.seenIds.has(item.notificationId)) {
            this.seenIds.add(item.notificationId);
            this.snackBar.open(item.message, 'View', {
              duration: 7000,
              horizontalPosition: 'end',
              verticalPosition: 'top',
              panelClass: ['payment-success-snack']
            }).onAction().subscribe(() => {
              window.location.href = '/notifications';
            });
            // Prevent the same Kafka/load-test message from popping on every refresh
            this.http.post(`${this.apiUrl}/notifications/${item.notificationId}/read`, {}).subscribe();
          }
        }
      },
      error: () => {
        /* gateway/backend may be restarting */
      }
    });
  }
}
