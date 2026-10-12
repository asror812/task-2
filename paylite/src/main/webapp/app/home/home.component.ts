import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { ReactiveFormsModule, UntypedFormControl, UntypedFormGroup, Validators } from '@angular/forms';
import { RouterModule } from '@angular/router';

import { Authority } from 'app/config/authority.constants';
import { Account } from 'app/core/auth/account.model';
import { AccountService } from 'app/core/auth/account.service';
import { LoginService } from 'app/login/login.service';
import SharedModule from 'app/shared/shared.module';
import { FormBuilder } from '@angular/forms';
import {
  Agent,
  Payment,
  PaymentCheckResponse,
  PaynetCatalogSyncResponse,
  PayliteApiService,
  Recipient,
  RecipientField,
  RecipientFieldValue,
} from './paylite-api.service';

@Component({
  selector: 'jhi-home',
  templateUrl: './home.component.html',
  styleUrl: './home.component.scss',
  imports: [SharedModule, RouterModule, ReactiveFormsModule],
})
export default class HomeComponent implements OnInit {
  readonly account = signal<Account | null>(null);
  readonly balance = signal<number | null>(null);
  readonly payments = signal<Payment[]>([]);
  readonly agents = signal<Agent[]>([]);
  readonly recipients = signal<Recipient[]>([]);
  readonly recipientFields = signal<RecipientField[]>([]);
  readonly checkResult = signal<PaymentCheckResponse | null>(null);
  readonly paymentStep = signal<'details' | 'confirmation'>('details');
  readonly page = signal(0);
  readonly pageSize = 10;
  readonly totalPages = signal(0);
  readonly totalElements = signal(0);
  readonly loading = signal(false);
  readonly submitting = signal(false);
  readonly checking = signal(false);
  readonly syncingCatalog = signal(false);
  readonly catalogSync = signal<PaynetCatalogSyncResponse | null>(null);
  readonly message = signal<string | null>(null);
  readonly error = signal<string | null>(null);

  readonly isAgent = computed(() => this.account()?.authorities.includes(Authority.AGENT) ?? false);
  readonly isAdmin = computed(() => this.account()?.authorities.includes(Authority.ADMIN) ?? false);

  readonly paymentForm = inject(FormBuilder).group({
    recipientId: [null as number | null, Validators.required],
  });

  readonly amountForm = inject(FormBuilder).group({
    amount: [null as number | null, [Validators.required, Validators.min(1)]],
  });

  fieldForm = new UntypedFormGroup({});

  readonly topUpForm = inject(FormBuilder).group({
    agentId: [null as number | null, [Validators.required, Validators.min(1)]],
    amount: [null as number | null, [Validators.required, Validators.min(1)]],
  });

  private readonly accountService = inject(AccountService);
  private readonly loginService = inject(LoginService);
  private readonly api = inject(PayliteApiService);
  private idempotencyKey: string | null = null;

  ngOnInit(): void {
    this.accountService.identity().subscribe(account => {
      this.account.set(account);
      if (account?.authorities.includes(Authority.AGENT)) {
        this.refreshAgentData();
        this.loadRecipients();
      }
      if (account?.authorities.includes(Authority.ADMIN)) {
        this.loadAgents();
      }
    });
  }

  login(): void {
    this.loginService.login();
  }

  selectRecipient(): void {
    const recipientId = this.paymentForm.controls.recipientId.value;
    this.invalidateCheck();
    this.recipientFields.set([]);
    this.fieldForm = new UntypedFormGroup({});

    if (recipientId === null) {
      return;
    }

    this.api.getRecipientFields(recipientId).subscribe({
      next: fields => {
        this.recipientFields.set(fields);
        this.buildFieldForm(fields);
      },
      error: response => this.error.set(this.readError(response)),
    });
  }

