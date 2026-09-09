# B1c — Doğrulanmış geometri modeli

Dış paket artık yalnız dosya hash'i olarak değil, hash doğrulamasından sonra sınırlı ve immutable bir Java modeli olarak okunur. Şema sürümü ve EPSG:4326 kontrol edilir; yalnız `building_outline` ve `road_centerline` katmanları kabul edilir. OSM way kimlikleri, koordinat dizisi, sonlu enlem/boylam aralıkları, tekrar eden kimlik/katmanlar ve kapalı bina halkaları doğrulanır.

Üst sınırlar 20.000 özellik, 200.000 koordinat noktası ve 16 MiB geometri dosyasıdır. Dosya bounded stream ile okunur; hash geçmeden JSON ayrıştırılmaz. Modelde özellik, katman ve nokta listeleri kopyalanıp değiştirilemez hale getirilir. Kaynak etiketleri çalıştırılmaz ve oyun modeline aktarılmaz.

Seçim ekranı paketi arka planda doğrular ve gerçek ayrıştırılmış modelden bina, yol ve nokta sayılarını gösterir. Bu işlem dünya kaydı oluşturmaz veya mevcut kaydı değiştirmez. Dünya oluşturma düğmesi kapalıdır.

Bu artım 7 geometri ve güncellenmiş 6 yükleyici JUnit testi içerir; önceki metre dönüşümü testleri korunur. Actions derleme sonucu ayrıca doğrulanmalıdır. Başarılı derleme dahi oyun içi yükleme, görsel QA, coğrafi doğruluk, yükseklik, rasterizasyon, spawn veya performans testi değildir.
