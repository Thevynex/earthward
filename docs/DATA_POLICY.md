# Çevrimdışı pilot veri politikası

İnceleme tarihi: 2026-09-08. Bu belge veri indirildiği anlamına gelmez.

## Seçilen yaklaşım

Pilot: İstanbul / Kadıköy / Caferağa çevresi, yaklaşık 1 km². İdari sınır ile dikdörtgen paket kutusu aynı şey değildir; kesin kapsama pakette ayrıca kaydedilecek. İlk istemci otomatik ağ isteği yapmaz. Geliştirmede hazırlanmış, kaynak/lisans/zaman bilgili yerel paketler kullanılacak. Sıfır bütçe ve ayrı sunucu gerektirmeme korunur.

## OpenStreetMap

OSM yol/bina geometrileri için adaydır, metre doğruluğu ve tam kapsama garantisi değildir. Atıf ve ODbL koşulları veri paketlerine uygulanır. Earthward'ın kısıtlı kaynak kod lisansı OSM verisine uygulanamaz; üçüncü taraf veri haklarını daraltamayız. Türetilmiş veri tabanı ile üretilmiş çıktı ayrımı ve paylaşım yükümlülükleri dağıtımdan önce ayrıca değerlendirilmelidir.

Kaynak: https://www.openstreetmap.org/copyright
Lisans: https://opendatacommons.org/licenses/odbl/1-0/

Standart OSM raster tile servisi çevrimdışı toplu indirme için kullanılmayacak. Politika bunu açıkça yasaklar. Gelecek harita görünümü izinli yerel vektör geometrilerinden üretilecek; sahte taban haritası çizilmeyecek.

Kaynak: https://operations.osmfoundation.org/policies/tiles/

Overpass ortak sunucuları kalıcı oyun backend'i olmayacak; dünyanın kutu kutu taranması yapılmayacak. Küçük geliştirme sorguları sınırlı ve önbellekli olabilir. HTTP 429/504 engel olarak kaydedilmeli; yoğun tekrar/kimlik değiştirme yapılmamalı. Dünya genişlemesi yeniden dağıtılabilir bölgesel extract/paket yaklaşımı gerektirir.

Kaynak: https://dev.overpass-api.de/overpass-doc/en/preface/commons.html

## Eksik veri

OSM tek başına yükseklik modeli sağlamaz. Arazi yüksekliği, bina yüksekliği ve dış cephe dokusu otomatik olarak bilinmiş sayılmaz. Eksik/inferred değerler ayrılır; bu aşamada düz arazi gerçek arazi yerine sunulmaz. Gerçek kişi adları, iletişim bilgileri ve kullanıcı kimlikleri oyun nüfusu için kullanılmaz; doluluk/ilanlar daima kurgusaldır.

Paketin sürümü, yatay/düşey referansı, seçilen sınırı, sağlayıcı URL'si, verinin zamanı, edinme zamanı, dosya hash'i, atıf/lisans ve eksik katmanları tutulmalıdır. Gelecek coğrafi güncellemeler oyuncunun değişikliklerini otomatik olarak ezmemelidir.
