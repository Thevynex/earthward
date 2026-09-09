# Birleşik geliştirme test paketi

`Earthward pilot data` iş akışının başarılı dev/bootstrap push veya manuel çalıştırması `earthward-test-bundle-<commit>` artifact'ını üretir. Bu bir release değildir.

Artifact açıldığında `mods/earthward-0.1.0-dev.1.jar`, JAR checksum'u, `earthward/packages/tr_istanbul_kadikoy_caferaga/` altında gerçek ODbL pilot geometrisi, veri manifesti/atıfı ve tüm dosyalar için `SHA256SUMS.txt` bulunur. Aynı koşuda mod yeniden derlenip JAR denetlenir; böylece eski bir JAR yeni veriyle yanlışlıkla paketlenmez.

Kurulum: ZIP içeriğini mevcut dünyaların kullanılmadığı yeni ve ayrı Minecraft oyun dizinine kopyala. Minecraft Java 1.21.1, NeoForge 21.1.249 ve Java 21 kullan. Bu paket yalnız modun açılmasını ve seçim ekranında harici veri paketinin hash doğrulamasını denemeye yarar.

Dünya üretimi kapalı kalır. Geometri henüz bloklara dönüştürülmez; arazi yüksekliği, güvenli spawn, bina/yol yerleştirme, iç mekân, NPC ve performans yoktur. Başarılı Actions sonucu oyun içinde yükleme veya görsel QA yerine geçmez.

PR doğrulamasında gerçek ağ, derleme ve bundle adımları kasıtlı olarak atlanır; yalnız sentetik paketleme testleri çalışır. Atlanan adımlar geçti sayılmaz. Gerçek paket yalnız dev/bootstrap push veya manuel dispatch koşusunda oluşturulur; otomatik tekrar sorgusu yoktur.
