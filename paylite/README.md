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
- `Payment`: hisob raqami, summa, komissiya, umumiy summa, holat, yaratilgan vaqt va egasi bo'lgan agent.
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
- Hisob raqami va xizmat maydonlari tanlangan Paynet xizmati orqali tekshiriladi. To'lov va top-up summalari musbat bo'lishi shart. Top-up uchun yuqori chegara `TOP_UP_MAX_AMOUNT` orqali sozlanadi (standart qiymat: `100000000` tiyin).
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

| Method | Endpoint                       | Rol          | Tavsif                                   |
| ------ | ------------------------------ | ------------ | ---------------------------------------- |
| `GET`  | `/api/payments/catalog/recipients?page=0&size=20` | `ROLE_AGENT` | Lokal katalogdagi faol xizmatlar |
| `GET`  | `/api/payments/catalog/recipients/{id}/fields?method=CHECK` | `ROLE_AGENT` | Xizmatning `CHECK` yoki `PAY` maydonlari |
| `POST` | `/api/payments/check`          | `ROLE_AGENT` | Paynet orqali maydonlarni tekshirish     |
| `POST` | `/api/payments`                | `ROLE_AGENT` | Yangi to'lov yaratish                    |
| `GET`  | `/api/payments?page=0&size=10` | `ROLE_AGENT` | Joriy agent to'lovlarini sahifalab olish |
| `GET`  | `/api/agents/me/balance`       | `ROLE_AGENT` | Joriy agent balansini olish              |
| `GET`  | `/api/agents?page=0&size=20`   | `ROLE_ADMIN` | Agentlarni sahifalab olish                |
| `POST` | `/api/agents/{id}/topup`       | `ROLE_ADMIN` | Agent balansini to'ldirish               |
| `POST` | `/api/admin/paynet-catalog/sync` | `ROLE_ADMIN` | Paynet katalogini qo'lda sinxronlash   |
| `GET`  | `/api/admin/paynet-catalog/latest` | `ROLE_ADMIN` | Oxirgi sinxronizatsiya natijasi      |

Recipient maydonlarini olishda `method` majburiy query parametri bo'lib, uning qiymati
`CHECK` yoki `PAY` bo'lishi kerak.

Paynet katalogi standart holatda har kuni soat `02:00` da (`Asia/Tashkent`) avtomatik sinxronlashtiriladi. Jadvalni `PAYNET_CATALOG_SYNC_CRON` va vaqt zonasini `PAYNET_CATALOG_SYNC_ZONE` orqali o'zgartirish mumkin.

To'lovni tekshirish so'rovi:

```json
{
  "recipientId": 2,
  "fields": {
    "phoneNumber": "+998901234567"
  }
}
```

To'lov yaratish so'rovi:

```json
{
  "recipientId": 2,
  "idempotencyKey": "550e8400-e29b-41d4-a716-446655440000",
  "checkId": "paynet-check-id",
  "fields": {
    "phoneNumber": "+998901234567"
  },
  "account": "+998901234567",
  "amount": 4000000
}
```

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
