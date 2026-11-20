# Yol Haritası

Fazlar, ders notlarının *([java-study-notes](https://github.com/software-lecture-labs/java-study-notes), Bölüm 01–14)* sırasını izler: önce iş nesneleri ve kurallar, sonra modül sınırları, servisler, olaylar, kalıcılık, HTTP sınırı ve en son uygulama sunucusu. Değişiklikler doğrudan `main`'e gönderilir; her faz ya da faz adımı kendi commit'indedir *(ADR-0017)*. Her commit'te `./mvnw verify` Docker olmadan geçer.

Hangi konunun hangi fazda işlendiği [CURRICULUM-MAP.md](CURRICULUM-MAP.md) içindedir.

| Faz | Durum | Başlık | Ders bölümleri | Çıktı |
| --- | --- | --- | --- | --- |
| **0** | ✅ | İskelet | 9 | Maven çok modüllü yapı, Maven Wrapper, dokümanlar |
| **1** | ✅ | Domain modeli | 1–5, 7, 8 | Değer nesneleri, varlıklar, sipariş durum makinesi, kargo kuralı, arama ölçütü, repository arayüzleri; 144 birim testi |
| **2** | ✅ | Kullanım senaryoları | 10, 11 | Katalog, sepet, hesap, sipariş verme, sipariş geçmişi, sipariş durum yönetimi ve yorum servisleri; beş port; bellek içi repository'lerle 54 test |
| **3** | ✅ | Kalıcılık | 12 | Flyway şeması, JDBC araç seti, yedi repository, sipariş numarası sayacı, işlem yönetimi; H2 üzerinde 59 test |
| **4** | ✅ | Dersle hizalama | 4–12 | İş alanına göre modüller, ArchUnit, tipli kimlikler, `api` sözleşmesi, olay yayıncısı, PostgreSQL ve sözleşme testleri |
| **5** | ✅ | HTTP sınırı *(elle)* | 13 | JDK HTTP sunucusu, DTO'lar, Bean Validation, RFC 9457 |
| **6** | ✅ | Uygulama sunucusu | 14 | Payara Micro 7 üzerinde WAR: CDI, JTA, CDI olayları, JAX-RS, JSON-B, MicroProfile Config ve OpenAPI |
| **7** | ✅ | Güvenlik ve arayüz | — | Jakarta Security, BCrypt, oturumda misafir sepeti, HTML ve JavaScript sayfaları, iki dilli arayüz |
| **8** | ✅ | Yönetim paneli | — | Kitap/yazar/kategori, kapak görseli, sipariş, kullanıcı ve yorum yönetimi |
| **9** | ✅ | Lab ilerisi ve sağlamlaştırma | 11–14 soruları | CI, Transactional Outbox, ETag, Idempotency-Key, sağlık kontrolü, performans hedefi, JPA |

---

## Faz 4 — Dersle hizalama

Faz 0–3, ders notları görülmeden önce genel bir Java müfredatı varsayılarak yapıldı. Kodun büyük kısmı dersle örtüşüyor; farklar [CURRICULUM-MAP.md](CURRICULUM-MAP.md) içinde 🔁 ile işaretli. Bu faz o farkları kapatır. Her adım ayrı bir commit'tir ve kendinden önceki adımın üzerine kurulur.

| Adım | Konu | İş | ADR |
| --- | --- | --- | --- |
| 4.1 ✅ | Modül yapısı | Modülleri iş alanına göre yeniden kur: `shared-kernel`, `shared-jdbc`, `catalog`, `customer`, `sales`, `review`, `app`, `quality/architecture-tests`. Her modülde `api`, `application`, `domain`, `persistence` paketleri. Spring Boot'u kaldır. `Order.place` ve `Cart.add` için `ProductSnapshot`; stok ayırma `catalog.api` üzerinden. Modül başına migration'lar; modül sınırı ArchUnit kuralları. | 0002, 0003, 0018 |
| 4.2 ✅ | Mimari kurallar | ADR-0003–0008'deki kuralların ArchUnit testleri. Kuralların yakaladığı ihlaller düzeltildi: `Optional` alanlı ve parametreli tipler. | 0003–0008 |
| 4.3 ✅ | Domain modeli | Tipli kimlikler; `DomainException` + `Kind` + `code`; `restore` → `reconstitute`; `shared-kernel`'de `Specification<T>`, `catalog`'da `BookSpecifications`. | 0004 |
| 4.4 ✅ | `api` sözleşmesi | Her modülde `api` paketi: komutlar, DTO'lar, façade arayüzleri; `application` paketinde mapper'lar. `app` modülünde elle yazılmış composition root *(`BookstoreModule`)* ve smoke test. | 0003, 0006 |
| 4.5 ✅ | Domain olayları | `DomainEventPublisher` portu, `InProcessEventPublisher`, bildirim gözlemcisi; dual-write problemini belgeleyen test. | 0007 |
| 4.6 ✅ | PostgreSQL | Referans tablolar, PostgreSQL 17 lehçesi; `docker-compose.yml`; H2 PostgreSQL modu; repository sözleşme testleri *(bellek içi, H2, Testcontainers)*; bellek içi repository'ler kopya saklar. | 0008 |

**Faz 4 sonunda *(tamamlandı)*:** Faz 1–3'ün bütün davranışı ve testleri yeni yapıda çalışır; ArchUnit kuralları geçer; Spring bağımlılığı kalmaz.

## Faz 5 — HTTP sınırı *(elle)*

Ders Bölüm 13'ü izler: JAX-RS'e geçmeden önce bir runtime'ın yaptığı işler bir kez elle yazılır.

- Her iş modülünde `rest` paketi; JDK HTTP sunucusu *(`com.sun.net.httpserver`)* üzerinde yönlendirme.
- İstek `record`'ları ve Bean Validation ile biçimsel doğrulama; tüm alan hataları tek yanıtta.
- `ProblemMapper`: `Kind` → durum kodu, RFC 9457 gövdesi, beklenmeyen hatalar için opak 500.
- Jackson ile JSON; `java.net.http.HttpClient` ile uçtan uca HTTP testleri.
- Uçlar: katalog ve arama, sepet, hesap ve adresler, sipariş verme ve geçmişi, yorumlar, sipariş durum yönetimi.

| Adım | İş |
| --- | --- |
| 5.1 ✅ | `shared-web` modülü *(ADR-0019)*: `HttpApi`, `Routes`, JSON, Bean Validation, `ProblemMapper`, HTTP Basic; ArchUnit kuralları |
| 5.2 ✅ | Katalog ve hesap uçları; `AccountFacade.authenticate` |
| 5.3 ✅ | Sepet, sipariş ve sipariş yönetimi uçları; `cart.changed` problemi düzeltmeleri `adjustments` uzantısıyla taşır |
| 5.4 ✅ | Yorum uçları; çalıştırılabilir sunucu *(`./mvnw -pl app exec:java`)* ve demo verisi; PBKDF2 parola özeti; uçtan uca HTTP testi |

Misafir sepeti Faz 5'te HTTP üzerinden sunulmaz; sepet uçları giriş yapmış müşteri içindir *(ADR-0019)*.

## Faz 6 — Uygulama sunucusu

Ders Bölüm 14'ü izler *(ADR-0010)*.

- `app` modülü WAR olur ve Payara Micro 7 üzerinde çalışır.
- CDI ile bağlama; `BookstoreWiring` yalnızca `Clock`, `DataSource` ve kargo kuralını üretir.
- `@DataSourceDefinition` ve MicroProfile Config; parolalar ortamdan.
- `BookstoreStartup`: açılışta migration ve isteğe bağlı demo verisi.
- `@Transactional` ile JTA transaction'ları; sunucuda `TransactionRunner` portunu `JtaTransactionRunner` uygular.
- CDI olayları ve `AFTER_SUCCESS` gözlemcileri.
- HTTP katmanı JAX-RS, JSON-B ve `ExceptionMapper`'a taşınır; Faz 5'in HTTP testleri JerseyTest ile aynen geçer.
- MicroProfile OpenAPI ile `/openapi`.

| Adım | İş |
| --- | --- |
| 6.1 ✅ | CDI ile bağlama: modüllerde `beans.xml`, `@ApplicationScoped` servis ve repository'ler; `BookstoreWiring`, `BookstoreDataSource`, `JtaTransactionRunner`, `CdiDomainEventPublisher`, `BookstoreStartup`; `ConnectionProvider`; Weld SE ile `CdiWiringTest`; platform ArchUnit kuralları |
| 6.2 ✅ | `app` WAR olur; Payara Micro 7.2026.10 üzerinde açılış, MicroProfile Config ile veri kaynağı, migration ve demo verisi |
| 6.3 ✅ | HTTP katmanı JAX-RS, JSON-B ve `ExceptionMapper`'a taşındı; Faz 5'in JDK sunucusu kalktı; HTTP testleri Jersey'nin JDK container'ında; Payara'da uçtan uca `curl` denemesi; `-Ppayara` profili |
| 6.4 ✅ | MicroProfile OpenAPI `/openapi` adresinde; Payara'da doğrulandı |

`JdbcTransactionRunner` kalkmadı: testler ve elle yazılmış `BookstoreModule` onu kullanmaya devam ediyor; sunucuda aynı portun yerini `JtaTransactionRunner` alıyor *(ADR-0010 notu)*.

## Faz 7 — Güvenlik ve arayüz

Ders kapsamı dışındadır *(ADR-0011)*.

- Jakarta Security: form ile giriş, API için HTTP Basic, `ADMIN` ve `CUSTOMER` rolleri *(ACC-3, ACC-6, ACC-7)*.
- `PasswordHasher` portunun BCrypt uygulaması.
- `app/src/main/webapp` altında HTML, CSS ve JavaScript sayfaları: katalog ve arama, kitap detayı ve yorumlar, sepet, sipariş verme ve geçmişi, hesap ve adresler.
- Türkçe ve İngilizce metin sözlükleri *(NFR-4)*.
- Misafir sepetinin nerede tutulacağı *(sunucu oturumu ya da tarayıcı)* bu fazın ADR'siyle kararlaştırılır.

| Adım | İş |
| --- | --- |
| 7.1 ✅ | `BcryptPasswordHasher` *(eski PBKDF2 özetleri geçerli)*; ADR-0020 |
| 7.2 ✅ | Jakarta Security: `BookstoreAuthenticationMechanism`, `AccountIdentityStore`, `/api/v1/session`, CSRF başlığı |
| 7.3 ✅ | Misafir sepeti oturumda *(`SessionGuestCart`, CDI `@SessionScoped`)*; girişte birleşir |
| 7.4 ✅ | HTML, CSS ve JavaScript sayfaları *(katalog, kitap ve yorumlar, sepet, sipariş, siparişlerim, hesap, giriş, kayıt)*; Türkçe ve İngilizce sözlükler |

## Faz 8 — Yönetim paneli

- `catalog`, `customer` ve `review` modüllerinde yönetim façade'ları *(ADM-1 – ADM-8)*.
- Kapak görseli yükleme için `FileStorage` portu ve dosya sistemi uygulaması *(ADM-4)*.
- Yönetim sayfaları.

| Adım | İş |
| --- | --- |
| 8.1 ✅ | Katalog yönetimi: `CatalogAdminFacade`, `/api/v1/admin/books`, `/admin/authors`, `/admin/categories`, `/admin/books/stats`; ADR-0021 |
| 8.2 ✅ | Kapak görseli: `FileStorage` portu, dosya sistemi uygulaması, yükleme ve sunma |
| 8.3 ✅ | Kullanıcı ve yorum yönetimi: `CustomerAdminFacade`, `ReviewAdminFacade` |
| 8.4 ✅ | Yönetim sayfaları *(özet, kitaplar, yazarlar ve kategoriler, siparişler, kullanıcılar, yorumlar)* |

## Faz 9 — Lab ilerisi ve sağlamlaştırma

Ders notlarının soru bölümlerinde Lab projesine bırakılan konular ve son kontroller:

- GitHub Actions: `./mvnw verify` ve Testcontainers ile PostgreSQL testleri.
- Transactional Outbox: olayların siparişle aynı transaction'da bir tabloya yazılması.
- `ETag` / `If-Match` ile kayıp güncellemenin HTTP seviyesinde önlenmesi.
- Sipariş verme için `Idempotency-Key`.
- Hata yanıtlarına korelasyon kimliği.
- MicroProfile Health readiness kontrolü.
- Performans hedefi *(NFR-9)* ve test kapsamı hedefi *(NFR-6)*.
- JPA ile ikinci bir persistence uygulaması; aynı sözleşme testlerinden geçer *(ADR-0008)*.

Kararlar [ADR-0022](adr/ADR-0022.md)'de.

| Adım | İş |
| --- | --- |
| 9.1 ✅ | GitHub Actions: `verify` ve PostgreSQL testleri *(`.github/workflows/build.yml`)* |
| 9.2 ✅ | Korelasyon kimliği *(`X-Correlation-Id`, problemlerde `correlationId`)*, MicroProfile Health readiness *(`/health/ready`)* |
| 9.3 ✅ | `ETag` / `If-Match` ile kayıp güncelleme koruması *(yönetim API'sinde kitaplar)* |
| 9.4 ✅ | Sipariş için `Idempotency-Key` |
| 9.5 ✅ | Transactional Outbox |
| 9.6 ✅ | Kapsam *(NFR-6, JaCoCo)* ve performans *(NFR-9)* hedefleri |
| 9.7 ✅ | JPA ile ikinci persistence uygulaması: yorum modülü *(`JpaReviewRepository`, `ReviewEntity`; ADR-0023)* |
| 9.8 ✅ | CI'da Payara Micro + PostgreSQL 17 üzerinde uçtan uca duman testi *(`scripts/smoke-test.sh`)*; başlatma betikleri ve kullanma kılavuzu |

---

## Sonraki adımlar

Dokuz faz bitti. Çalışma her kesildiğinde nerede kalındığı burada tutulur; sıradaki iş en üsttedir.

| Durum | İş | Not |
| --- | --- | --- |
| ✅ | Outbox'u birden çok sunucuya hazırlamak: bekleyen olaylar `FOR UPDATE SKIP LOCKED` ile kilitleniyor | ADR-0022 notu; `OutboxRelayRaceContract` |
| ⏳ sıradaki | Arayüzü klavye ve ekran okuyucuyla denetlemek *(odak sırası, etiketler, renk kontrastı)* | NFR-3/NFR-4'ün tamamlayıcısı |
| ⏳ | JPA uygulamasını sunucuda seçilebilir yapmak: JTA veri kaynağı üzerinde kapsayıcının yönettiği `EntityManager` | ADR-0023'ün "sonraki adım"ı |
| 🙋 karar | İlk sürüm etiketi `v2026.11.1` *(ADR-0017)* | Proje sahibinin kararı |
| 🙋 karar | Ortak depoda *(software-lecture-labs)* PR #1'in onaylanıp `main`'e alınması | Depo kuralı bir onay istiyor |
| 🙋 karar | Gerçek ödeme ve e-posta sağlayıcısı | Bugün demo ödeme ve günlüğe yazan bildirim var |

---

## Tamamlanan fazların notları

### Faz 9

- 9.7: Yorum modülü için JPA ile ikinci persistence uygulaması *(ADR-0023)*; aynı sözleşme testlerinden geçiyor. Sözleşmeye eklenen "eski kopya yeniyi ezemez" testini dört uygulama da geçiyor.
- Yedi commit: CI ve ADR-0022 *(ilk CI koşusunun bulduğu bir hatanın düzeltmesiyle)*; korelasyon kimliği ve sağlık kontrolü; `ETag` / `If-Match`; `Idempotency-Key`; Transactional Outbox; kapsam ve performans hedefleri.
- İlk CI koşusu, Docker'lı bir ortamda 38 PostgreSQL testini ilk kez koşturdu; hepsi geçti. Aynı koşu, Linux'ta 2 MB'ı aşan kapak yüklemesinin yanıtının bağlantı sıfırlanınca kaybolduğunu gösterdi; düzeltildi.
- JDK HTTP test sunucusu başlık adlarının harf büyüklüğünü değiştiriyor; korelasyon filtresi başlığı harfe duyarsız arıyor.
- Payara üzerinde `curl` ile denendi: `/health/ready`, problemlerde korelasyon kimliği, `If-Match` ile 412, aynı `Idempotency-Key` ile tek sipariş, outbox üzerinden giden bildirimler ve ortam değişkeniyle ayarlanan aktarım aralığı.
- Kapsam: domain ve application paketlerinin hepsi %90'ın üstünde. Test sayısı: 540 koşan, 40'ı Docker'a bağlı.

### Faz 8

- Dört commit: katalog yönetimi ve ADR-0021; kapak görselleri; kullanıcı ve yorum yönetimi; yönetim sayfaları.
- Sipariş yönetimi Faz 5'ten beri vardı; bu fazda yalnızca sayfası yazıldı.
- Payara üzerinde `curl` ve uygulama içi tarayıcıyla denendi: özet ekranı, stok değişikliği, yazar ve kitap ekleme, kapak yükleme ve ETag ile 304, sipariş durumu, kullanıcıyı kapatınca açık oturumunun da kapanması, yöneticinin kapatılamaması. Bu deneme, girişten sonra tireli sayfa adlarına *(`admin-orders.html`)* dönülmediğini gösterdi; düzeltildi.
- Test sayısı: 512 koşan, 38 Docker'a bağlı.

### Faz 7

- Dört commit: BCrypt ve ADR-0020; Jakarta Security; oturumda misafir sepeti; sayfalar ve sözlükler.
- Payara, Türkçe yerel ayarda çerez ayarlarını okurken açılamıyor *(`dynamic` → `DYNAMİC`)*; sunucu `-Duser.language=en` ile başlatılıyor *(ADR-0020 notu)*. `SameSite` niteliğini Payara yalnızca `glassfish-web.xml`'den okuyor.
- Payara üzerinde `curl` ve uygulama içi tarayıcıyla denendi: Basic ve oturumla giriş, 403 veren CSRF başlığı denetimi, ziyaretçinin sepeti girişte hesaba geçti, sipariş verildi, kayıt sonrası otomatik giriş, sunucu hatasının alanın yanında Türkçe ya da İngilizce gösterilmesi.
- Test sayısı: 476 koşan, 38 Docker'a bağlı.

### Faz 5

- Dört commit: `shared-web` *(ADR-0019)*; katalog ve hesap uçları; sepet ve sipariş uçları; yorum uçları ve çalıştırılabilir sunucu.
- Kimlik Faz 5'te elle yazılmış HTTP Basic ile belirleniyor *(`AccountFacade.authenticate`)*; Faz 7'de Jakarta Security'ye geçecek. Misafir sepeti HTTP üzerinden sunulmuyor.
- Sunucu `curl` ile elle de denendi: katalog araması, HTTP Basic ile profil ve kimliksiz sepet isteğine RFC 9457 401 yanıtı. Bu deneme `Money`'nin `isZero()` gibi metotlarının JSON'a sızdığını gösterdi; düzeltildi ve `JsonTest` ile sabitlendi.
- Test sayısı: 412 koşan, 38 Docker'a bağlı.

### Faz 1–2

- Domain kuralları ve kullanım senaryoları yazıldı. Faz 1'den Faz 2'ye devredilen dört madde *(CRT-5, REV-2/REV-3, ACC-1, ORD-10)* Faz 2'de tamamlandı.
- Bellek içi test çiftleri işlemi geri alamıyordu; bu sınır Faz 3'te kapandı.

### Faz 4.6

- Üç commit: PostgreSQL lehçesi ve referans tablolar; bellek içi repository'lerin kopya saklaması; sözleşme testleri ve Testcontainers.
- Dört modülün `V1` migration'ı PostgreSQL 17 lehçesine yazıldı *(hiçbir ortam uygulamamıştı)*. `CASE` kısıtlarının yerini `languages`, `roles`, `order_statuses`, `payment_methods` referans tabloları aldı. Testler H2'nin PostgreSQL modunda; yerel veritabanı için `docker-compose.yml`.
- Sipariş sayacı PostgreSQL'de hata sonrası iptal olan transaction yüzünden `INSERT … ON CONFLICT DO NOTHING` + `UPDATE` ile çalışıyor; `insert` üretilen `id` sütununu adıyla istiyor.
- `InMemoryStore` kopya saklıyor, her okumada yeni kopya veriyor ve eski sürümle kaydı reddediyor. Bu, kaydetmeden nesne değiştiren üç testi ve yorum repository'sindeki bir kimlik karşılaştırması hatasını ortaya çıkardı.
- Modül başına sözleşme testleri bellek içi, H2 ve PostgreSQL üzerinde aynı testleri koşuyor; uçtan uca `CheckoutWithDatabaseTest` de PostgreSQL varyantına sahip.
- **Doğrulanmadı:** Bu makinede Docker olmadığı için PostgreSQL varyantları *(38 test)* hiç koşmadı; Docker olan bir makinede ya da Faz 9'daki CI'da ilk kez çalışacaklar.
- Test sayısı: 364 koşan, 38 Docker'a bağlı.

### Faz 4.5

- `shared-kernel`'de `DomainEventPublisher` portu ve `InProcessEventPublisher`. `CheckoutService` ve `OrderAdminService` siparişi kaydettikten sonra olayları porta veriyor.
- `Notifier` artık dış dünyaya giden adaptör; `sales.application.OrderNotificationObserver` sipariş olaylarını ona iletiyor. Aboneliği `BookstoreModule` kuruyor.
- Dual-write problemi bir testle belgelendi: bildirim hata verince sipariş kayıtlı kalıyor, bildirim kayboluyor.
- `only_application_publishes_events` ArchUnit kuralı eklendi. Test sayısı 316 → 321.

### Faz 4.4

- Her modülün `api` paketinde façade'lar: `CatalogFacade`, `AccountFacade`, `CartFacade`, `CheckoutFacade`, `OrderFacade`, `OrderAdminFacade`, `ReviewFacade`. Okuma modelleri ve komutlar `api`'ye taşındı; `application` paketlerinde birer mapper var.
- Sözleşme kuralı *(ADR-0003)*: shared-kernel kimlikleri tipli, modüle özel kimlikler `long`, domain enum'ları kod olarak. Misafir sepeti `GuestCart` değeri; oturum mu tarayıcı mı kararı Faz 7'ye açık kaldı.
- `app` modülünde `BookstoreModule`: modülleri elle bağlar, yalnızca façade verir. `BookstoreModuleTest` boş veritabanını migrate edip bir müşterinin bütün yolculuğunu façade'larla yürütür; `app` testleri de artık bu kökü kullanıyor.
- `facades_live_in_api` ArchUnit kuralı eklendi. Geçersiz kimlik değeri artık `ValidationException`. Test sayısı 313 → 316.

### Faz 4.3

- Dört commit: `DomainException` + `Kind` + `code`; `restore` → `reconstitute`; tipli kimlikler; `Specification`.
- `BookstoreException`, `Kind` taşıyan `DomainException` oldu. Alan dışı kural ihlalleri için `RuleViolationException` eklendi; boş sepetle sipariş artık `RULE_VIOLATION`. `DomainExceptionKindTest` türleri ve varsayılan kodları sabitliyor.
- `Entity<I extends Identifier>`. `BookId` ve `CustomerId` `shared-kernel`'de *(başka modüllerin domain'i anıyor)*; `AuthorId`, `CategoryId`, `AddressId`, `OrderId`, `ReviewId` kendi modüllerinde. Repository'ler, servisler, `api` façade'ları ve okuma modelleri tipli kimlik kullanıyor. Misafir sepeti oturumda tutulduğu için kimlikler `Serializable`.
- `BookSearchCriteria` kaldırıldı. Arama `Specification<Book>` ağacı: `BookSpecifications` sekiz kural veriyor, `BookSearchSql` ağacı `switch` desen eşlemeyle SQL'e çeviriyor *(`or` ve `not` dahil)*. Ters fiyat aralığı denetimi `BookSearchQuery`'ye taşındı.
- Test sayısı 299 → 313.

### Faz 4.2

- `quality/architecture-tests` altında ADR başına bir kural sınıfı: `LayerDependencyTest` *(ADR-0003)*, `DomainModelRulesTest` *(ADR-0004)*, `CodingRulesTest` *(ADR-0005)*, `ApplicationRulesTest` *(ADR-0006)*, `DomainEventRulesTest` *(ADR-0007)*, `PersistenceRulesTest` *(ADR-0008)*; `ModuleBoundaryTest` ile 30 kural. Her kural kendini gerektiren ADR'yi `because(...)` içinde anar.
- Kurallar, kasıtlı ihlaller içeren geçici bir sınıfla denendi; on kuralın onu da ihlali yakaladı.
- Yakalanan ihlaller: `BookSearchCriteria`, `BookSearchQuery`, `BookSummary`, `BookDetails` ve `JdbcBookRepository` içindeki satır kaydı `Optional` alan taşıyordu; `Customer.restore` ve `OrderAdminService.orders` `Optional` parametre alıyordu. `OrderAdminService.orders` ikiye ayrıldı *(`orders`, `ordersIn`)*.
- `common` paketi ve eksik `private final` alanı 4.1'de zaten kalkmıştı; kurallar artık bunların geri gelmesini engelliyor.
- `rest` paketleri olmadığı için `rest_sees_only_the_contract` boş geçer; Faz 5'te dolar.

### Faz 4.1

- Altı katman modülü, iş alanına göre sekiz modüle taşındı; Spring Boot iskeleti kaldırıldı. Bütün davranış ve testler korundu (259 → 276 test).
- Modüller birbirini yalnızca `api` paketleriyle kullanıyor: `catalog.api` (`StockFacade`, `RatingFacade`), `customer.api` (`CustomerDirectory`), `sales.api` (`PurchaseHistory`). ArchUnit bunu `ModuleBoundaryTest` ile denetliyor; bilerek eklenen bir ihlalin yakalandığı denendi.
- `sales` artık kitap için kendi `ProductSnapshot` değerini kullanıyor; stok ayırma ve iptalde geri bırakma `catalog` üzerinden, aynı işlem içinde. `app` modülündeki `CheckoutWithDatabaseTest`, reddedilen kartta başka bir modüldeki stoğun da geri alındığını doğruluyor.
- Kitabın puan dağılımı `catalog`'dan `review`'a taşındı (döngüyü önlemek için).
- Tek V1 şema dosyası modül başına dörde bölündü; her modülün Flyway geçmişi ayrı (ADR-0018). Bu, 4.6'daki işin bir kısmını öne aldı.
- `api` paketleri şimdilik yalnızca modüller arası çağrıları taşıyor. Web katmanının kullanacağı komutlar, DTO'lar ve façade'lar 4.4'te gelecek.

### Faz 3

- `CheckoutWithDatabaseTest`, reddedilen ödemede stok, sipariş, sepet ve sipariş numarasının geri alındığını ve son kopyayı aynı anda isteyen iki müşteriden yalnızca birinin aldığını gerçek işlemlerle doğruluyor.
- H2 2.4.240'ın `CHECK (… IN …)` hatası bulundu; ders notlarının ADR-0008'i aynı hatayı belgeliyor.
- Testler yalnızca H2'nin MySQL modunda çalıştı. Faz 4.6'da PostgreSQL'e geçilir.
