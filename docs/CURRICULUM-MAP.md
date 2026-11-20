# Müfredat Eşlemesi

Projenin izlediği müfredat, **Java Patterns and Practices** ders notlarıdır: [software-lecture-labs/java-study-notes](https://github.com/software-lecture-labs/java-study-notes), Bölüm 01–14. Bu doküman, her bölümde anlatılan konunun kitap mağazasında **nerede** ve **hangi durumda** olduğunu gösterir.

Ders notları, konuları Northwind veritabanı üzerinde bir `sales` modülüyle anlatıyor ve bunların "Lab projesi"nde uygulanacağını söylüyor. Kitap mağazası bu Lab projesinin yerine geçer: aynı kavramlar katalog, müşteri, satış ve yorum modüllerinde uygulanır.

**Durum işaretleri**

| İşaret | Anlamı |
| --- | --- |
| ✅ | Uygulandı ve testi var. |
| 🔁 | Uygulandı ama ders notlarındaki biçimden farklı; belirtilen fazda dersle uyumlu hale getirilecek. |
| ⏳ | Henüz yok; belirtilen fazda yapılacak. |
| — | Projede kod gerektirmeyen ya da bilinçli olarak uygulanmayan konu. |

Faz numaraları [ROADMAP.md](ROADMAP.md) içindeki aşamalardır; ADR'ler [docs/adr](adr/README.md) klasöründedir.

---

## Bölüm 01 — Gitflow ve İlk İş Nesnesi

| Konu | Kitap mağazasında | Durum | Faz |
| --- | --- | --- | --- |
| Gitflow: `main`, `develop`, `feature/*`, `release/*`, `hotfix/*` | Bilinçli olarak uygulanmıyor; proje doğrudan `main` üzerinde ilerliyor *([ADR-0017](adr/ADR-0017.md); Gitflow'u kabul eden [ADR-0001](adr/ADR-0001.md) uygulanmadan değiştirildi)* | — | — |
| Sürüm etiketleri, CalVer | Yayın yapıldığında `main` üzerinde `v2026.11.1` biçimi *(ADR-0017)* | ⏳ | yayında |
| POJO'dan değişmez *(immutable)* iş nesnesine | `Money`, `Isbn`, `Email`, `PersonName`, `Address`, `Stock`, `Rating` (`record`) | ✅ | 1 |
| Kurucuda doğrulama, `final` sınıf | Değer nesnelerinin compact constructor'ları; `shared-kernel` `Guard` | ✅ | 1 |
| Boş `catch` ile hatayı yutmamak | Exception'lar sarılıp yeniden fırlatılıyor *(`DataAccessException`)* | ✅ | 3 |
| Notification pattern — tüm hataları birden bildirmek | `ValidationException` birden çok `Violation` taşır; `PasswordPolicy` tüm kuralları birlikte raporlar | ✅ | 1 |

## Bölüm 02 — Git Pratikleri, Anemik Model ve Record'lar

| Konu | Kitap mağazasında | Durum | Faz |
| --- | --- | --- | --- |
| Feature dalı, `--no-ff` merge, Pull Request | Bilinçli olarak uygulanmıyor *(ADR-0017)* | — | — |
| Anemik model ve sorunları | Domain baştan zengin tasarlandı; anemik karşı örnek tutulmuyor | — | — |
| Value object, `equals` / `hashCode` | `record`'lar değere göre; `Entity` kimliğe göre eşit *(`EntityTest`)* | ✅ | 1 |
| Class ile record karşılaştırması | Değer nesneleri `record`; kimliği olan varlıklar sınıf | ✅ | 1 |
| Record'ların sığ değişmezliği, `List.copyOf` | `Page`, `CartView`, `OrderDetails`, `BookDetails` kurucuları listeleri kopyalar | ✅ | 1, 2 |
| `invokedynamic`, bytecode | Projede kod gerektirmez | — | — |

## Bölüm 03 — Money Value Object ve Rich Entity Tasarımı

| Konu | Kitap mağazasında | Durum | Faz |
| --- | --- | --- | --- |
| `double` / `float` yuvarlama hataları | `MoneyTest.addsDecimalFractionsExactly` | ✅ | 1 |
| `Money` record: `BigDecimal`, para birimi, `setScale`, `HALF_UP` | `shared-kernel` `Money` | ✅ | 1 |
| Farklı para birimleriyle işlem yasağı | `Money.requireSameCurrency` | ✅ | 1 |
| Zengin entity: `Order`, `OrderLine`, `OrderStatus` | `sales` `Order.place`, `markPaid`, `ship`, `deliver`, `cancel` | ✅ | 1 |
| Sipariş anındaki fiyatın kopyalanması | `OrderLine` başlık ve birim fiyatı kopyalar | ✅ | 1 |
| Value object mi entity mi? | `Entity` taban sınıfı ile `record` değer nesneleri ayrımı | ✅ | 1 |

## Bölüm 04 — Aggregate Sınırları, State Machine ve Domain Exception'lar

| Konu | Kitap mağazasında | Durum | Faz |
| --- | --- | --- | --- |
| Niyet bildiren metotlar *(setter yerine)* | `Customer.rename`, `changeEmail`; `Book.reserve`, `changePrice` | ✅ | 1 |
| Aggregate kökü ve parçaları | `Order` → `OrderLine`, `Customer` → `SavedAddress`, `Cart` → `CartItem` | ✅ | 1 |
| Aggregate'ler arası kimlikle referans | `Order` ve `Review` müşteriye kimlikle bağlı; `Cart.add` ve `Order.place` artık `catalog`'un `Book` aggregate'ini değil, `sales`'in kendi `ProductSnapshot` değerini alıyor *(ADR-0002)*. Kimlikler henüz `long`. | ✅ | 4.1 |
| `OrderStatus` state machine, `EnumSet`, final durumlar | `OrderStatus.allowedNext`, `canMoveTo`, `isFinal` | ✅ | 1 |
| Domain exception hiyerarşisi | `shared-kernel`: `DomainException` + `Kind` + `code`; `NotFoundException`, `ConflictException`, `RuleViolationException`, `ValidationException`, `AccessDeniedException` *(ADR-0004)* | ✅ | 4.3 |
| JUnit ve AssertJ ile birim testler | Tüm modüllerde; parametreli testler *(`@CsvSource`, `@EnumSource`)* | ✅ | 1 |
| "Turkish I problem", `Locale.ROOT` | `Email`, `Isbn`, `TextNormalizer` | ✅ | 1 |

## Bölüm 05 — Optional, Stream API ve Sorgu Tasarımı

| Konu | Kitap mağazasında | Durum | Faz |
| --- | --- | --- | --- |
| `Optional` dönüş tipi sözleşmesi | `findBy…` metotları, `Book.publicationYear()`, `Customer.defaultAddress()` | ✅ | 1 |
| `Optional` alan ya da parametre olarak kullanılmaz | `BookSearchQuery`, `BookSummary`, `BookDetails` boş olabilen bileşenli record'lar; `optional_is_not_a_field_type`, `optional_is_not_a_parameter_type` *(ADR-0004)* | ✅ | 4.2 |
| Stream API, `reduce(Money::plus)` | `Money.sum`, `Cart.subtotal`, `Order` toplamları | ✅ | 1 |
| `Collectors.groupingBy`, `toMap`, `mapping` | `RatingDistribution`, repository'lerde kimliğe göre gruplama | ✅ | 1, 3 |
| Bellek içi sorgu nesnesi *(OrderBook)* | Test çiftleri: `InMemoryBookRepository` … `InMemoryStore<T>` | ✅ | 2 |
| Big-O: liste yerine `Map` indeksi | `InMemoryStore` kimliğe, ISBN'e ve e-postaya göre `Map` | ✅ | 2 |
| Deterministik sıralama | `BookSort` eşitlikte başlık ve ISBN'e göre sıralar | ✅ | 1 |

## Bölüm 06 — Yazılım Mimarileri

| Konu | Kitap mağazasında | Durum | Faz |
| --- | --- | --- | --- |
| Coupling, cohesion, test edilebilirlik, bilişsel yük | ADR-0002'nin gerekçeleri | ✅ | — |
| Katmanlı, hexagonal *(ports & adapters)*, onion/clean | Her iş modülünün içi hexagonal: `domain` merkezde; repository'ler ve portlar domain'de arayüz; `persistence` (ve Faz 5'te `rest`) adaptör | ✅ | 4.1 |
| Modüler monolit | İş alanına göre Maven modülleri; modüller birbirini yalnızca `api` paketleriyle kullanıyor *(ADR-0002, ADR-0003)* | ✅ | 4.1 |
| Mikroservis, SOA | Bilinçli olarak seçilmedi *(ADR-0002)* | — | — |

## Bölüm 07 — Typed Identity, Static Factory, Factory ve Builder

| Konu | Kitap mağazasında | Durum | Faz |
| --- | --- | --- | --- |
| Tipli kimlikler *(`CustomerId`, `OrderId`)* | `shared-kernel`: `Identifier`, `BookId`, `CustomerId`; modüllerde `AuthorId`, `CategoryId`, `AddressId`, `OrderId`, `ReviewId`; `Entity<I extends Identifier>` *(ADR-0004)* | ✅ | 4.3 |
| Static factory metotları | `Money.of`, `Customer.register`, `Cart.guest`, `OrderNumber.of`, `Rating.of` | ✅ | 1 |
| Factory ve kimlik üreteci arayüzü | `Order.place` + `OrderNumberSequence` portu *(bellek içi ve JDBC uygulamaları)* | ✅ | 1, 3 |
| Builder | `Book.Builder` *(4.3'e kadar `BookSearchCriteria.Builder` de vardı; yerini `Specification` zinciri aldı)* | ✅ | 1 |

## Bölüm 08 — Strategy, Specification, Repository ve Mapper

| Konu | Kitap mağazasında | Durum | Faz |
| --- | --- | --- | --- |
| Strategy | `ShippingPolicy` / `FreeShippingThreshold`, `BookSortField` | ✅ | 1 |
| Strategy için lambda | `ShippingPolicy.alwaysFree()`, testte `subtotal -> …` | ✅ | 1 |
| Specification *(kuralı veri haline getirmek)* | `shared-kernel`: `Specification<T>` *(`and` / `or` / `not`, `all()`)*, `AndSpecification`, `OrSpecification`, `NotSpecification`, `MatchAll`; `catalog.domain.BookSpecifications`: `active`, `inStock`, `inCategories`, `byAuthor`, `pricedFrom`, `pricedUpTo`, `inLanguage`, `matchingText`, `stockBelow` *(8.1, stoğu azalan kitaplar)* | ✅ | 4.3, 8.1 |
| Specification'ı SQL'e çevirmek | `BookSearchSql`, `Specification` ağacını desen eşleme *(`switch`)* ile `WHERE` koşuluna çevirir; `and` / `or` / `not` dahil. `JdbcCatalogRepositoryTest` SQL ile bellek içi sonucu her kural ve sıralama için karşılaştırır *(ders bunu "bir sonraki adım" olarak bırakıyor)* | ✅ | 3, 4.3 |
| Repository deseni, arayüzün yeri | Arayüzler domain'de *(ders ADR-0003'ün risk notu baştan önlendi)* | ✅ | 1 |
| Mapper *(domain ↔ DTO)* | Modül başına mapper: `CatalogMapper`, `CustomerMapper`, `SalesMapper`, `ReviewMapper` *(application paketinde, paket içi)*; modüller arası `sales.application.Products` *(`BookOffer` → `ProductSnapshot`)*. Domain nesnesi modülden çıkmaz | ✅ | 4.1, 4.4 |
| Paket düzeni: `api`, `application`, `domain`, `persistence` | Her iş modülünde; `rest` Faz 5'te *(ADR-0002)* | ✅ | 4.1 |

## Bölüm 09 — Maven Modülleri ve ArchUnit

| Konu | Kitap mağazasında | Durum | Faz |
| --- | --- | --- | --- |
| Çok modüllü Maven, `pom` / `jar` paketleme | Kök `pom` ve sekiz `jar` modülü; sürümler kök POM'da sabit *(Spring BOM kaldırıldı)* | ✅ | 4.1 |
| İş alanına göre modüller *(package-by-feature)* | `shared-kernel`, `shared-jdbc`, `catalog`, `customer`, `sales`, `review`, `app`, `quality/architecture-tests` *(ADR-0002, ADR-0018)* | ✅ | 4.1 |
| `shared-kernel` | `Money`, `Address`, `Entity`, `DomainEvent`, `TransactionRunner`, sayfalama, metin ve hatalar; hiçbir iş modülüne bağımlı değil *(ArchUnit ile korunuyor)* | ✅ | 4.1 |
| Test fixture'ını `test-jar` ile paylaşmak | `shared-kernel` *(`InMemoryStore`, `DirectTransactions`, `TestClock`)*, `shared-jdbc` *(`TestDatabase`)*, `catalog` *(`CatalogDb`)*, `customer` *(`CustomerDb`)*, `sales` *(`SalesDb`)* | ✅ | 4.1 |
| Uçtan uca smoke test | `app` modülünde dört modülü gerçek JDBC ile kuran `JdbcBookstore`; `CheckoutWithDatabaseTest`, `ReviewFlowTest` | ✅ | 4.1 |
| ArchUnit ile mimari testler | `quality/architecture-tests`: modül sınırı *(`ModuleBoundaryTest`)*, paket matrisi *(`LayerDependencyTest`)*, domain *(`DomainModelRulesTest`)*, kodlama *(`CodingRulesTest`)*, application *(`ApplicationRulesTest`)*, olay *(`DomainEventRulesTest`)* ve persistence *(`PersistenceRulesTest`)* kuralları; kasıtlı ihlallerle doğrulandı | ✅ | 4.1, 4.2 |
| `no_standard_streams`, `no_dumping_ground_packages` | [ADR-0005](adr/ADR-0005.md): `CodingRulesTest` | ✅ | 4.2 |

## Bölüm 10 — Uygulama Servisleri ve Manuel Dependency Injection

| Konu | Kitap mağazasında | Durum | Faz |
| --- | --- | --- | --- |
| Kullanım senaryosu servisleri | `CatalogService`, `CartService`, `AccountService`, `CheckoutService`, `OrderService`, `OrderAdminService`, `ReviewService` | ✅ | 2 |
| Komut nesneleri *(commands)* | `RegistrationCommand`, `PlaceOrderCommand`, `ReviewCommand` | ✅ | 2 |
| `api` paketi ve façade arayüzleri | `CatalogFacade`, `AccountFacade`, `CartFacade`, `CheckoutFacade`, `OrderFacade`, `OrderAdminFacade`, `ReviewFacade` ve modüller arası `StockFacade`, `RatingFacade`, `CustomerDirectory`, `PurchaseHistory`; komutlar ve görünümler *(`BookSearchQuery`, `CustomerView`, `CartView`, `GuestCart`, `OrderDetails`, `ReviewView` …)* `api` paketinde. Kural: shared-kernel kimlikleri tipli, modüle özel kimlikler `long`, domain enum'ları kod olarak *(ADR-0003)*; `facades_live_in_api` | ✅ | 4.4 |
| Constructor injection, `private final` işbirlikçiler | Tüm servisler | ✅ | 2 |
| `Clock` enjeksiyonu, `SteppingClock` | `Clock.fixed` ve `Clock.offset` ile testler | ✅ | 1, 2 |
| Composition root | `app` modülünde `BookstoreModule`: bütün modülleri JDBC repository'leriyle elle bağlar, dışarıya yalnızca façade'ları verir; `BookstoreModule.Adapters` saat, ödeme, parola ve bildirim adaptörlerini alır. `BookstoreModuleTest` boş veritabanını migrate edip bir müşterinin bütün yolculuğunu façade'larla yürütür | ✅ | 4.4 |
| Fake, stub, mock | Modül testlerinde sahte façade'lar *(`FakeCatalog`, `FakeCustomers`, `FakeRatings` …)* ve sahte portlar | ✅ | 2, 4.1 |

## Bölüm 11 — Domain Events ve Observer Deseni

| Konu | Kitap mağazasında | Durum | Faz |
| --- | --- | --- | --- |
| Olaylar `sealed` arayüz + `record` | `shared-kernel` `DomainEvent`; `sales` modülünün `sealed` `OrderEvent` arayüzü: `OrderPlaced`, `OrderStatusChanged` | ✅ | 4.1 |
| Aggregate olayı biriktirir, servis toplar *(`pullEvents`)* | `Entity.raise` / `pullEvents` | ✅ | 1 |
| Önce kayıt, sonra yayın | `CheckoutService`, `OrderAdminService` | ✅ | 2 |
| Yayıncı portu ve gözlemciler | `shared-kernel`: `DomainEventPublisher` portu ve `InProcessEventPublisher` *(aynı thread, abonelik sırası, alt türler dahil)*; `sales.application.OrderNotificationObserver` sipariş olaylarını `Notifier` adaptörüne verir; `BookstoreModule` aboneliği kurar *(ADR-0007)* | ✅ | 4.5 |
| Dual-write problemi | `CheckoutWithDatabaseTest.aFailingNotificationLeavesTheStoredOrderInPlaceAndIsLost`: bildirim hata verince sipariş kayıtlı kalır, bildirim kaybolur *(çözümü Faz 9'daki Transactional Outbox)* | ✅ | 4.5 |
| `switch` ile tür eşleme *(exhaustiveness)* | `OrderTest.target`, `CartService.load`, `CheckoutService` ödeme sonucu | ✅ | 1, 2 |

## Bölüm 12 — Persistence Boundary, JDBC ve Flyway

| Konu | Kitap mağazasında | Durum | Faz |
| --- | --- | --- | --- |
| Flyway ile şema yönetimi | Modül başına `V1__<modül>.sql`; `ModuleMigrations` *(ADR-0018)* | ✅ | 3, 4.1 |
| Migration'ların sahibi olan modül | Her modül `db/migration/<modül>` altında kendi dosyalarına ve `flyway_history_<modül>` tablosuna sahip *(ADR-0018)* | ✅ | 4.1 |
| JDBC repository, aggregate = depolama birimi | `JdbcBookRepository`, `JdbcOrderRepository` … | ✅ | 3 |
| Alt satırları silip yeniden yazmak | `cart_items`, `book_authors` | ✅ | 3 |
| Veritabanından yeniden oluşturma *(reconstitute)* | `Author`, `Category`, `Customer`, `SavedAddress`, `Order`, `Cart`, `Review` için `reconstitute…`; `Book.builder().persisted(...)`; yalnızca persistence çağırır *(`only_persistence_reconstitutes`)* | ✅ | 4.3 |
| N+1 sorgu problemi | Bir sayfa kitap üç sorguyla yüklenir | ✅ | 3 |
| Specification sorgularının bedeli | `BookSearchSql` tüm filtreyi SQL'de yapar | ✅ | 3 |
| `BigDecimal` ölçek problemi | `Money` her zaman 2 basamağa yuvarlar | ✅ | 1 |
| `CHECK (... IN ...)` ve H2 2.4.240 hatası | Faz 3'te bulundu, `CASE` ile aşıldı; Faz 4.6'da `languages`, `roles`, `order_statuses`, `payment_methods` referans tablolarıyla değiştirildi *(ADR-0008)* | ✅ | 4.6 |
| `SQLException`'ı sarmalamak | `DataAccessException`; kısıt ihlalleri `ConflictException`'a çevrilir | ✅ | 3 |
| Sipariş numarası veritabanından | `order_number_sequences` + `JdbcOrderNumberSequence` | ✅ | 3 |
| Transaction sınırı kullanım senaryosunda | `TransactionRunner` portu + `JdbcTransactionRunner` *(`ThreadLocal` ile bağlantı)* | ✅ | 2, 3 |
| Sözleşme testleri *(contract tests)* | Modül başına soyut `…RepositoryContract`; `InMemory…Test`, `H2…Test` ve `Postgres…Test` alt sınıfları aynı testleri koşar. Ham SQL kısıtları `…SchemaConstraintsTest` *(H2 ve PostgreSQL)* | ✅ | 4.6 |
| Bellek içi repository'nin kopya saklaması | `InMemoryStore` kopya saklar, her okumada yeni kopya verir ve eski sürümle kaydı `concurrent-modification` ile reddeder; kategori ağacı satır olarak tutulup her okumada yeniden kurulur. Paylaşılan referansa güvenen üç test ve bir kimlik karşılaştırması hatası düzeltildi | ✅ | 4.6 |
| PostgreSQL ve Testcontainers | Migration'lar PostgreSQL 17 lehçesinde; hızlı testler H2 PostgreSQL modunda; `PostgresTestDatabase` *(Testcontainers 2.0.5, `postgres:17`)* her teste yeni bir veritabanı verir; `@EnabledIfDocker` Docker yoksa atlar; yerel veritabanı `docker-compose.yml` *(ADR-0008)* | ✅ | 4.6 |
| Atomik kayıt, geri alma | `app` modülünde `CheckoutWithDatabaseTest.declinedPaymentRollsBack…`: başka bir modüldeki stok da geri alınıyor | ✅ | 3, 4.1 |
| JPA'nın neleri ortadan kaldırdığı | `JpaReviewRepository` ve ayrı persistence modeli `ReviewEntity`; `ReviewRepositoryContract`'tan H2 ve PostgreSQL'de geçer *(ADR-0023)* | ✅ | 9.7 |

## Bölüm 13 — HTTP Boundary, DTO, Validasyon ve Hata Sözleşmeleri

| Konu | Kitap mağazasında | Durum | Faz |
| --- | --- | --- | --- |
| `rest` paketi, façade üzerinden erişim | `CatalogResource`, `AccountResource`, `CartResource`, `OrderResource`, `OrderAdminResource`, `ReviewResource`; ortak işler `shared-web`'de *(ADR-0009, ADR-0019)* | ✅ | 5 |
| Request DTO ve Bean Validation *(biçimsel doğrulama)* | `AccountRequests`, `SalesRequests`, `ReviewResource.ReviewRequest`; `RequestValidator` bütün alan hatalarını tek 400 yanıtında döner | ✅ | 5 |
| Doğrulamanın yeri: sınır mı domain mi? | Biçim sınırda *(400)*, kural domain'de *(422)*; örneğin zayıf parola 422, boş ad 400 *(`AccountResourceTest`)* | ✅ | 5 |
| `DomainException.Kind` → durum kodu | `ProblemMapper.statusOf`: 404 / 409 / 422 / 403; biçim hatası 400 | ✅ | 4.3, 5 |
| RFC 9457 Problem Details | `Problem`, `ProblemMapper`, `application/problem+json`; uzantı alanları *(`cart.changed` için `adjustments`)*; opak 500 | ✅ | 5 |
| JSON dönüşümleri | Faz 5'te Jackson; Faz 6'da JSON-B *(Yasson; `JsonbConfiguration`: kimlikler sayı, `Money` tutar ve para birimi, bilinmeyen alan hata)* | ✅ | 5, 6.3 |
| JDK HTTP sunucusu *(`com.sun.net.httpserver`)* | `HttpApi`: istek başına sanal thread, `Routes` ile yol şablonları *(404 / 405)*, HTTP Basic; Faz 6.3'te JAX-RS'e devredildi | ✅ | 5 |
| HTTP testleri | `HttpTestClient` *(`java.net.http.HttpClient`)*; modül başına `…ResourceTest` ve uçtan uca `BookstoreHttpTest` | ✅ | 5 |

## Bölüm 14 — Uygulama Sunucusu: CDI, JTA, JAX-RS ve Payara Micro

| Konu | Kitap mağazasında | Durum | Faz |
| --- | --- | --- | --- |
| WAR paketleme, Payara Micro 7 | `app` modülü → `bookstore.war` *(sınıflar ayrıca `bookstore-classes.jar`)*; Payara'nın sağladığı Bean Validation WAR'dan çıkarıldı; Payara Micro 7.2026.10 *(MicroProfile 7.1)* üzerinde açıldı | ✅ | 6.2 |
| CDI: `beans.xml`, `annotated` keşif, kapsamlar, proxy | Her modülde `META-INF/beans.xml` *(annotated)*; servisler ve repository'ler `@ApplicationScoped`, `@Inject` kurucu ve proxy için korumalı parametresiz kurucu; `CdiWiringTest` *(Weld SE)* | ✅ | 6.1 |
| Producer'lar *(container'ın kuramadığı nesneler)* | `BookstoreWiring`: `Clock`, kargo kuralı, `JdbcExecutor`, parola, ödeme, bildirim; `BookstoreDataSource`: havuz ve `ConnectionProvider`; `@Typed` ile producer tiplerini sınırlamak *(`CdiWiringTest`)* | ✅ | 6.1 |
| `@DataSourceDefinition` ve MicroProfile Config | `BookstoreDataSource` *(`${MPCONFIG=...}`)*; varsayılanlar `microprofile-config.properties`'te, parola ortamdan; Payara'da doğrulandı | ✅ | 6.1, 6.2 |
| Açılışta migration ve demo verisi | `BookstoreStartup` *(CDI `Startup` olayı, `bookstore.demo-data`)*; Payara'da 4 migration ve demo verisi | ✅ | 6.1, 6.2 |
| JTA `@Transactional`, repository'ler commit etmez | `JtaTransactionRunner`: `TransactionRunner` portunun `@Transactional` implementasyonu *(ADR-0010 notu)*; repository'ler havuzdan JTA'ya katılan bağlantı alır; Payara'da sipariş akışıyla denendi *(geri alma yolu yalnızca H2 testlerinde)* | ✅ | 6.1, 6.3 |
| CDI olayları, `@Observes(during = AFTER_SUCCESS)` | `CdiDomainEventPublisher`, `OrderNotificationObserver.onOrderEvent`; `domain_event_observers_wait_for_commit` | ✅ | 6.1 |
| JAX-RS kaynakları, JSON-B, `ExceptionMapper` | Modül başına `…Resource` *(`@Path`, CDI ile yönetilen)*, `BookstoreApplication` *(`/api/v1`)*, `ProblemExceptionMappers`, `JsonbConfiguration`, `BasicAuthenticationFilter`; Payara'da `curl` ile denendi | ✅ | 6.3 |
| JerseyTest | Faz 5'in HTTP testleri aynı beklentilerle Jersey'nin JDK HTTP container'ında *(`RestTestServer`, JerseyTest'in kullandığı runtime)* | ✅ | 6.3 |
| MicroProfile OpenAPI | `/openapi` *(API-4)*: `BookstoreApplication` üzerinde `@OpenAPIDefinition` ve HTTP Basic şeması; modül jar'ları `mp.openapi.extensions.scan.lib=true` ile taranır; Payara'da 21 uç listelendi | ✅ | 6.4 |

---

## Ders notlarının soru bölümlerinden alınanlar

Ders, bazı konuları sorularda bırakıp Lab projesine erteliyor. Kitap mağazasında bunlardan uygulananlar ve planlananlar:

| Soru *(bölüm)* | Kitap mağazasında | Durum | Faz |
| --- | --- | --- | --- |
| İyimser kilitleme, `version` kolonu *(12, 14)* | Tüm aggregate tabloları; [ADR-0015](adr/ADR-0015.md) | ✅ | 3 |
| Aynı anda son kopyayı alan iki istemci *(14)* | `CheckoutWithDatabaseTest.twoCustomersRacingForTheLastCopyCannotBothGetIt` | ✅ | 3 |
| Sipariş numarasında boşluk ve geri alma *(12)* | Geri alınan numara yeniden verilir; testle belgelendi | ✅ | 3 |
| Transactional Outbox *(11, 12, 14)* | `OrderEventOutbox` *(`JdbcOrderEventOutbox`, `order_outbox`, sales V3)*, `OrderEventRelay`, `OutboxRelayTimer` *(Jakarta Concurrency)* | ✅ | 9.5 |
| `ETag` / `If-Match` *(13)* | `EntityTags` *(shared-web)*, `AdminBookResource`; `CatalogAdminFacade` beklenen sürümü alır; yönetim sayfaları `If-Match` gönderir | ✅ | 9.3 |
| `Idempotency-Key` *(13)* | `OrderRequest`, `OrderRequestLog` *(`JdbcOrderRequestLog`, `order_requests` tablosu, sales V2)*; `CheckoutService` anahtarı ödemeden önce kaydeder; ödeme sayfası her açılışta anahtar üretir | ✅ | 9.4 |
| Hata yanıtına korelasyon kimliği *(13)* | `CorrelationIds`, `CorrelationIdFilter` *(shared-web)*: `X-Correlation-Id` başlığı, problemlerde `correlationId`, beklenmeyen hatanın günlüğünde aynı kimlik | ✅ | 9.2 |
| MicroProfile Health readiness *(14)* | `BookstoreReadiness` *(`/health/ready`: migration'lar ve veritabanı)* | ✅ | 9.2 |
| PostgreSQL ile entegrasyon testi CI'da *(14)* | GitHub Actions: `.github/workflows/build.yml`, Testcontainers ile PostgreSQL 17 *(ADR-0022)* | ✅ | 9.1 |

## Dersin dışında kalan, projede uygulanan konular

Bunlar ders notlarında yok; kitap mağazasının ihtiyacından doğdu ya da Faz 0–3'te genel Java müfredatı varsayılarak eklendi. Korunuyorlar ama müfredatın parçası sayılmıyorlar.

| Konu | Nerede |
| --- | --- |
| `ThreadLocal` | `JdbcTransactionRunner` *(Faz 6'da JTA'ya bırakılınca kalkar)* |
| Yansıma *(reflection)*, `RecordComponent` | `RecordRowMapper` |
| İç sınıf, `Iterator` | `Cart.LineIterator` |
| `ArrayDeque` ile genişlik öncelikli gezinme | `Category.selfAndDescendants` |
| Diziler ve kontrol basamağı hesabı | `Isbn` *(ISBN-10 → ISBN-13)* |
| Java serileştirmesi | ✅ `GuestCart` ve `SessionGuestCart` *(misafir sepeti HTTP oturumunda, ADR-0020)* |
| Eşzamanlılık testleri *(`ExecutorService`, `CountDownLatch`)* | `JdbcSalesRepositoriesTest.OrderNumbers`, `CheckoutWithDatabaseTest` |
| Kimlik doğrulama, BCrypt | ✅ Faz 7.1–7.2 *(ADR-0011, ADR-0020)*: `BcryptPasswordHasher`, `BookstoreAuthenticationMechanism`, `AccountIdentityStore`, `/api/v1/session`, `ClientHeaderFilter` |
| HTML / JavaScript arayüz | ✅ Faz 7.4 *(ADR-0011)*: `app/src/main/webapp`, `js/api.js`, `js/i18n.js`, sayfa başına `js/pages/…`, `i18n/tr.json` ve `en.json`; `WebPagesTest` |
| Yönetim paneli | Faz 8 *(ADR-0021)*: 8.1 ✅ `CatalogAdminFacade`, `CatalogAdminService`, `AdminBookResource`, `AdminAuthorResource`, `AdminCategoryResource`; 8.2 ✅ `FileStorage`, `FileSystemFileStorage`, `CoverImage`, `BookstoreFiles`; 8.3 ✅ `CustomerAdminFacade`, `ReviewAdminFacade`, `AdminCustomerResource`, `AdminReviewResource`; 8.4 ✅ `admin*.html`, `js/admin.js`, `js/pages/admin-…` |
| Kapsam ölçümü ve eşiği *(NFR-6)*, performans testi *(NFR-9)* | ✅ Faz 9.6 *(ADR-0022)*: JaCoCo `check`, `CatalogPerformanceTest` |

## Plandan çıkarılanlar

Önceki planda, ders notları görülmeden önce eklenmiş ama müfredatla ilgisi olmayan konular: thread havuzuyla arka plan bildirimleri, `ConcurrentCache`, NIO.2 ile dosya işleme ve CSV'den örnek veri yükleme. Bildirimler ADR-0007'deki gözlemcilere, örnek veri yükleme ADR-0010'daki açılış adımına taşındı. Kapak görseli depolama yönetim paneliyle birlikte Faz 8'de yapılacak.
