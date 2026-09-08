# Earthward

Minecraft Java Edition **1.21.1 / NeoForge** için geliştirme başlangıcı. Mod kimliği: `earthward`.

**Henüz oynanabilir bir mod veya gerçek dünya üreticisi teslim edilmedi.** İlk mod kaynakları `dev/bootstrap` dalında hazırlanır. Tamamlanmış sürüm veya performans garantisi yoktur.

## Hedef ve kapsam

Nihai hedef dünya çapında bölgesel veri desteği; hedef ölçek 1 blok = 1 metre. Bu hedef, küresel 1:1 doğruluğun kanıtlandığı anlamına gelmez. İlk sürüm tek oyunculu; ücretli servis veya ayrı sunucu gerektirmeyecek. Tek oyunculunun yerleşik sunucusu kullanılabilir.

Ülkeye uygun idari hiyerarşi üzerinden haritadan başlangıç seçimi, kaynakta bulunan arazi/yol/bina dışları, işleve uygun sentetik iç mekânlar, ayrı mülk durumları ve sınırlı aktif NPC nüfusu planlanıyor. Bunlar henüz uygulanmadı.

## Çalışma varsayımları

NPC'ler kurgusal; gerçek kişisel veriler kullanılmaz. Doluluk ve ilanlar oyun simülasyonudur, gerçek ilan/kişi bilgisi değildir. Gerçek pilot bölge ve oyun içi internet izni henüz belirlenmedi. Shader, araç fiziği ve kapsamlı ekonomi ilk iskeletin kapsamında değildir.

## Aşamalar

- A: Pilot sınırı, veri lisansları, koordinat/yükseklik kararları ve risklerin doğrulanması. Açık.
- B: Derleme, mod yükleme ve harita seçim akışı. Açık.
- C: Gerçek bölgeyi üretme, güvenli spawn, iç mekân/mülk/NPC ve kayıt dönüş testi. Başlamadı.
- D: Tekrarlanabilir hedef donanım ölçümü ve bölgesel genişleme. Başlamadı.

## Lisans

Kendi proje kodumuz açık kaynak değildir. `LICENSE` yalnızca resmi derlenmiş modu kişisel, ticari olmayan oyun amacıyla kullanma izni verir. Kaynakları yeniden kullanma veya yeniden dağıtma izni vermez. Herkese açık GitHub deposu görüntülenebilir ve GitHub koşullarınca fork edilebilir; kopyalamayı teknik olarak engelleme garantisi yoktur. Üçüncü tarafların kendi lisansları saklıdır. Minecraft/NeoForge dağıtılmaz.

## Güvenlik ve veri

Büyük coğrafi veriler, önbellekler, kayıtlar, sırlar ve lisansı belirsiz varlıklar depoya eklenmez. Gerçek veriden alınan, çıkarılan ve bilinmeyen ayrıntılar ayrılmalıdır. Veri yokken gerçeğe uygun bölge üretildiği iddia edilmeyecektir.
