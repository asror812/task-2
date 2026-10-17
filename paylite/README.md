# PayLite

PayLite - JHipster monolit arxitekturasi asosida yaratilgan kichik billing ilovasi.
Tizimga kirgan agent mijoz to'lovini qabul qiladi, ilova komissiyani hisoblaydi va umumiy summani agent depozitidan yechadi.
Administrator agent balansini to'ldirishi mumkin.

Barcha pul qiymatlari **tiyinda** saqlanadi va uzatiladi (`1 so'm = 100 tiyin`).

## Texnologiyalar

- Java 17 va Spring Boot 3.4.5
- JHipster 8.11.0
- Angular
- PostgreSQL 17 va Spring Data JPA
- Liquibase
- Keycloak (OAuth 2.0 / OpenID Connect)
- Spring Cloud Consul Config va Discovery
- Maven
- Docker Compose

## Arxitektura

```text
Angular UI
    |
REST resource'lar (@PreAuthorize, validatsiya)
    |
Service qatlami (biznes qoidalari va tranzaksiyalar)
    |
Spring Data JPA repository'lari
    |
PostgreSQL

Keycloak  -> autentifikatsiya va ROLE_AGENT / ROLE_ADMIN
Consul    -> payment.commission-percent, payment.min-amount, payment.max-amount
Paynet    -> xizmatlar katalogi, to'lovni tekshirish va bajarish
Liquibase -> sxema migratsiyalari va boshlang'ich Agent yozuvlari
```

Asosiy entity'lar:

- `Agent`: Keycloak foydalanuvchi logini va uning tiyindagi balansi.
- `Payment`: CHECK vaqtida shakllangan to'lov rekvizitlari (`paymentReference`), summa, komissiya, umumiy summa, holat, yaratilgan vaqt va egasi bo'lgan agent.
- `TopUpLedger`: administrator bajargan balans to'ldirishlar auditi.
- `PaynetCategory`, `PaynetRecipient`, `PaynetRecipientField`, `PaynetRecipientFieldValue`: lokal Paynet katalogi.
- `PaynetCatalogSync`: katalog sinxronizatsiyasi natijasi va vaqti.

Keycloak'dagi foydalanuvchi nomi (`preferred_username`) `Agent.login` bilan bog'lanadi.

## Biznes qoidalari va tanlangan yechimlar

### Pul qiymatini saqlash

Summalar eng kichik pul birligi bo'lgan tiyinda `Long` sifatida saqlanadi.

### Komissiya va yaxlitlash

Komissiya foizi Consul'dan `BigDecimal` sifatida olinadi:

```text
commission = amount * commissionPercent / 100
total      = amount + commission
```

Hisoblangan komissiya `RoundingMode.HALF_UP` yordamida eng yaqin butun tiyingacha yaxlitlanadi. Masalan, `1.5` tiyin natija `2` tiyinga aylanadi.

### To'lov natijasi

- Balans umumiy summaga teng yoki undan katta bo'lsa, summa balansdan yechiladi va to'lov `PAID` holatida saqlanadi.
- Balans yetarli bo'lmasa, balans o'zgarmaydi va to'lov `FAILED` holatida saqlanadi; API HTTP `422` qaytaradi.
- Balansdan yechish va to'lovni saqlash bitta `@Transactional` service operatsiyasi ichida bajariladi. Saqlashda xato yuz bersa, tranzaksiya rollback qilinadi.
- Xizmat maydonlari CHECK bosqichida Paynet orqali tekshiriladi va keyingi PAY so'rovi aynan shu saqlangan ma'lumotlardan foydalanadi. To'lov va top-up summalari musbat bo'lishi shart. Top-up uchun yuqori chegara `TOP_UP_MAX_AMOUNT` orqali sozlanadi (standart qiymat: `100000000` tiyin).
- Agent faqat autentifikatsiyadan o'tgan o'z loginiga tegishli to'lovlarni ko'ra oladi.

## Lokal ishga tushirish

### 1. PostgreSQL, Keycloak va Consul'ni ishga tushirish

