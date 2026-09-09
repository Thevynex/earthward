# A1 — Gerçek geometri için sınırlı paket hazırlama

## Kapsam

Bu artım mod içi dünya üretimi değildir. Geliştirme sırasında gerçek OSM yol/bina geometrisi edinip yerel paket hazırlamak içindir. İlk dikdörtgen Caferağa çevresi hedefli seçildi: güney 40.9803, batı 29.0208, kuzey 40.9893, doğu 29.0328. Yaklaşık 1 km² hedefidir; doğrulanmış idari mahalle sınırı veya tam kapsama iddiası değildir.

Minecraft 1.21.1 / NeoForge 21.1.249 değişmedi. Oyun içinde otomatik indirme yok. Standart OSM raster tile servisi ve kalıcı geliştirici backend'i kullanılmaz.

## Uygulananlar

- Tek, süre/boyut/bölge sınırı olan Overpass sorgusu. Timeout 45 saniye, bildirilen sunucu bellek sınırı 64 MiB, indirme sınırı 16 MiB; 20.000 öğe ve 200.000 nokta sınırı. HTTP 429/504 dahil hata durumunda otomatik tekrar yok.
- Seçilen way geometrileri, gerçek kaynak kimlikleri ve yalnız fiziksel niteliklerin izinli listesi. İsim, adres, telefon, işletmeci ve katkıcı bilgileri pakete taşınmaz.
- Eksik/bozuk geometri, kapanmayan bina, desteklenmeyen multipolygon relation ve yol alanları sayılır; uydurma geometri eklenmez.
- EPSG:4326 boylam/enlem derece koordinatları korunur. Bunlar henüz metre/blok koordinatları değildir; küresel projeksiyon uygulanmadı. Bina çizgisi kapanışı kontrol edilir, kendini kesme/topoloji doğrulanmaz.
- Pakette kaynak anlık görüntü zamanı, edinme zamanı, SHA-256, atıf/lisans, sayımlar ve eksik katmanlar bulunur. Mevcut paket dizini değiştirilmez. Modun kayıt dosyalarına yazım yok.

## Dosyalar

Başarılı gerçek koşu `earthward-pilot-geometry-<commit>` Actions artifact'ı üretir:

- `manifest.json`: sürüm, kapsama isteği, kaynak/lisans/tarih/hash ve sınırlamalar.
- `geometry.json`: seçilen gerçek bina dış çizgileri ve yol merkez çizgileri; görüntü veya Minecraft dünyası değildir.
- `ATTRIBUTION.txt`: © OpenStreetMap contributors, ODbL-1.0 ve değişiklik bildirimi.
- `source-query.overpass`: kullanılan sorgu.

Geometri dosyası ODbL altında kalır; Earthward'ın kısıtlı kod lisansı bu veriye uygulanamaz. İlk artifact yedi gün saklanır; kalıcı dağıtım sistemi henüz yok. Mod bu paketi henüz okuyup haritada göstermiyor; mods klasörüne koyulacak bir JAR değildir.

## Komutlar

Sentetik sözleşmeler:

```sh
python3 -m unittest discover -s tools -p 'test_prepare_pilot.py' -v
```

Geliştirme sırasında gerçek veri için (CI çalıştırır):

```sh
python3 tools/prepare_pilot.py --area data/pilot_area.json --output pilot-output --fetch
```

Ağsız, kullanıcının sağladığı yerel Overpass JSON için:

```sh
python3 tools/prepare_pilot.py --area data/pilot_area.json --output pilot-output --input source.json
```

Yerel girişin menşei otomatik doğrulanmış sayılmaz; manifest bunu açıkça belirtir. Başka bir bölgenin JSON'u bu araca verilirse alan tanımıyla örtüştüğü henüz denetlenmez; yerel girişte operatör kontrolü gerekir. Gelecek mod içi yükleyici bunu ayrıca doğrulamalıdır.

## Test durumu ve CI

17 sentetik Python testi yerelde geçti; gerçek veri doğruluğu testi değildir. YAML, salt-okuma depo izni ve gerçek sorgunun yalnız dev/bootstrap push koşusunda çalışması yerelde kontrol edildi. Actions PR koşusunda yalnız sentetik testler çalışır; gerçek indirme ve artifact adımlarının atlanması gerçek veri başarısı sayılmaz. Gerçek push koşusunun sonucu ayrıca kontrol edilmelidir.

Arazi yüksekliği, dış cephe, gerçek bina yüksekliği kapsaması, spawn, dünya üretimi, oyun yükleme/görsel QA ve performans halen yok veya çalıştırılmadı. Dünya oluşturma açılmayacak.

## Kaynaklar

- https://www.openstreetmap.org/copyright
- https://opendatacommons.org/licenses/odbl/1-0/
- https://operations.osmfoundation.org/policies/tiles/
- https://dev.overpass-api.de/overpass-doc/en/preface/commons.html
