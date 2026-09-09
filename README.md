# Earthward

Minecraft Java Edition **1.21.1 / NeoForge 21.1.249** için gerçek dünya modu geliştirmesi. Mod kimliği: `earthward`. Aktif dal: `dev/bootstrap`.

**Tamamlanmış bir gerçek dünya modu değildir.** `fb452721` commit'inde iki Actions kontrolü geçti: mod derleme, sözleşmeler/JUnit ve JAR doğrulaması. Bu commit'in doğrulanmış geliştirme JAR'ı Actions artifact'larında bulunur; oyun yükleme testi anlamına gelmez.

Sonraki `9cfbba50` artımı bölge seçim önizlemesi kaynaklarını ekledi; Java derleme sonrasında yeni JUnit testlerinde hata görüldü. En yeni dalın durumunu PR #1 kontrollerinden doğrulayın; eski yeşil koşu yeni commit'in geçtiğini göstermez.

## Hedef ve mevcut kapsam

Nihai hedef dünya çapında bölgesel veri desteği; hedef ölçek 1 blok = 1 metre. Küresel 1:1 doğruluk kanıtlanmadı. İlk sürüm tek oyunculu; ücretli servis veya ayrı sunucu gerektirmeyecek. Minecraft'ın yerleşik tek oyunculu sunucusu kullanılabilir.

Önizleme kaynaklarında dünya listesinden açılan, JSON katalog üzerinden ilerleyen Ülke/İl/İlçe/Mahalle akışı ve eksik veri mesajı var. Tek pilot yolu sunulur; henüz gerçek harita, doğuş seçimi veya coğrafi dünya üretimi yok. Dünya oluşturma kapalı; vanilla kayıt akışı değiştirilmez. Oyun içi davranış ve görsel QA henüz çalıştırılmadı.

Arazi/yol/bina dışları, işlevsel sentetik iç mekânlar, ayrı mülk durumları ve sınırlı aktif NPC nüfusu sonraki işlerdir; uygulanmış sayılmaz.

## Devredilen kararlar

Pilot olarak İstanbul / Kadıköy / Caferağa çevresinde yaklaşık 1 km² seçildi; kesin paket sınırı veri incelemesiyle kaydedilecek. İlk akış yerel paket, oyun içi otomatik coğrafi indirme kapalı. NPC, doluluk ve ilanlar kurgusal. Shader, araç fiziği ve kapsamlı ekonomi ilk iskelette yok.

## Aşamalar

- A: Pilot veri/lisans/projeksiyon ve yükseklik doğrulaması açık. `docs/DATA_POLICY.md`.
- B: İlk derleme/JAR kapısı geçti; yeni UI artımının testleri ve gerçek oyun yükleme/görsel QA açık. `docs/SELECTION_PREVIEW.md`.
- C: Gerçek bölge, güvenli spawn, iç mekân/mülk/NPC ve kayıt dönüş testi uygulanmadı.
- D: Hedef donanım ölçümü ve bölgesel genişleme uygulanmadı.

## Lisans

Kendi kodumuz açık kaynak değildir. `LICENSE` resmi derlenmiş modu kişisel, ticari olmayan oyun amacıyla kullanmaya izin verir; kaynakları yeniden kullanma/dağıtma izni vermez. Herkese açık GitHub deposu görüntülenebilir ve GitHub koşullarınca fork edilebilir; kopyalamayı teknik olarak engelleme garantisi yoktur. Üçüncü tarafların kendi lisansları saklıdır. Minecraft/NeoForge dağıtılmaz. OSM verisinin ODbL hakları kendi kod lisansımızla daraltılamaz.

## Güvenlik ve veri

Büyük coğrafi veriler, önbellekler, kayıtlar, sırlar ve lisansı belirsiz varlıklar depoya eklenmez. Kaynaktan alınan, çıkarılan ve bilinmeyen ayrıntılar ayrılır. Veri yokken gerçeğe uygun bölge üretildiği iddia edilmez.