Loyihaning asosiy papkasida:

```bash
./start.sh
```

Servislar quyidagi manzillarda ishlaydi:

| Servis     | Manzil                  |
| ---------- | ----------------------- |
| PostgreSQL | `localhost:5433`        |
| Keycloak   | `http://localhost:9080` |
| Consul     | `http://localhost:8500` |

### 2. Consul'ga komissiya qiymatini qo'shish

Consul UI orqali quyidagi qiymatlarni yarating:

```text
config/paylite-service/payment.commission-percent = 1.5
config/paylite-service/payment.min-amount = 100
config/paylite-service/payment.max-amount = 100000000
```

Yoki quyidagi buyruqlarni bajaring:

```bash
curl --request PUT --data '1.5' \
  http://localhost:8500/v1/kv/config/paylite-service/payment.commission-percent

curl --request PUT --data '100' \
  http://localhost:8500/v1/kv/config/paylite-service/payment.min-amount

curl --request PUT --data '100000000' \
  http://localhost:8500/v1/kv/config/paylite-service/payment.max-amount
```

Ilova qiymatlarni `@ConfigurationProperties(prefix = "payment")` orqali oladi. Lokal muhitda qiymat o'zgartirilgandan keyin backend'ni qayta ishga tushirish eng aniq usul hisoblanadi.

### 3. Backend'ni ishga tushirish

Paynet OAuth2 client secret'ini environment variable orqali bering:

```bash
export PAYNET_CLIENT_SECRET='<Keycloak paylite-service client secret>'
```

```bash
./mvnw
```

Backend `http://localhost:8080` manzilida ishga tushadi. Liquibase sxemani yaratadi va boshlang'ich `admin` hamda `agent` yozuvlarini avtomatik ravishda qo'shadi. Hibernate orqali DDL yaratish `ddl-auto=none` bilan o'chirilgan.

### 4. Angular frontend'ni ishga tushirish

Ikkinchi terminalda:

```bash
./npmw install
./npmw start
```

Brauzerda `http://localhost:9000` manzilini oching.

## Demo foydalanuvchilar

| Foydalanuvchi | Parol   | Rol          | Bazadagi agent ID | Boshlang'ich balans |
| ------------- | ------- | ------------ | ----------------: | ------------------: |
| `agent`       | `agent` | `ROLE_AGENT` |                 2 |   100 000 000 tiyin |
| `admin`       | `admin` | `ROLE_ADMIN` |                 1 |             0 tiyin |

Keycloak realm umumiy loyihadagi `realm-config` papkasidan import qilinadi.

## API

| Method | Endpoint                                                    | Rol          | Tavsif                                   |
| ------ | ----------------------------------------------------------- | ------------ | ---------------------------------------- |
| `GET`  | `/api/payments/catalog/recipients?page=0&size=20`           | `ROLE_AGENT` | Lokal katalogdagi faol xizmatlar         |
| `GET`  | `/api/payments/catalog/recipients/{id}/fields?method=CHECK` | `ROLE_AGENT` | Xizmatning `CHECK` yoki `PAY` maydonlari |
| `POST` | `/api/payments/check`                                       | `ROLE_AGENT` | Paynet orqali maydonlarni tekshirish     |
| `POST` | `/api/payments`                                             | `ROLE_AGENT` | To'lovni navbatga qo'yish (`202`)        |
| `GET`  | `/api/payments/{id}`                                        | `ROLE_AGENT` | Bitta to'lovning joriy holatini olish    |
| `GET`  | `/api/payments?page=0&size=10`                              | `ROLE_AGENT` | Joriy agent to'lovlarini sahifalab olish |
| `GET`  | `/api/agents/me/balance`                                    | `ROLE_AGENT` | Joriy agent balansini olish              |
| `GET`  | `/api/agents?page=0&size=20`                                | `ROLE_ADMIN` | Agentlarni sahifalab olish               |
| `POST` | `/api/agents/{id}/topup`                                    | `ROLE_ADMIN` | Agent balansini to'ldirish               |
| `POST` | `/api/admin/paynet-catalog/sync`                            | `ROLE_ADMIN` | Paynet katalogini qo'lda sinxronlash     |
| `GET`  | `/api/admin/paynet-catalog/latest`                          | `ROLE_ADMIN` | Oxirgi sinxronizatsiya natijasi          |

