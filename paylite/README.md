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
Consul    -> payment.commission-percent
Liquibase -> sxema migratsiyalari va boshlang'ich Agent yozuvlari
```

Domen faqat ikki ta entity'dan iborat:

- `Agent`: Keycloak foydalanuvchi logini va uning tiyindagi balansi.
- `Payment`: hisob raqami, summa, komissiya, umumiy summa, holat, yaratilgan vaqt va egasi bo'lgan agent.

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
- Mijoz hisob raqami aynan 16 ta raqamdan iborat bo'lishi kerak. To'lov va top-up summalari musbat bo'lishi shart.
- Agent faqat autentifikatsiyadan o'tgan o'z loginiga tegishli to'lovlarni ko'ra oladi.

## Lokal ishga tushirish

### 1. PostgreSQL, Keycloak va Consul'ni ishga tushirish

```bash
docker compose -f src/main/docker/services.yml up -d --wait
```

Servislar quyidagi manzillarda ishlaydi:

| Servis     | Manzil                  |
| ---------- | ----------------------- |
| PostgreSQL | `localhost:5433`        |
| Keycloak   | `http://localhost:9080` |
| Consul     | `http://localhost:8500` |

### 2. Consul'ga komissiya qiymatini qo'shish

Consul UI orqali `config/paylite/payment.commission-percent` kalitini `1.5` qiymati bilan yarating yoki quyidagi buyruqni bajaring:

```bash
curl --request PUT --data '1.5' \
  http://localhost:8500/v1/kv/config/paylite/payment.commission-percent
```

Ilova qiymatni `@ConfigurationProperties(prefix = "payment")` orqali oladi. Qiymatni o'zgartirgandan keyin backend'ni qayta ishga tushiring. `@RefreshScope` orqali restart'siz yangilash ixtiyoriy bonus bo'lib, hozircha alohida yoqilmagan.

### 3. Backend'ni ishga tushirish

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

Keycloak realm konteyner yaratilganda `src/main/docker/realm-config/jhipster-realm.json` faylidan import qilinadi.

Realm'ni qaytadan import qilish uchun:

```bash
docker compose -f src/main/docker/keycloak.yml down
docker compose -f src/main/docker/keycloak.yml up -d
```

## API

| Method | Endpoint                       | Rol          | Tavsif                                   |
| ------ | ------------------------------ | ------------ | ---------------------------------------- |
| `POST` | `/api/payments`                | `ROLE_AGENT` | Yangi to'lov yaratish                    |
| `GET`  | `/api/payments?page=0&size=10` | `ROLE_AGENT` | Joriy agent to'lovlarini sahifalab olish |
| `GET`  | `/api/agents/me/balance`       | `ROLE_AGENT` | Joriy agent balansini olish              |
| `POST` | `/api/agents/{id}/topup`       | `ROLE_ADMIN` | Agent balansini to'ldirish               |

To'lov yaratish so'rovi:

```json
{
  "account": "8600123412341234",
  "amount": 4000000
}
```

Balansni to'ldirish so'rovi:

```json
{
  "amount": 1000000
}
```

## Testlar

Biznes mantiqning asosiy testlarini ishga tushirish:

```bash
./mvnw test
```

Payment service testlari komissiya hisobini, muvaffaqiyatli to'lovda balans yechilishini va balans yetishmaganda to'lov `FAILED` bo'lib, balans o'zgarmasligini tekshiradi.

## Infrastrukturani to'xtatish

```bash
docker compose -f src/main/docker/services.yml down
```
