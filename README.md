# PayLite va Paynet loyihasini ishga tushirish

## Kerakli dasturlar

Kompyuterda quyidagilar o'rnatilgan bo'lishi kerak:

- Docker va Docker Compose
- Java 17
- Maven
- Node.js va npm

## 1. Docker network yaratish

Bu buyruqni faqat birinchi marta bajarish kerak:

```bash
docker network create general
```

Agar `general already exists` xabari chiqsa, bu xato emas.

## 2. Konteynerlarni ishga tushirish

Loyihaning asosiy papkasida:

```bash
./start.sh
```

`start.sh` Consul'ni aniq `development` rejimida ishga tushiradi va uning
host portlarini faqat `127.0.0.1` manziliga ochadi. `consul.yml` boshqa
muhitda to'g'ridan-to'g'ri ishlatilsa, muhit xavfsiz tarzda `production`
deb qabul qilinadi. Bunday rejimda `CONSUL_CLIENT_BIND=0.0.0.0` berilsa,
Consul ishga tushmaydi.

Real muhitda bind qiymatlarini aniq ko'rsating:

```bash
CONSUL_DEPLOY_ENV=production \
CONSUL_CLIENT_BIND='<private-consul-interface-ip>' \
CONSUL_HOST_BIND='<private-host-interface-ip>' \
docker compose -f docker-compose.yml up -d
```

`CONSUL_CLIENT_BIND=0.0.0.0` va `CONSUL_HOST_BIND=0.0.0.0` qiymatlarini
real muhitda ishlatmang.

`start.sh` quyidagi konteynerlarni ishga tushiradi:

- Consul
- Keycloak
- PayLite PostgreSQL
- Paynet PostgreSQL
- Redis

Konteynerlar holatini tekshirish:

```bash
docker ps
```

Keycloak to'liq ishga tushishi uchun biroz kutish kerak bo'lishi mumkin.

## 3. Consul'ga komissiya qiymatini kiritish

PayLite backend'ni ishga tushirishdan oldin Consul'ga komissiya foizini kiritish kerak.

Brauzerda Consul'ni oching:

```text
http://localhost:8500
```

`Key/Value` bo'limida quyidagi qiymatlarni yarating:

```text
Key:   config/paylite-service/payment.commission-percent
Value: 1.5

Key:   config/paylite-service/payment.min-amount
Value: 100

Key:   config/paylite-service/payment.max-amount
Value: 100000000
```

Yoki terminal orqali:

```bash
curl --request PUT --data '1.5' \
  http://localhost:8500/v1/kv/config/paylite-service/payment.commission-percent

curl --request PUT --data '100' \
  http://localhost:8500/v1/kv/config/paylite-service/payment.min-amount

curl --request PUT --data '100000000' \
  http://localhost:8500/v1/kv/config/paylite-service/payment.max-amount
```

`1.5` — komissiya 1.5 foiz ekanini bildiradi. Minimal va maksimal summa tiyinda beriladi.

## 4. Paynet backend'ni ishga tushirish

Yangi terminal oching:

```bash
cd paynet
mvn
```

Paynet quyidagi manzilda ishlaydi:

```text
http://localhost:8000
```

## 5. PayLite backend'ni ishga tushirish

Yana bir terminal oching:

```bash
cd paylite
export PAYNET_CLIENT_SECRET='<Keycloak paylite-service client secret>'
./mvnw
```

PayLite backend quyidagi manzilda ishlaydi:

```text
http://localhost:8080
```

Paynet va PayLite ishga tushganda Consul'ga avtomatik ro'yxatdan o'tadi. Ularni Consul UI'dagi `Services` bo'limida ko'rish mumkin:

```text
http://localhost:8500/ui/dc1/services
```

`paynet` va `paylite-service` servislarining holati yashil bo'lishi kerak.

## 6. PayLite frontend'ni ishga tushirish

Yana bir terminal oching:

```bash
cd paylite
./npmw install
./npmw start
```

`./npmw install` buyrug'ini odatda faqat birinchi marta yoki `package.json` o'zgarganda bajarish kifoya.

Frontend manzili:

```text
http://localhost:9000
```

## Ishga tushirish tartibi

Qisqacha tartib:

```text
1. ./start.sh
2. Consul'ga commission-percent qiymatini kiritish
3. Paynet backend
4. PayLite backend
5. PayLite frontend
```

## Test foydalanuvchilari

| Login   | Parol   | Rol          |
|---------|---------|--------------|
| `admin` | `admin` | `ROLE_ADMIN` |
| `agent` | `agent` | `ROLE_AGENT` |

Admin katalogni Paynet'dan qo'lda sinxronizatsiya qilishi va agent balansini idempotency key bilan to'ldirishi mumkin. Agent lokal katalogdan xizmatni tanlaydi, uning maydonlarini to'ldiradi, Paynet orqali tekshiradi va UUID idempotency key bilan to'lov qiladi.

## Loyihani to'xtatish

Backend va frontend terminallarida `Ctrl+C` bosing. Keyin asosiy papkada:

```bash
./down.sh
```