  checkPayment(): void {
    if (this.paymentForm.invalid || this.fieldForm.invalid || this.checking()) {
      this.paymentForm.markAllAsTouched();
      this.fieldForm.markAllAsTouched();
      return;
    }

    const recipientId = this.paymentForm.controls.recipientId.value!;
    const fields = this.paymentFields();
    this.clearFeedback();
    this.checking.set(true);

    this.api.checkPayment(recipientId, fields).subscribe({
      next: result => {
        this.checking.set(false);
        if (!result.valid || !result.checkId) {
          this.error.set(result.errors.join(', ') || 'Проверка реквизитов не пройдена.');
          return;
        }
        this.checkResult.set(result);
        this.idempotencyKey = crypto.randomUUID();
        this.amountForm.reset();
        this.paymentStep.set('confirmation');
      },
      error: response => {
        this.error.set(this.readError(response));
        this.checking.set(false);
      },
    });
  }

  createPayment(): void {
    const checked = this.checkResult();
    const recipientId = this.paymentForm.controls.recipientId.value;
    if (this.amountForm.invalid || this.submitting() || !checked?.checkId || !recipientId || !this.idempotencyKey) {
      this.amountForm.markAllAsTouched();
      return;
    }

    const amount = this.amountForm.controls.amount.value!;
    const fields = this.paymentFields();
    this.clearFeedback();
    this.submitting.set(true);

    this.api
      .createPayment({
        recipientId,
        checkId: checked.checkId,
        idempotencyKey: this.idempotencyKey,
        fields,
        account: this.paymentReference(fields),
        amount,
      })
      .subscribe({
        next: payment => {
          this.message.set(`Платёж #${payment.id} успешно выполнен. Списано ${this.formatTiyin(payment.totalAmount)} тийин.`);
          this.resetPaymentFlow();
          this.page.set(0);
          this.refreshAgentData();
          this.submitting.set(false);
        },
        error: (response: HttpErrorResponse) => {
          const failedPayment = response.error as Payment | undefined;
          if (response.status === 422 && failedPayment?.status === 'FAILED') {
            this.error.set(`Недостаточно средств. Платёж сохранён со статусом FAILED.`);
            this.page.set(0);
            this.refreshAgentData();
          } else {
            this.error.set(this.readError(response));
          }
          this.submitting.set(false);
        },
      });
  }

  editPaymentDetails(): void {
    this.invalidateCheck();
  }

  fieldInputType(field: RecipientField): string {
    if (field.type === 'NUMBER' || field.type === 'AMOUNT') return 'number';
    if (field.type === 'PHONE') return 'tel';
    if (field.type === 'DATE') return 'date';
    return 'text';
  }

  availableValues(field: RecipientField): RecipientFieldValue[] {
    const values = field.values;
    if (values.every(value => value.parentId === null)) {
      return values;
    }

    const selectedValueIds = new Set<number>();
    for (const candidateField of this.recipientFields()) {
      const selected = this.fieldForm.get(candidateField.name)?.value;
      const selectedValue = candidateField.values.find(value => value.value === selected);
      if (selectedValue) selectedValueIds.add(selectedValue.id);
    }
    return values.filter(value => value.parentId === null || selectedValueIds.has(value.parentId));
  }

  fieldChanged(): void {
    this.clearFeedback();
    for (const field of this.recipientFields().filter(item => item.type === 'SELECT')) {
      const control = this.fieldForm.get(field.name);
      if (control?.value && !this.availableValues(field).some(option => option.value === control.value)) {
        control.reset();
      }
    }
  }

