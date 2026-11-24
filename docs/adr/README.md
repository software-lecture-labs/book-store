# Mimari Karar Kayıtları (ADR)

Sonradan değiştirilmesi pahalı olan her karar burada bir ADR *(Architecture Decision Record)* olarak tutulur. Biçim, ders notlarındaki ADR'lerle aynıdır: **Bağlam**, **Karar**, **Değerlendirilen Alternatifler**, **Sonuçlar** ve kararın hangi test ya da ArchUnit kuralıyla korunduğunu gösteren **Doğrulama** bölümü.

ADR-0003 ile ADR-0010 arası, ders notlarındaki aynı numaralı ADR'lerin konularını kitap mağazasına uyarlar. Böylece iki repo yan yana okunabilir.

## Kurallar

- Kabul edilmiş bir ADR silinmez. Değişen bir madde için yeni bir ADR yazılır ve eski ADR'ye tarihli bir **Not** düşülür *(dersteki "ADR-0010 ile 3. madde güncellendi" notları gibi)*.
- Bir ADR'nin **Doğrulama** tablosundaki her satır ya bir ArchUnit kuralına ya da bir teste karşılık gelir. Karşılığı henüz yazılmamış satırlar *(planlı)* olarak işaretlenir.
- Yeni bir ADR, onu gerektiren kod değişikliğiyle aynı commit'te gelir.

## Liste

| ADR | Başlık | Ders bölümü | Durum |
| --- | --- | --- | --- |
| [0001](ADR-0001.md) | Gitflow ve Sürümleme | 1, 2 | Yerini aldı (0017) |
| [0002](ADR-0002.md) | İş Alanına Göre Maven Modülleri | 6, 9 | Kabul edildi |
| [0003](ADR-0003.md) | Modül ve Paket Bağımlılıklarının Yönü | 9 | Kabul edildi |
| [0004](ADR-0004.md) | Domain Modeli ve Paylaşılan Çekirdek Kuralları | 2, 3, 4, 7 | Kabul edildi |
| [0005](ADR-0005.md) | Ortak Kodlama Kuralları | 9 | Kabul edildi |
| [0006](ADR-0006.md) | Composition Root, Constructor Injection ve Clock | 10 | Kabul edildi |
| [0007](ADR-0007.md) | Domain Event'ler ve Yayınlama | 11 | Kabul edildi |
| [0008](ADR-0008.md) | Persistence Sınırı, JDBC, Flyway ve PostgreSQL | 12 | Kabul edildi |
| [0009](ADR-0009.md) | HTTP Sınırı ve Hata Sözleşmesi | 13 | Kabul edildi |
| [0010](ADR-0010.md) | Uygulama Sunucusu: Jakarta EE ve Payara Micro | 14 | Kabul edildi |
| [0011](ADR-0011.md) | Kullanıcı Arayüzü: REST API Üzerinde HTML ve JavaScript | — | Kabul edildi |
| [0012](ADR-0012.md) | Puan Özetinin Kitapta Tutulması | 4 | Kabul edildi |
| [0013](ADR-0013.md) | Ödeme Yöntemine Göre İlk Sipariş Durumu | 4 | Kabul edildi |
| [0014](ADR-0014.md) | Sipariş Öncesi Sepet Kontrolü | 10 | Kabul edildi |
| [0015](ADR-0015.md) | İyimser Kilitleme | 12, 14 | Kabul edildi |
| [0016](ADR-0016.md) | Türkçe Duyarsız Arama İçin Normalleştirilmiş Anahtarlar | 4, 12 | Kabul edildi |
| [0017](ADR-0017.md) | Doğrudan main Üzerinde Çalışma | 1, 2 | Kabul edildi |
| [0018](ADR-0018.md) | Paylaşılan JDBC Modülü ve Modül Başına Migration Geçmişi | 9, 12 | Kabul edildi |
| [0019](ADR-0019.md) | Paylaşılan Web Modülü ve Elle Yazılmış HTTP Sınırı | 13 | Kabul edildi |
| [0020](ADR-0020.md) | Kimlik Doğrulama, Oturum ve Misafir Sepeti | — | Kabul edildi |
| [0021](ADR-0021.md) | Yönetim API'si ve Kapak Görselleri | — | Kabul edildi |
| [0022](ADR-0022.md) | Sağlamlaştırma: CI, İzlenebilirlik, Tekrar ve Eşzamanlılık | — | Kabul edildi |
| [0023](ADR-0023.md) | JPA ile İkinci Persistence Uygulaması | — | Kabul edildi |

## Eski karar numaraları

Bu klasörden önce kararlar `docs/DECISIONS.md` içinde **K-xx** numaralarıyla tutuluyordu. Eşleşmeleri:

| Eski | Yeni | Değişiklik |
| --- | --- | --- |
| K-01 Çok modüllü katmanlı monolit | ADR-0002 | Katmana göre modüller yerine iş alanına göre modüller |
| K-02 İş katmanlarında framework yok | ADR-0004, ADR-0006 | Korundu; ArchUnit ile doğrulanıyor *(Faz 4.2)* |
| K-03 Veri erişimi elle JDBC ile | ADR-0008 | Korundu; JPA ileri bir faza bırakıldı |
| K-04 MySQL; testlerde H2 | ADR-0008 | MySQL yerine PostgreSQL 17 |
| K-05 Thymeleaf + REST API | ADR-0009, ADR-0011 | Thymeleaf yerine REST API üzerinde HTML ve JavaScript |
| K-06 Web'de oturum, API'de HTTP Basic | ADR-0011 | ADR-0020 ile Jakarta Security'de uygulandı |
| K-07 Java 21, Spring Boot 4.1 | ADR-0010 | Spring Boot yerine Jakarta EE 11 ve Payara Micro 7 |
| K-08 Kod İngilizce, doküman Türkçe | ADR-0005 | Korundu |
| K-09 Kimliği repository atar | ADR-0004 | Tipli kimliklerle (*typed identity*) yeniden ele alındı |
| K-10 Puan özeti kitapta tutulur | ADR-0012 | Korundu |
| K-11 Ödeme yöntemine göre ilk sipariş durumu | ADR-0013 | Korundu |
| K-12 Sipariş öncesi sepet kontrolü ayrı işlemde | ADR-0014 | Korundu |
| K-13 Servisler okuma modeli döner | ADR-0003 | `api` paketindeki DTO'lar ve mapper'lar olarak yeniden ele alındı |
| K-14 Stok tutarlılığı iyimser kilitlemeyle | ADR-0015 | Korundu |
| K-15 Arama için normalleştirilmiş anahtarlar | ADR-0016 | Korundu |