Recipient maydonlarini olishda `method` majburiy query parametri bo'lib, uning qiymati
`CHECK` yoki `PAY` bo'lishi kerak.

Paynet katalogi standart holatda har kuni soat `02:00` da (`Asia/Tashkent`) avtomatik sinxronlashtiriladi. Jadvalni `PAYNET_CATALOG_SYNC_CRON` va vaqt zonasini `PAYNET_CATALOG_SYNC_ZONE` orqali o'zgartirish mumkin.

To'lovni tekshirish so'rovi:

```json
{
  "recipientId": 2,
  "amount": 4000000,
  "fields": {
    "phoneNumber": "+998901234567"
  }
}
```

### Asinxron to'lovlarni qayta ishlash

To'lov yaratish, balansni rezerv qilish va `payment_outbox_event` yozuvini yaratish bitta
tranzaksiyada bajariladi. API `202 Accepted` va `PENDING` holatini qaytaradi. Outbox publisher
xabarni RabbitMQ'dagi `payment.process` navbatiga yuboradi, consumer esa Paynet'ga murojaat qiladi.

Vaqtinchalik xatoda qayta urinishlar `10 soniya -> 30 soniya -> 2 daqiqa`
oralig'ida bajariladi. Standart maksimal urinishlar soni 4. Natija noma'lum bo'lib qolsa,
to'lov `RECONCILIATION_REQUIRED` holatiga o'tadi va `payment.dlq` navbatiga yuboriladi;
bu holatda rezerv qilingan pul avtomatik qaytarilmaydi. Har 15 daqiqada ishlaydigan safety job
yo'qolgan xabarlar va uzoq vaqt `PENDING`/`PROCESSING` holatida qolgan to'lovlarni qayta navbatga qo'yadi.

Birinchi urinish Paynet'ning `POST /pay` endpointini chaqiradi. Timeoutdan keyingi urinishlar
avval `GET /pay/status?idempotencyKey=...` orqali holatni tekshiradi: `PAID` lokal to'lovni
yakunlaydi, `PROCESSING` keyingi tekshiruvni rejalashtiradi va faqat `NOT_FOUND` yangi `POST /pay`
so'roviga ruxsat beradi.

Asosiy sozlamalar:

```bash
RABBITMQ_HOST=localhost
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=paylite
RABBITMQ_PASSWORD=paylite
PAYMENT_MAX_ATTEMPTS=4
PAYMENT_OUTBOX_BATCH_SIZE=50
PAYMENT_OUTBOX_PUBLISH_DELAY_MS=1000
PAYMENT_RECONCILIATION_DELAY_MS=900000
PAYMENT_RECONCILIATION_STALE_AFTER_MS=900000
```

Development RabbitMQ management UI: `http://localhost:15672` (`paylite` / `paylite`).

To'lov yaratish so'rovi:

```json
{
  "idempotencyKey": "550e8400-e29b-41d4-a716-446655440000",
  "checkId": "paynet-check-id"
}
```

Paylite `recipientId`, summa va ko'rsatiladigan `paymentReference` qiymatini Paynet'da
`checkId` bo'yicha saqlangan, oldindan tekshirilgan ma'lumotlardan oladi. `pay` so'rovi ham
faqat `checkId` va `idempotencyKey` yuboradi; rekvizitlar mijozdan qayta qabul qilinmaydi.

Balansni to'ldirish so'rovi:

```json
{
  "amount": 1000000,
  "idempotencyKey": "18b7d63e-53a3-4c5c-930b-ae7a03de2948"
}
```

## Infrastrukturani to'xtatish

Loyihaning asosiy papkasida:

```bash
./down.sh
```
