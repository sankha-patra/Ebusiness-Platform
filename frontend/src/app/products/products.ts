import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { environment } from '../../environments/environment';

export interface ProductResponse {
  productId: string;
  categoryId: string;
  categoryName: string;
  name: string;
  description: string;
  price: number;
  currency: string;
  stockQuantity: number;
  status: string;
  imageUrl: string;
  createdAt: string;
  updatedAt: string;
}

@Component({
  selector: 'app-products',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatCardModule,
    MatButtonModule,
    MatFormFieldModule,
    MatSelectModule,
    MatProgressBarModule
  ],
  templateUrl: './products.html',
  styleUrl: './products.scss'
})
export class ProductsComponent implements OnInit {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly apiUrl = environment.apiUrl;

  selectedCategory = '';
  readonly products = signal<ProductResponse[]>([]);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  ngOnInit() {
    this.loadProducts();
  }

  loadProducts() {
    this.loading.set(true);
    this.error.set(null);
    this.products.set([]);

    const endpoint = this.selectedCategory
      ? `${this.apiUrl}/products/category/${this.selectedCategory}`
      : `${this.apiUrl}/products`;

    this.http.get<ProductResponse[]>(endpoint).subscribe({
      next: (response) => {
        this.products.set(Array.isArray(response) ? response : []);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set('Failed to load products: ' + (err.error?.message || err.message));
        this.products.set([]);
        this.loading.set(false);
      }
    });
  }

  buy(product: ProductResponse) {
    this.router.navigate(['/payments'], {
      queryParams: {
        productId: product.productId,
        name: product.name,
        price: product.price,
        currency: product.currency || 'INR'
      }
    });
  }
}
