# Book Store — Ürün Spesifikasyonu

Bu doküman sistemin **ne yaptığını** tanımlar. **Nasıl** yapıldığı [ARCHITECTURE.md](ARCHITECTURE.md) içindedir.

## 1. Amaç

Book Store, kitap satışı yapan bir web uygulamasıdır. İki hedefi vardır:

1. Çalışan, uçtan uca bir e-ticaret uygulaması olmak (katalog → sepet → sipariş).
2. **Java Patterns and Practices** ders notlarındaki *([java-study-notes](https://github.com/software-lecture-labs/java-study-notes), Bölüm 01–14)* konuların her birini gerçek bir kullanım yerinde uygulamak; ders notlarının sözünü ettiği "Lab projesi"nin yerine geçmek. Konu–kod eşlemesi [CURRICULUM-MAP.md](CURRICULUM-MAP.md) içindedir.

## 2. Aktörler

| Aktör | Tanım |
|---|---|
| **Ziyaretçi** | Giriş yapmamış kullanıcı. Kataloğu gezer, arama yapar, sepet oluşturur. |
| **Müşteri** | Kayıtlı ve giriş yapmış kullanıcı (`CUSTOMER` rolü). Sipariş verir, yorum yazar. |
| **Yönetici** | `ADMIN` rolüne sahip kullanıcı. Kataloğu, siparişleri ve kullanıcıları yönetir. |

## 3. Fonksiyonel gereksinimler

Öncelik: **M** = olmazsa olmaz, **S** = olmalı, **C** = olursa iyi.

### 3.1 Katalog (CAT)

| No | Gereksinim | Öncelik |
|---|---|---|
| CAT-1 | Kitaplar sayfalı liste olarak görüntülenir (varsayılan sayfa boyutu 20). | M |
| CAT-2 | Kitap detay sayfası: başlık, yazar(lar), ISBN, yayınevi, yayın yılı, dil, sayfa sayısı, açıklama, fiyat, stok durumu, kapak görseli, ortalama puan. | M |
| CAT-3 | Bir kitabın birden çok yazarı, bir yazarın birden çok kitabı olabilir. | M |
| CAT-4 | Kategoriler iki seviyeli bir ağaçtır (ör. *Edebiyat → Roman*). Bir kitap tek bir kategoriye bağlıdır. | M |
| CAT-5 | Kategoriye ve yazara göre listeleme yapılır. Üst kategori seçildiğinde alt kategorilerin kitapları da gelir. | M |
| CAT-6 | Stokta olmayan kitap listede görünür ama sepete eklenemez. | M |
| CAT-7 | Pasife alınmış kitap müşteriye görünmez; geçmiş siparişlerde görünmeye devam eder. | S |

### 3.2 Arama ve filtreleme (SRC)

| No | Gereksinim | Öncelik |
|---|---|---|
| SRC-1 | Başlık, yazar adı ve ISBN üzerinde metin araması yapılır (büyük/küçük harf ve Türkçe karakter duyarsız). | M |
| SRC-2 | Sonuçlar kategori, fiyat aralığı, dil ve stok durumuna göre filtrelenir. | M |
| SRC-3 | Sonuçlar fiyata, başlığa, yayın yılına ve ortalama puana göre artan/azalan sıralanır. | M |
| SRC-4 | Arama, filtre ve sıralama birlikte kullanılabilir; sonuç sayfalıdır. | M |

### 3.3 Sepet (CRT)

| No | Gereksinim | Öncelik |
|---|---|---|
| CRT-1 | Ziyaretçi ve müşteri sepete kitap ekler, adet değiştirir, satır siler, sepeti boşaltır. | M |
| CRT-2 | Ziyaretçinin sepeti oturumda tutulur; giriş yaptığında kayıtlı sepetiyle birleştirilir (aynı kitabın adetleri toplanır). | M |
| CRT-3 | Müşterinin sepeti kalıcıdır; tekrar giriş yaptığında geri gelir. | M |
| CRT-4 | Bir satırın adedi mevcut stoğu ve satır başına üst sınırı (10) aşamaz. | M |
| CRT-5 | Sepet her görüntülendiğinde güncel fiyat ve stokla yeniden doğrulanır; değişiklik varsa kullanıcı uyarılır. | S |

### 3.4 Sipariş (ORD)

| No | Gereksinim | Öncelik |
|---|---|---|
| ORD-1 | Yalnızca giriş yapmış müşteri sipariş verebilir. | M |
| ORD-2 | Sipariş verilirken teslimat adresi ve ödeme yöntemi seçilir. | M |
| ORD-3 | Ödeme **simülasyondur**: gerçek bir ödeme sağlayıcısı çağrılmaz, kart bilgisi saklanmaz. | M |
| ORD-4 | Sipariş oluşturulurken stok düşülür. Aynı anda gelen siparişler stoğu eksiye düşüremez. | M |
| ORD-5 | Sipariş satırı, sipariş anındaki birim fiyatı ve kitap başlığını kopyalar; sonradan yapılan fiyat değişikliği eski siparişi etkilemez. | M |
| ORD-6 | Sipariş durumları: `CREATED → PAID → SHIPPED → DELIVERED`. `CREATED` ve `PAID` durumundan `CANCELLED` durumuna geçilebilir. Başka geçiş yoktur. | M |
| ORD-7 | İptal edilen siparişin stoğu geri eklenir. | M |
| ORD-8 | Müşteri kendi sipariş geçmişini ve sipariş detayını görür; başkasının siparişini göremez. | M |
| ORD-9 | Sipariş oluştuğunda ve durumu değiştiğinde müşteriye bildirim üretilir (e-posta **gönderilmez**, log'a yazılır). Bildirim, isteği bekletmeden arka planda işlenir. | S |
| ORD-10 | Her siparişin insan tarafından okunabilir benzersiz bir numarası vardır (ör. `BS-2026-000042`). | S |
| ORD-11 | Kargo ücreti kurala göre hesaplanır: sepet tutarı 500 TL ve üzeriyse ücretsiz, altındaysa sabit 49,90 TL. Kural kod değiştirmeden başka bir kuralla değiştirilebilir olmalıdır. | S |

### 3.5 Üyelik ve yetkilendirme (ACC)

| No | Gereksinim | Öncelik |
|---|---|---|
| ACC-1 | Ziyaretçi ad, soyad, e-posta ve parola ile kayıt olur. E-posta benzersizdir. | M |
| ACC-2 | Parola en az 8 karakterdir, harf ve rakam içerir; geri döndürülemez biçimde (BCrypt) saklanır. | M |
| ACC-3 | Giriş e-posta ve parola ile yapılır; çıkış oturumu sonlandırır. | M |
| ACC-4 | Müşteri profilini günceller, parolasını değiştirir. | M |
| ACC-5 | Müşteri birden çok teslimat adresi kaydeder, birini varsayılan yapar. | S |
| ACC-6 | Roller: `CUSTOMER`, `ADMIN`. Yönetim ekranları ve yönetim API'si yalnızca `ADMIN` rolüne açıktır. | M |
| ACC-7 | Pasife alınan kullanıcı giriş yapamaz. | S |

### 3.6 Yönetim paneli (ADM)

| No | Gereksinim | Öncelik |
|---|---|---|
| ADM-1 | Kitap ekleme, güncelleme, pasife alma. | M |
| ADM-2 | Yazar ve kategori ekleme, güncelleme, silme (bağlı kitabı olan silinemez). | M |
| ADM-3 | Stok adedi ve fiyat güncelleme. | M |
| ADM-4 | Kitap kapak görseli yükleme (JPEG/PNG, en çok 2 MB). | S |
| ADM-5 | Tüm siparişleri listeleme, duruma göre filtreleme, sipariş durumunu ilerletme veya iptal etme. | M |
| ADM-6 | Kullanıcıları listeleme, pasife/aktife alma. | S |
| ADM-7 | Yorumları listeleme ve uygunsuz yorumu kaldırma. | S |
| ADM-8 | Özet ekranı: toplam kitap, stoğu azalan kitaplar, bekleyen siparişler. | C |

### 3.7 Yorum ve puanlama (REV)

| No | Gereksinim | Öncelik |
|---|---|---|
| REV-1 | Müşteri bir kitaba 1–5 arası puan ve isteğe bağlı metin (en çok 2000 karakter) ile yorum yazar. | M |
| REV-2 | Yalnızca kitabı satın almış ve siparişi `DELIVERED` olmuş müşteri yorum yazabilir. | M |
| REV-3 | Bir müşteri bir kitaba tek yorum yazar; yorumunu güncelleyebilir ve silebilir. | M |
| REV-4 | Kitap detayında yorumlar tarihe göre yeniden eskiye, sayfalı listelenir. | M |
| REV-5 | Kitabın ortalama puanı ve yorum sayısı liste ve detayda gösterilir. | M |
| REV-6 | Detay sayfasında puan dağılımı (kaç adet 5 yıldız, 4 yıldız …) gösterilir. | C |

### 3.8 REST API (API)

| No | Gereksinim | Öncelik |
|---|---|---|
| API-1 | Tüm işlemler `/api/v1` altında JSON REST API olarak sunulur. Web arayüzü bu API'nin bir istemcisidir; API'de olmayan bir işlemi yapamaz. | M |
| API-2 | Katalog ve arama uçları herkese açıktır; sepet, sipariş, hesap ve yorum uçları kimlik doğrulama ister; `/api/v1/admin/**` `ADMIN` rolü ister. | M |
| API-3 | Hatalar tek biçimde döner (RFC 9457 *Problem Details*, `application/problem+json`): durum kodu, kararlı hata kodu (`code`), mesaj, alan hataları. Durum kodları: biçim hatası 400, yetkisiz erişim 403, bulunamayan kayıt 404, durum çatışması 409, iş kuralı ihlali 422, beklenmeyen hata 500 (iç ayrıntı içermez). | M |
| API-4 | API, MicroProfile OpenAPI ile `/openapi` adresinde belgelenir. | S |

## 4. Fonksiyonel olmayan gereksinimler

| No | Gereksinim |
|---|---|
| NFR-1 | **Para**: tüm tutarlar `BigDecimal` tabanlı bir değer nesnesidir; para birimi TRY, 2 ondalık, yuvarlama `HALF_UP`. `double`/`float` kullanılmaz. |
| NFR-2 | **Doğrulama**: girdinin biçimi HTTP sınırında (Bean Validation) doğrulanır ve tüm hatalı alanlar tek yanıtta döner; iş kuralları domain'de doğrulanır ve sınırda tekrar yazılmaz. |
| NFR-3 | **Güvenlik**: BCrypt parola, rol tabanlı yetki, tarayıcı oturumunda CSRF koruması, SQL'de yalnızca parametreli sorgu, arayüzde kullanıcı verisinin HTML olarak değil metin olarak yazılması; parola ve bağlantı bilgisi uygulama arşivinde tutulmaz. |
| NFR-4 | **Dil**: arayüz Türkçe ve İngilizce (iki metin sözlüğü); varsayılan Türkçe. Hata mesajları API'nin döndürdüğü `code` değerine göre seçilir. |
| NFR-5 | **Tutarlılık**: sipariş oluşturma (stok düşme + sipariş kaydı + sepet boşaltma) tek bir veritabanı işlemidir. |
| NFR-6 | **Test**: domain ve application paketlerinde birim testi; her repository arayüzü için bellek içi ve veritabanı implementasyonlarının birlikte geçtiği sözleşme testi; HTTP katmanında uçtan uca test. Hedef satır kapsamı: domain + application için %80. |
| NFR-7 | **Kayıt**: `System.Logger` ile kayıt; iş kodu konsola yazmaz; parola ve kişisel veri log'a yazılmaz. |
| NFR-8 | **Taşınabilirlik**: `./mvnw verify` komutu, JDK 21 dışında hiçbir ön kurulum olmadan (PostgreSQL ve Docker dahil) geçer. Docker varsa aynı testler gerçek PostgreSQL 17 üzerinde de çalışır. |
| NFR-9 | **Performans**: katalog listesi ve arama, 10.000 kitaplık veri setinde tek istekte 300 ms altında yanıt verir (yerel makine). |
| NFR-10 | **Mimari kurallar**: modül ve paket bağımlılık yönleri ile domain kuralları ([docs/adr](adr/README.md)) ArchUnit testleriyle her derlemede doğrulanır; ihlal derlemeyi kırar. |

## 5. Kapsam dışı

Bilinçli olarak yapılmayacaklar:

- Gerçek ödeme, gerçek e-posta/SMS gönderimi, kargo entegrasyonu
- Kupon, kampanya, istek listesi, öneri motoru
- Satış raporları ve CSV/JSON toplu içe-dışa aktarım
- Ayrı çalışan TCP soket bildirim sunucusu
- Tek sayfa uygulaması (React vb.), mobil uygulama, masaüstü arayüz (Swing/JavaFX)
- Mikroservis mimarisi, mesaj kuyruğu, dağıtık önbellek
- JWT / OAuth2 ile kimlik doğrulama

## 6. Sözlük

| Terim | Kod karşılığı | Anlamı |
|---|---|---|
| Kitap | `Book` | Satılan ürün. ISBN ile benzersizdir. |
| Yazar | `Author` | Kitabı yazan kişi. |
| Kategori | `Category` | Kitapların gruplandığı iki seviyeli ağaç düğümü. |
| Müşteri | `Customer` | Sipariş verebilen kayıtlı kullanıcı. |
| Sepet | `Cart`, `CartItem` | Henüz siparişe dönüşmemiş kitap–adet listesi. |
| Sipariş | `Order`, `OrderLine` | Onaylanmış, fiyatı sabitlenmiş satın alma. |
| Sipariş durumu | `OrderStatus` | Siparişin yaşam döngüsündeki adım. |
| Yorum | `Review` | Müşterinin bir kitaba verdiği puan ve metin. |
| Stok | `Stock` | Bir kitabın satılabilir adedi. |
| Tutar | `Money` | Para birimi ile birlikte parasal değer. |
