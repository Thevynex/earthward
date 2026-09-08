# Başlangıç kararları — 2026-09-08

## Onaylananlar

- Thevynex/earthward: yeni, herkese açık depo.
- Kendi koduna kısıtlı kullanım lisansı; üçüncü taraf hakları korunur.
- main başlangıç dosyaları; normal geliştirme dalı dev/bootstrap.
- Minecraft 1.21.1 ve NeoForge değiştirilmeyecek.

## İlk teknik sınırlar

Mevcut dünya oluşturma ve kayıt akışına bu artımda müdahale edilmeyecek. Yeni UI, üretim ve kayıt sürümleme testleri olmadan eski kayıtların yerine geçirilmez.

Küresel WGS84 koordinatını doğrudan blok koordinatı sanmak yanlış olur. Yerel projeksiyon ve bölgesel geçiş seçenekleri incelenecek; kutuplar, tarih değiştirme çizgisi, Minecraft yatay/dikey sınırları ve yükseklik referansı açık kararlardır. Küresel tek düzleme hatasız 1:1 dönüşüm vaat edilmiyor. Ölçek veya yükseklik sessizce sıkıştırılmayacak.

Yerel paket öncelikli altyapı öneriliyor; oyun içi indirme kullanıcı iznine bağlı. Paket eksikse üretim durmalı ve neden görünmeli. Veri paketi kaynak/tarih/lisans, yükseklik referansı, kapsama sınırı ve bilinen eksikleri içermeli. Ağ/ayrıştırma işçileri oyun dünyasını doğrudan değiştirmemeli. Kayda yazım uygun oyun iş parçacığı bağlamında yapılmalı.

Kalıcı bölge/özellik/üretim sürümü kimlikleri kullanılacak. Yeni coğrafi veri oyuncu değişikliklerinin üstüne otomatik yazılmayacak. Bina/bağımsız bölüm kimliği, kullanım, doluluk ve ilan durumu ayrı tutulacak. Yakında aktif NPC, uzakta hafif kalıcı durum öneriliyor; tüm dünya entity olarak yüklenmeyecek.

## Engeller

- Pilot konumu ve sınırı seçilmedi.
- Oyun içi internetten ücretsiz veri indirme izni bilinmiyor.
- Coğrafi kaynak/lisans değerlendirmesi yapılmadı; OSM yalnızca adaydır.
- Bu çalışma ortamında Java 25 çalışma zamanı var, javac ve JDK 21 yok; services.gradle.org DNS çözümlemesi başarısız. Derleme engellendi.
- Hedef bilgisayarda test yapılmadı. İşletim sistemi, çözünürlük ve performans kabul eşikleri henüz net değil.

Bu belge uygulanmış özellik listesi veya tamamlanmış A aşaması değildir.
