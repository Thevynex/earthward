# B1a — Bölge seçim önizlemesi

## Kaynaklar ve kapsam

Minecraft 1.21.1 ekran dokümantasyonu ve NeoForge 1.21.1 ScreenEvent API incelendi:
- https://docs.neoforged.net/docs/1.21.1/gui/screens/
- https://github.com/NeoForged/NeoForge/blob/327897179428559b4dd7fd29ef3f9f83aa40452f/src/main/java/net/neoforged/neoforge/client/event/ScreenEvent.java
- Client-only kayıt örneği: NeoForgeMDKs/MDK-1.21.1-ModDevGradle, 70d335c commit, ExampleModClient.java. MDK MIT bildirimi korunuyor. ScreenEvent kaynak kodu kopyalanmadı; yalnız API kullanıldı.

Tek oyunculu dünya listesinde Earthward düğmesi, Türkçe/İngilizce kaynak metinleri, değişken derinlikli idari seçim kataloğu, ileri/geri/iptal ve eksik veri durumu eklendi. Şimdilik tek pilot yolu sunulur; birden fazla ülke/bölge listesi ve gerçek harita yok. Katalog Java koordinatlarına gömülmez; küçük JSON kaynağından yüklenir. 1-8 idari seviye, sürüm, boyut ve kimlik kontrolleri vardır.

Dünya oluşturma düğmesi kasıtlı olarak pasiftir. Coğrafi veri paketi, harita tabanı ve generator henüz yoktur. Vanilla yeni dünya ekranı henüz değiştirilmez. Bu, nihai kullanıcı gereksiniminin tamamlanması değil, kayıt oluşturmayan güvenli UI artımıdır. İstemci ekranı fiziksel CLIENT tarafıyla sınırlandırılır.

Gson Minecraft geliştirme bağımlılıklarından kullanılır; ek kopyası/shading yapılmaz, kendi Apache-2.0 lisansı geçerlidir. Gerçek coğrafi veri veya görsel eklenmedi.

## Test durumu

B0 fb452721 commit'inin iki Actions kontrolü geçti: mod derleme, sözleşmeler/JUnit ve JAR doğrulaması başarılı. Bu durum bu yeni UI commit'inin sonucu değildir.

Bu artım 8 katalog JUnit testi ekler. Standalone 15 sözleşme testi korunur. Yeni kaynaklar CI'da derlenip test edilmelidir.

Oyun içi yükleme, tıklama, görsel QA, GUI ölçekleri, ekran boyutları, Escape ve eski kayıtlarla uyumluluk henüz çalıştırılmadı. Bunlar geçti sayılmadan B1a kabulü açık kalır. Özellikle aşırı büyük GUI ölçeğinde yerleşim ve uzun yer adlarında metin taşması kontrol edilmelidir.

## Çalıştırma ve kontrol

Başarılı bu commit'in Actions artifact'ı kullanılır veya JDK 21 ve Gradle 9.2.1 ile `gradle runClient` çalıştırılır. Tek oyunculu → Earthward → Ülke/İl/İlçe/Mahalle → veri yok. Geri düğmesi bir önceki adıma, başlangıçta İptal dünya listesine döner; Escape dünya listesine döner. Son adımda dünya oluşturulmamalı. Sonra ayrı yedekli test kaydı açılıp çıkış/yeniden giriş denenmeli. Kod incelemesinde yazım yoktur; gerçek kayıt güvenliği oyun testi olmadan kanıtlanmış sayılmaz.
