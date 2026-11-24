# Book Store 

[![build](https://github.com/omerberatt/book-store/actions/workflows/build.yml/badge.svg)](https://github.com/omerberatt/book-store/actions/workflows/build.yml)

Java ile yazılmış, modüler bir çevrimiçi kitap mağazası.

Projenin amacı: çalışan bir e-ticaret uygulaması ortaya koymak.

> **Durum:** Faz 0–9 tamamlandı: parolalar BCrypt ile özetleniyor, giriş Jakarta Security ile yapılıyor; müşteri sayfaları ve yönetim paneli Türkçe ve İngilizce çalışıyor. Faz 9 sağlamlaştırmayı getirdi: GitHub Actions *(PostgreSQL testleri dahil)*, korelasyon kimliği, `/health/ready`, `ETag` / `If-Match`, sipariş için `Idempotency-Key`, Transactional Outbox, kapsam ve performans hedefleri, yorum modülü için JPA ile ikinci bir persistence uygulaması. Kod iş alanına göre modüllerde *(katalog, müşteri, satış, yorum)*; modüller birbirini yalnızca `api` paketleriyle kullanıyor. Bu kural ve mimari kararların diğer kuralları ArchUnit testleriyle korunuyor. Veritabanı PostgreSQL 17; Payara Micro 7 üzerinde JAX-RS ile bir REST API var *(aşağıda nasıl çalıştırılacağı; tarif `/openapi` adresinde)*. Yönetim paneli `admin.html` adresindedir; demo yöneticiyle girilir. Ayrıntılar [yol haritasında](docs/ROADMAP.md).

## Dokümanlar

| Doküman | İçerik |
| --- | --- |
| [KULLANIM-KILAVUZU.md](docs/KULLANIM-KILAVUZU.md) | Mağazayı başlatmak, müşteri ve yönetici olarak kullanmak |
| [SPEC.md](docs/SPEC.md) | Sistem ne yapar: aktörler, gereksinimler, kapsam dışı kalanlar |
| [ARCHITECTURE.md](docs/ARCHITECTURE.md) | Hedef mimari: modüller, paketler, uygulama sunucusu, veritabanı, test |
| [adr/](docs/adr/README.md) | Mimari karar kayıtları *(ADR-0001 – ADR-0023)* |
| [CURRICULUM-MAP.md](docs/CURRICULUM-MAP.md) | Ders notlarının her bölümü projenin neresinde |
| [ROADMAP.md](docs/ROADMAP.md) | Fazlar ve sıradaki adımlar |

## Hedef mimari

```
shared-kernel                 Money, Address, Entity, hatalar
shared-jdbc                   JDBC araç seti, modül başına migration'lar
shared-web                    HTTP sınırı: yönlendirme, JSON, doğrulama, RFC 9457
catalog | customer | sales | review     iş modülleri; her birinde api, application, domain, persistence, rest
app                           WAR: Payara Micro 7, CDI bağlama, açılış, HTML/JS arayüz
quality/architecture-tests    ArchUnit kuralları
```

İş kuralları domain paketlerinde, yalnızca JDK ile yazılır. Modüller birbirini yalnızca `api` paketleri üzerinden kullanır; bu kurallar ArchUnit testleriyle korunur.

## Teknolojiler

Java 21 · Maven · Jakarta EE 11 · MicroProfile 6.1 · Payara Micro 7 · PostgreSQL 17 · Flyway · JDBC · JUnit · AssertJ · ArchUnit · Testcontainers

*(Testler H2'nin PostgreSQL modunda çalışıyor; yerel PostgreSQL için `docker compose up -d`. Jakarta EE geçişi Faz 6'da.)*

## Gereksinimler

- JDK 21

Maven kurmanız gerekmez; depo Maven Wrapper içerir. Docker zorunlu değildir; varsa repository sözleşme testleri Testcontainers ile gerçek PostgreSQL 17 üzerinde de çalışır, yoksa atlanır.

## Derleme ve test

Git Bash, macOS veya Linux:

```bash
./mvnw verify
```

Windows komut satırı (`cmd`):

```cmd
mvnw.cmd verify
```

## API'yi çalıştırma

Uygulama, Payara Micro 7 üzerinde çalışan bir WAR'dır *([ADR-0010](docs/adr/ADR-0010.md))*. En kolayı, derleyip başlatan betik: Windows'ta `scripts\start.cmd`, Linux ve macOS'ta `./scripts/start.sh`. Mağazanın nasıl kullanılacağı [kullanma kılavuzunda](docs/KULLANIM-KILAVUZU.md).

Adımları elle çalıştırmak için önce bütün modülleri derleyin:

```bash
./mvnw install -DskipTests
```

Sonra sunucuyu başlatın. Maven, Payara Micro'yu indirip WAR'ı onunla açar:

```bash
./mvnw -pl app -Ppayara package exec:exec
```

Mağaza `http://localhost:8080/` adresinde açılır; API `http://localhost:8080/api/v1` adresindedir. Sunucu, Payara'nın Türkçe yerel ayardaki bir hatası yüzünden İngilizce yerel ayarla *(`-Duser.language=en`)* başlatılır; profil bunu kendisi yapar. Veritabanı belirtilmezse bellek içi bir H2 veritabanı kullanılır ve demo verisi yüklenir: 8 kitap, bir müşteri (`ayse@bookstore.local`) ve bir yönetici (`admin@bookstore.local`). İkisinin parolası `kitap2026`. Sunucu durunca veriler silinir.

Örnek istekler:

```bash
curl "http://localhost:8080/api/v1/books?q=madonna"
```

```bash
curl -u ayse@bookstore.local:kitap2026 http://localhost:8080/api/v1/me
```

```bash
curl -u ayse@bookstore.local:kitap2026 -H "Content-Type: application/json" -d '{"bookId": 1, "quantity": 2}' http://localhost:8080/api/v1/cart/items
```

Tarayıcı ise parolayı bir kez `POST /api/v1/session` ile gönderir ve sonra oturum çereziyle çalışır; çerezle yapılan ve veriyi değiştiren istekler `X-Bookstore-Client` başlığını taşımak zorundadır *([ADR-0020](docs/adr/ADR-0020.md))*:

```bash
curl -c cerez.txt -H "X-Bookstore-Client: web" -H "Content-Type: application/json" -d '{"email": "ayse@bookstore.local", "password": "kitap2026"}' http://localhost:8080/api/v1/session
```

Kalıcı bir PostgreSQL için önce `docker compose up -d`, sonra sunucuyu başlatmadan önce şu ortam değişkenleri *(MicroProfile Config)*: `BOOKSTORE_DB_CLASS_NAME=org.postgresql.ds.PGSimpleDataSource`, `BOOKSTORE_DB_URL=jdbc:postgresql://localhost:5432/bookstore`, `BOOKSTORE_DB_USER=bookstore`, `BOOKSTORE_DB_PASSWORD=bookstore`; demo verisi istenmiyorsa `BOOKSTORE_DEMO_DATA=false`. Yüklenen kapak görselleri varsayılan olarak sistemin geçici dizinindeki `bookstore-files` klasörüne yazılır; kalıcı bir yer için `BOOKSTORE_FILES_DIRECTORY`.

Uçların listesi her modülün `rest` paketindeki `…Resource` sınıflarının açıklamasındadır; hatalar RFC 9457 biçiminde döner *([ADR-0009](docs/adr/ADR-0009.md))*.

## Geliştirme akışı

Değişiklikler doğrudan `main` dalına gönderilir *([ADR-0017](docs/adr/ADR-0017.md))*; `main`'e giden her commit'te `./mvnw verify` geçer. Yayınlar `main` üzerinde `v2026.11.1` biçiminde etiketlenir.
