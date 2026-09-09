# Katalog testinin çalışma zamanı düzeltmesi

8e0e980 commit'inde JUnit XML ayrıntısı gerçek nedeni doğruladı: SelectionCatalogTest JSON kullanan üç testte `NoClassDefFoundError: com/google/gson/JsonParser` alıyordu. Java derlemesi geçti; izole JVM test çalışma ortamında Gson yoktu. Bu, harita verisi veya UI tıklama testi hatası değildi.

`testRuntimeOnly 'com.google.code.gson:gson:2.10.1'` eklendi. Minecraft oyunda Gson sağlar; test bağımlılığı mod JAR'ına gömülmez. Gson'un Apache-2.0 lisansı geçerlidir. Testler kaldırılmadı, exception beklentileri gevşetilmedi. Bu kontroller Minecraft sınıfları başlatmadığından NeoForge game-test desteği açılmış sayılmaz.

Kanıt: https://api.github.com/repos/Thevynex/earthward/check-runs/102190800086/annotations?per_page=100

Düzeltmenin yeni commit'teki sonucu Actions ile ayrıca doğrulanmalıdır. Oyun yükleme, görsel QA, gerçek harita ve dünya üretimi halen çalıştırılmadı.
