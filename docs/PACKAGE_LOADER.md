# B1b — Yerel paket doğrulama ve pilot metre dönüşümü

Gerçek pilot veri koşusu 6df380e commit'inde başarılı oldu: 2.177 bina dış çizgisi ve 434 yol merkez çizgisi içeren ayrı ODbL artifact üretildi. Bu sayılar kaynak doğruluğu, eksiksizlik veya dünya üretimi kanıtı değildir.

Bu artım mod tarafına salt-okuma paket denetleyicisi ekler. Beklenen konum `<oyun-dizini>/earthward/packages/<package-id>/` ve gerekli dosyalar `manifest.json` ile `geometry.json`dır. Kimlik, şema, EPSG:4326, koordinat sırası/birimi, 1 metre/blok hedefi, dosya boyutu ve SHA-256 doğrulanır. Symlink ve dizin geçişi reddedilir. Geometri bu aşamada RAM'e ayrıştırılmaz veya kayıtlara yazılmaz. Denetim istemci çizim iş parçacığı dışında yürütülür.

Seçim ekranı eksik, geçersiz ve hash'i doğrulanmış geometri paketini ayırır. Doğrulanmış paket bile dünya oluşturmayı açmaz; manifest v1 `generation_ready=true` değerini kabul etmez. Bu, geometri paketinin tek başına yükseklik, spawn veya oynanabilir dünya anlamına gelmesini engeller.

`LocalMetricProjection`, Caferağa gibi küçük pilotlarda EPSG:4326 koordinatlarını yerel metre koordinatlarına çevirmek için equirectangular/tangent yaklaşımı sağlar: +X doğu, +Z güney. Girdi merkezden en fazla iki dereceyle sınırlandırılır ve ±85° dışındaki merkezler reddedilir. Bu küresel projeksiyon değildir; kutuplar, tarih çizgisi ve Minecraft dünya sınırları çözülmedi. Geometri henüz bloklara rasterize edilmez ve düşey koordinat üretilmez.

11 yeni JUnit testi paket yokluğu, hash/kimlik/generation-ready reddi, dizin geçişi ve pilot metre eksenlerini kapsar. Oyun içi yükleme, menü tıklama ve görsel QA henüz çalıştırılmadı; Actions sonucu ayrıca doğrulanmalıdır.
