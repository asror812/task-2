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

`Key/Value` bo'limida quyidagi qiymatni yarating:

```text
Key:   config/paylite-service/payment.commission-percent
Value: 1.5
```

Yoki terminal orqali:

```bash
curl --request PUT --data '1.5' \
  http://localhost:8500/v1/kv/config/paylite-service/payment.commission-percent
```

`1.5` — komissiya 1.5 foiz ekanini bildiradi.

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

Admin katalogni Paynet'dan qo'lda sinxronizatsiya qilishi va agent balansini to'ldirishi mumkin. Agent esa xizmatni tanlab to'lov qiladi.

## Loyihani to'xtatish

Backend va frontend terminallarida `Ctrl+C` bosing. Keyin asosiy papkada:

```bash
./down.sh
```