  topUp(): void {
    if (this.topUpForm.invalid || this.submitting()) {
      this.topUpForm.markAllAsTouched();
      return;
    }

    const agentId = Number(this.topUpForm.controls.agentId.value);
    const amount = this.topUpForm.controls.amount.value!;
    this.clearFeedback();
    this.submitting.set(true);

    this.api.topUp(agentId, amount).subscribe({
      next: () => {
        const agent = this.agents().find(item => item.id === agentId);
        this.message.set(`Баланс пользователя ${agent?.login ?? `#${agentId}`} пополнен на ${this.formatTiyin(amount)} тийин.`);
        this.topUpForm.reset();
        this.loadAgents();
        this.submitting.set(false);
      },
      error: (response: HttpErrorResponse) => {
        this.error.set(this.readError(response));
        this.submitting.set(false);
      },
    });
  }

  syncPaynetCatalog(): void {
    if (this.syncingCatalog()) {
      return;
    }

    this.clearFeedback();
    this.syncingCatalog.set(true);
    this.api.syncPaynetCatalog().subscribe({
      next: result => {
        this.catalogSync.set(result);
        this.message.set(
          `Каталог Paynet обновлён: ${result.categoriesProcessed} категорий, ${result.recipientsProcessed} услуг, ` +
            `${result.fieldsProcessed} полей и ${result.valuesProcessed} значений.`,
        );
        if (this.isAgent()) {
          this.loadRecipients();
        }
        this.syncingCatalog.set(false);
      },
      error: (response: HttpErrorResponse) => {
        this.error.set(this.readError(response));
        this.syncingCatalog.set(false);
      },
    });
  }

  previousPage(): void {
    if (this.page() > 0) {
      this.page.update(value => value - 1);
      this.loadPayments();
    }
  }

  nextPage(): void {
    if (this.page() + 1 < this.totalPages()) {
      this.page.update(value => value + 1);
      this.loadPayments();
    }
  }

  refreshAgentData(): void {
    this.loadBalance();
    this.loadPayments();
  }

  formatTiyin(value: number): string {
    return new Intl.NumberFormat('ru-RU').format(value);
  }

  formatSom(value: number): string {
    return new Intl.NumberFormat('ru-RU', { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(value / 100);
  }

  selectedRecipient(): Recipient | undefined {
    const recipientId = this.paymentForm.controls.recipientId.value;
    return this.recipients().find(recipient => recipient.id === recipientId);
  }

  private loadBalance(): void {
    this.api.getBalance().subscribe({
      next: response => this.balance.set(response.balance),
      error: response => this.error.set(this.readError(response)),
    });
  }

  private loadRecipients(): void {
    this.api.getRecipients().subscribe({
      next: recipients => this.recipients.set(recipients),
      error: response => this.error.set(this.readError(response)),
    });
  }

  private buildFieldForm(fields: RecipientField[]): void {
    const controls: Record<string, UntypedFormControl> = {};
    for (const field of fields) {
      const validators = [];
      if (field.required) validators.push(Validators.required);
      if (field.validationRegex) validators.push(Validators.pattern(field.validationRegex));
      controls[field.name] = new UntypedFormControl('', validators);
    }
    this.fieldForm = new UntypedFormGroup(controls);
  }

  private paymentFields(): Record<string, string> {
    return { ...this.fieldForm.getRawValue() } as Record<string, string>;
  }

  private paymentReference(fields: Record<string, string>): string {
    const preferred = fields['accountNumber'] || fields['phoneNumber'] || '';
    if (preferred.trim()) {
      return preferred.trim();
    }
    const firstValue = Object.values(fields).find(value => value.trim());
    return firstValue === undefined ? (this.selectedRecipient()?.name ?? 'payment') : firstValue.trim();
  }

  private invalidateCheck(): void {
    this.checkResult.set(null);
    this.idempotencyKey = null;
    this.paymentStep.set('details');
    this.amountForm.reset();
  }

  private resetPaymentFlow(): void {
    this.paymentForm.reset();
    this.recipientFields.set([]);
    this.fieldForm = new UntypedFormGroup({});
    this.invalidateCheck();
  }

  private loadAgents(): void {
    this.api.getAgents().subscribe({
      next: agents => this.agents.set(agents),
      error: response => this.error.set(this.readError(response)),
    });
  }

  private loadPayments(): void {
    this.loading.set(true);
    this.api.getPayments(this.page(), this.pageSize).subscribe({
      next: response => {
        this.payments.set(response.content);
        this.totalElements.set(response.totalElements);
        this.totalPages.set(response.totalPages);
        this.loading.set(false);
      },
      error: response => {
        this.error.set(this.readError(response));
        this.loading.set(false);
      },
    });
  }

  private clearFeedback(): void {
    this.message.set(null);
    this.error.set(null);
  }

  private readError(response: HttpErrorResponse): string {
    if (typeof response.error === 'object' && response.error !== null) {
      const body = response.error as Record<string, unknown>;
      if (typeof body['detail'] === 'string') {
        return body['detail'];
      }
      if (typeof body['message'] === 'string') {
        return body['message'];
      }
    }
    return `Ошибка запроса (${response.status})`;
  }
}
