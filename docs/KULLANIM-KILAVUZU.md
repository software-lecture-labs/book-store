# Kitap Mağazası — Kullanma Kılavuzu

Bu kılavuz mağazayı bilgisayarınızda nasıl açacağınızı, müşteri ve yönetici olarak neler yapabileceğinizi anlatır. Geliştirme ve mimari belgeleri için [README](../README.md) ve [docs](.) klasörüne bakın.

## İçindekiler

1. [Mağazayı başlatmak](#1-mağazayı-başlatmak)
2. [Müşteri olarak kullanmak](#2-müşteri-olarak-kullanmak)
3. [Yönetici olarak kullanmak](#3-yönetici-olarak-kullanmak)
4. [Baştan sona bir deneme](#4-baştan-sona-bir-deneme)
5. [Sık karşılaşılan durumlar](#5-sık-karşılaşılan-durumlar)
6. [Geliştiriciler ve API istemcileri için](#6-geliştiriciler-ve-api-istemcileri-için)

---

## 1. Mağazayı başlatmak

### Gerekenler

- **JDK 21** *(Java 21)*. `JAVA_HOME` ortam değişkeni JDK 21 klasörünü göstermeli ya da `java` komutu 21 sürümünü açmalı.
- İnternet bağlantısı, yalnızca ilk çalıştırmada: Maven ve Payara Micro indirilir.
- Başka bir kurulum gerekmez; veritabanı varsayılan olarak bellek içindedir.

### Başlatma

**Windows:** `scripts\start.cmd` dosyasına çift tıklayın ya da proje klasöründe bir komut penceresinde çalıştırın:

```bat
scripts\start.cmd
```

`JAVA_HOME` tanımlı değilse önce PowerShell'de tanımlayın *(klasörü kendi JDK'nıza göre değiştirin)*:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-21.0.13.9-hotspot'; .\scripts\start.cmd
```

**Linux, macOS ve Git Bash:**

```bash
./scripts/start.sh
```

Betik önce projeyi derler *(testler atlanır, ilk seferde birkaç dakika sürebilir)*, sonra Payara Micro'yu başlatır. Pencerede **`Payara Micro ... ready`** yazısı görününce mağaza hazırdır:

> **http://localhost:8080/**

Daha önce derlediyseniz derlemeyi atlayabilirsiniz: `scripts\start.cmd -SkipBuild` ya da `./scripts/start.sh --skip-build`.

**Durdurmak için** sunucunun çalıştığı pencerede `Ctrl+C`'ye basın.

### Demo hesaplar

Sunucu her açıldığında 8 kitap, kategoriler ve iki hesap yüklenir:

| Hesap | E-posta | Parola |
| --- | --- | --- |
| Müşteri *(Ayşe Yılmaz, kayıtlı bir adresi var)* | `ayse@bookstore.local` | `kitap2026` |
| Yönetici | `admin@bookstore.local` | `kitap2026` |

> **Dikkat:** Varsayılan veritabanı bellek içindedir. Sunucu durunca bütün siparişler, kayıtlar ve değişiklikler silinir; bir sonraki açılışta demo verisiyle yeniden başlar. Verileri saklamak için [PostgreSQL ile çalıştırma](#postgresql-ile-kalıcı-çalıştırma) bölümüne bakın.

### Dil

Sağ üstteki **TR / EN** düğmeleri arayüzü Türkçe ya da İngilizce yapar. Seçim tarayıcıda hatırlanır; varsayılan Türkçedir.

---

## 2. Müşteri olarak kullanmak

### Kitaplara göz atmak ve aramak

Ana sayfa **Kitaplar**'dır. Üstteki arama kutusu:

- **Ara:** kitap adının, yazar adının ya da ISBN'in bir parçası. Türkçe harfler ve büyük/küçük harf fark etmez *(“calikusu” yazınca “Çalıkuşu” bulunur)*.
- **Kategori:** bir üst kategori seçilince alt kategorilerdeki kitaplar da gelir.
- **Dil**, **Sıralama** *(ada göre, fiyat artan/azalan, en yeni, en beğenilen)* ve **Yalnızca stokta olanlar**.

Arama adres çubuğunda saklanır; bağlantıyı paylaşabilir, geri tuşuyla dönebilirsiniz. Tükenen kitaplar listede görünür ama sepete eklenemez.

### Kitap sayfası

Bir kitabın adına tıklayınca kapak görseli, yazarlar, yayın bilgileri, fiyat ve stok görünür. Buradan istediğiniz adette sepete ekleyebilirsiniz. Sayfanın altında puan dağılımı *(kaç kişi 5 yıldız, 4 yıldız…)* ve yorumlar yer alır.

### Sepet

- Giriş yapmadan da sepete kitap ekleyebilirsiniz; bu **ziyaretçi sepeti** tarayıcı oturumunuzda saklanır.
- Giriş yapınca ziyaretçi sepetiniz hesabınızdaki sepete eklenir.
- Sepet sayfasında adetleri değiştirebilir, bir kitabı çıkarabilir ya da sepeti boşaltabilirsiniz. Bir kitaptan en çok 10 adet alınabilir.
- **Kargo** 500,00 ₺ ve üstü siparişlerde ücretsiz, altında 49,90 ₺.
- Bir kitabın fiyatı değiştiyse, stoku azaldıysa ya da satıştan kalktıysa sepet sayfası bunu üstte bildirir ve sepeti düzeltir; görmediğiniz bir tutarı asla ödemezsiniz.

### Kayıt olmak ve giriş yapmak

- **Kayıt ol:** ad, soyad, e-posta ve parola. Parola en az 8 karakter olmalı, en az bir harf ve bir rakam içermeli. Kayıttan sonra otomatik olarak giriş yapılır.
- **Giriş yap / Çıkış yap:** sağ üstte. Giriş yapınca adınız ve **Siparişlerim**, **Hesabım** bağlantıları görünür.

### Sipariş vermek

1. Sepette **Siparişi tamamla**'ya basın *(giriş yapmadıysanız önce giriş sayfası açılır)*.
2. **Teslimat adresi** seçin. Kayıtlı adresiniz yoksa **Yeni adres ekle** ile Hesabım sayfasından ekleyin.
3. **Ödeme yöntemi** seçin:

   | Yöntem | Sipariş durumu |
   | --- | --- |
   | Kredi kartı | Hemen **Ödendi** *(bu demo sürümde her kart onaylanır, gerçek ödeme alınmaz)* |
   | Havale / EFT | **Oluşturuldu**; ödemeyi yönetici işaretler |
   | Kapıda ödeme | **Oluşturuldu**; ödemeyi yönetici işaretler |

4. **Siparişi ver**'e basın. Sipariş numaranız *(örneğin `BS-2026-000001`)* gösterilir.

Düğmeye iki kez basmak ya da bağlantı koptuktan sonra tekrar denemek ikinci bir sipariş açmaz; aynı sipariş gösterilir.

### Siparişlerim

Geçmiş siparişleriniz en yenisi üstte listelenir. Birine tıklayınca kitaplar, tutarlar, teslimat adresi, ödeme yöntemi ve **durum** görünür. Siparişin durumunu yönetici ilerletir: **Oluşturuldu → Ödendi → Kargoda → Teslim edildi**, ya da **İptal edildi**.

### Hesabım

- **Bilgilerim:** ad, soyad, e-posta.
- **Parola:** şimdiki parolanızla yeni bir parola belirleyin.
- **Adreslerim:** adres ekleyin, varsayılan yapın ya da silin. Sipariş sayfasında varsayılan adres seçili gelir.

### Yorum yazmak

Bir kitaba yorum yazabilmek için o kitabı satın almış ve siparişin **Teslim edildi** olması gerekir. Kitap sayfasında 1–5 yıldız ve isteğe bağlı bir yorum yazarsınız; sonra düzenleyebilir ya da silebilirsiniz. Her kitaba bir yorum yazılabilir.

---

## 3. Yönetici olarak kullanmak

`admin@bookstore.local` ile giriş yapınca üst menüde **Yönetim** bağlantısı çıkar. Yönetim sayfalarının üstünde sekmeler vardır. Bu sayfaları yalnızca yöneticiler açabilir; başkası açmaya çalışırsa ana sayfaya yönlendirilir.

### Özet

- Satıştaki kitap sayısı, stoğu 5'in altına düşen kitaplar ve kargolanmayı bekleyen *(Ödendi durumundaki)* siparişler.
- Stoğu azalan bir kitaba tıklayınca düzenleme sayfası açılır.

### Kitaplar

- Satıştan kaldırılmış olanlar dahil bütün kitaplar. Satışta olmayanlar soluk görünür.
- **Fiyat** ve **Stok** kutularını değiştirip kutudan çıkınca değişiklik hemen kaydedilir.
- **Satışta** kutusunun işaretini kaldırmak kitabı müşterilerden gizler. Kitap silinmez, geçmiş siparişlerde görünmeye devam eder.
- **Yeni kitap** ile kitap eklenir.

### Kitap ekleme ve düzenleme

- **ISBN** yalnızca eklerken girilir *(ISBN-10 ya da ISBN-13, tireli ya da tiresiz)*, sonra değişmez.
- **Yazarlar:** birden çok yazar için Ctrl *(Mac'te Cmd)* tuşuna basılı tutarak seçin. Listede olmayan bir yazarı önce **Yazarlar ve kategoriler** sekmesinden ekleyin.
- Kaydettikten sonra sağ tarafta **Kapak görseli** yükleyebilirsiniz: JPEG ya da PNG, en çok 2 MB. Yeni kapak eskisinin yerini alır.
- Siz bir kitabı düzenlerken başka bir yönetici aynı kitabı değiştirip kaydettiyse kaydınız reddedilir ve sayfayı yenilemeniz istenir. Böylece kimse başkasının değişikliğini fark etmeden ezemez.

### Yazarlar ve kategoriler

- Yazar ekleme, yeniden adlandırma ve silme.
- Kategori ekleme *(en üst düzeyde ya da bir üst kategorinin altında)*, yeniden adlandırma ve silme.
- Kitabı olan bir yazar ya da kategori, alt kategorisi olan bir kategori silinemez; önce kitapları başka bir yere taşıyın.

### Siparişler

- Bütün siparişler; üstteki **Durum** listesiyle süzülür.
- Bir siparişe tıklayınca müşteri numarası, adres, kitaplar ve o durumdan geçilebilecek adımların düğmeleri görünür:

  | Şimdiki durum | Düğmeler |
  | --- | --- |
  | Oluşturuldu | Ödendi olarak işaretle, Siparişi iptal et |
  | Ödendi | Kargoya verildi, Siparişi iptal et |
  | Kargoda | Teslim edildi |
  | Teslim edildi, İptal edildi | *(son durum, değişmez)* |

- **İptal** edilen siparişin kitapları stoğa geri döner. Durum değişikliklerinde müşteriye bildirim gönderilir *(bu sürümde bildirimler sunucu günlüğüne yazılır)*.

### Kullanıcılar

- Bütün hesaplar, en yeni kayıt üstte.
- **Giriş yapabilir** kutusunun işaretini kaldırınca hesap kapanır: kişi giriş yapamaz, açık oturumu da bir sonraki tıklamasında sona erer. İşareti koyunca hesap yeniden açılır.
- Yönetici hesapları buradan kapatılamaz.

### Yorumlar

Bütün yorumlar, en yenisi üstte. Uygunsuz bir yorumu **Yorumu kaldır** ile silebilirsiniz; yorumun puanı kitabın ortalamasından da düşülür.

---

## 4. Baştan sona bir deneme

Mağazanın neredeyse her parçasını 10 dakikada görmek için:

1. **Ziyaretçi olarak** ana sayfada “Dune”u sepete ekleyin, sepete gidin.
2. **Sipariş için giriş yapın** → `ayse@bookstore.local` / `kitap2026`. Sepetiniz hesaba geçer, ödeme sayfası açılır.
3. **Kredi kartı** seçip siparişi verin. Sipariş **Ödendi** olarak görünür.
4. **Çıkış yapın**, `admin@bookstore.local` ile girin. **Yönetim → Özet**'te sipariş kargolanmayı bekliyor.
5. Siparişi açın: **Kargoya verildi**, sonra **Teslim edildi**.
6. **Yönetim → Kitaplar**'da Dune'un stoğunu ve fiyatını değiştirin; bir kitabı satıştan kaldırıp ana sayfada kaybolduğunu görün.
7. Çıkış yapıp yeniden Ayşe olarak girin. Dune'un sayfasında artık **yorum yazabilirsiniz**; 5 yıldız verin.
8. Yönetici olarak **Yorumlar**'da yorumu görün; isterseniz kaldırın, puanın kitaptan da düştüğünü görün.
9. Sağ üstten **EN**'e geçip aynı sayfaların İngilizcesine bakın.

---

## 5. Sık karşılaşılan durumlar

| Ne görüyorsunuz? | Ne anlama gelir, ne yapmalı? |
| --- | --- |
| Tarayıcı `localhost:8080`'i açamıyor | Sunucu henüz hazır değil *(“ready” yazısını bekleyin)* ya da durdu. Başlatma penceresine bakın. |
| Başlatırken “Address already in use … 8080” | 8080 portunu başka bir program kullanıyor; o programı ya da önceki Payara'yı kapatın. |
| Başlatırken “JDK 21 or newer is needed” | `JAVA_HOME` JDK 21'i göstermiyor; yukarıdaki gibi tanımlayın. |
| `mvnw clean` “Failed to delete … payara-micro.jar” diyor | Sunucu açıkken bu dosya kullanımdadır. Önce sunucuyu `Ctrl+C` ile durdurun. |
| “Sepetiniz katalogla karşılaştırıldı ve şunlar değişti” | Fiyat, stok ya da satış durumu değişmiş; sepet düzeltildi. Kontrol edip yeniden deneyin. |
| “Bu kayıt siz okuduktan sonra başkası tarafından değiştirildi” | Başka bir yönetici aynı kitabı değiştirdi; sayfayı yenileyip tekrar deneyin. |
| Bir sayfada birden giriş sayfasına yönlendirildiniz | Oturum bitti ya da hesabınız kapatıldı. Yeniden giriş yapın. |
| Hata mesajının sonunda “Başvuru kodu: …” | Sunucuda beklenmeyen bir hata oldu. Bu kodu bildirirseniz sunucu günlüğünde tam olarak bulunur. |
| Sunucuyu kapatıp açınca siparişler kayboldu | Varsayılan veritabanı bellek içindedir; aşağıdaki PostgreSQL bölümüne bakın. |

### PostgreSQL ile kalıcı çalıştırma

Docker kuruluysa proje klasöründe yerel bir PostgreSQL 17 başlatın ve sunucuyu şu ortam değişkenleriyle açın:

```bash
docker compose up -d
```

```powershell
$env:BOOKSTORE_DB_CLASS_NAME = 'org.postgresql.ds.PGSimpleDataSource'; $env:BOOKSTORE_DB_URL = 'jdbc:postgresql://localhost:5432/bookstore'; $env:BOOKSTORE_DB_USER = 'bookstore'; $env:BOOKSTORE_DB_PASSWORD = 'bookstore'; .\scripts\start.cmd
```

Demo verisi yalnızca veritabanı boşsa yüklenir; istemiyorsanız `BOOKSTORE_DEMO_DATA=false` ekleyin. Kapak görselleri varsayılan olarak sistemin geçici klasörüne yazılır; kalıcı bir klasör için `BOOKSTORE_FILES_DIRECTORY`'yi ayarlayın.

---

## 6. Geliştiriciler ve API istemcileri için

| Adres | Ne var? |
| --- | --- |
| `http://localhost:8080/api/v1` | REST API; hatalar RFC 9457 biçiminde `application/problem+json` |
| `http://localhost:8080/openapi` | API'nin OpenAPI tarifi |
| `http://localhost:8080/health/ready` | Hazır olma kontrolü *(veritabanı ve migration'lar)* |

API istemcileri her istekte HTTP Basic ile girer:

```bash
curl -u ayse@bookstore.local:kitap2026 http://localhost:8080/api/v1/me
```

Daha fazla örnek, derleme ve test komutları [README](../README.md)'de; kararlar [docs/adr](adr) klasöründe.
