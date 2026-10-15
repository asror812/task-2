import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { ApplicationConfigService } from 'app/core/config/application-config.service';

export type PaymentStatus = 'PENDING' | 'PAID' | 'FAILED' | 'REVERSED' | 'REFUNDED';

export interface Payment {
  id: number;
  account: string;
  amount: number;
  commissionAmount: number;
  totalAmount: number;
  status: PaymentStatus;
  createdDate: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
}

export type PaymentPage = PageResponse<Payment>;
export type AgentPage = PageResponse<Agent>;
export type RecipientPage = PageResponse<Recipient>;

export interface BalanceResponse {
  balance: number;
}

export interface Agent {
  id: number;
  login: string;
  balance: number;
}

export interface TopUpResponse {
  id: number;
  agentId: number;
  amount: number;
  balanceBefore: number;
  balanceAfter: number;
  createdAt: string;
}

export interface Recipient {
  id: number;
  name: string;
  code: string;
  active: boolean;
  categoryName: string;
  categoryId: number;
}

export type RecipientFieldType = 'TEXT' | 'NUMBER' | 'PHONE' | 'AMOUNT' | 'DATE' | 'SELECT';
export type RecipientFieldMethod = 'CHECK' | 'PAY';

export interface RecipientFieldValue {
  id: number;
  value: string;
  label: string;
  active: boolean;
  sortOrder: number;
  recipientFieldId: number;
  parentId: number | null;
}

export interface RecipientField {
  id: number;
  name: string;
  label: string;
  validationRegex: string | null;
  method: RecipientFieldMethod;
  active: boolean;
  required: boolean;
  type: RecipientFieldType;
  sortOrder: number;
  recipientId: number;
  recipientName: string;
  categoryName: string;
  values: RecipientFieldValue[];
}

export interface PaymentCheckResponse {
  valid: boolean;
  recipientName: string;
  checkId: string | null;
  errors: string[];
}

export interface PaynetCatalogSyncResponse {
  id: number;
  status: 'RUNNING' | 'SUCCESS' | 'FAILED';
  startedAt: string;
  finishedAt: string | null;
  categoriesProcessed: number;
  recipientsProcessed: number;
  fieldsProcessed: number;
  valuesProcessed: number;
  errorMessage: string | null;
}

export interface CreatePaymentRequest {
  recipientId: number;
  idempotencyKey: string;
  checkId: string;
  fields: Record<string, string>;
  account: string;
  amount: number;
}

@Injectable({ providedIn: 'root' })
export class PayliteApiService {
  private readonly http = inject(HttpClient);
  private readonly applicationConfigService = inject(ApplicationConfigService);

  private readonly paymentsUrl = this.applicationConfigService.getEndpointFor('api/payments');
  private readonly agentsUrl = this.applicationConfigService.getEndpointFor('api/agents');
  private readonly paynetCatalogUrl = this.applicationConfigService.getEndpointFor('api/admin/paynet-catalog');

  getBalance(): Observable<BalanceResponse> {
    return this.http.get<BalanceResponse>(`${this.agentsUrl}/me/balance`);
  }

  getAgents(page: number, size: number): Observable<AgentPage> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<AgentPage>(this.agentsUrl, { params });
  }

  getRecipients(page: number, size: number): Observable<RecipientPage> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<RecipientPage>(`${this.paymentsUrl}/catalog/recipients`, { params });
  }

  getRecipientFields(recipientId: number, method: RecipientFieldMethod): Observable<RecipientField[]> {
    const params = new HttpParams().set('method', method);
    return this.http.get<RecipientField[]>(`${this.paymentsUrl}/catalog/recipients/${recipientId}/fields`, { params });
  }

  checkPayment(recipientId: number, fields: Record<string, string>, amount: number): Observable<PaymentCheckResponse> {
    return this.http.post<PaymentCheckResponse>(`${this.paymentsUrl}/check`, { recipientId, fields, amount });
  }

  getPayments(page: number, size: number): Observable<PaymentPage> {
    const params = new HttpParams().set('page', page).set('size', size).set('sort', 'createdDate,desc');
    return this.http.get<PaymentPage>(this.paymentsUrl, { params });
  }

  createPayment(request: CreatePaymentRequest): Observable<Payment> {
    return this.http.post<Payment>(this.paymentsUrl, request);
  }

  topUp(agentId: number, amount: number, idempotencyKey: string): Observable<TopUpResponse> {
    return this.http.post<TopUpResponse>(`${this.agentsUrl}/${agentId}/topup`, { amount, idempotencyKey });
  }

  syncPaynetCatalog(): Observable<PaynetCatalogSyncResponse> {
    return this.http.post<PaynetCatalogSyncResponse>(`${this.paynetCatalogUrl}/sync`, {});
  }

  getLatestPaynetCatalogSync(): Observable<PaynetCatalogSyncResponse> {
    return this.http.get<PaynetCatalogSyncResponse>(`${this.paynetCatalogUrl}/latest`);
  }
}
