# B1d — Tam ekran kaydırılabilir pilot harita

Kullanıcının sağladığı önceki `real life mod.zip` incelendi. Eski moddaki `ScreenEvent.Opening` ile fresh `CreateWorldScreen` değiştirme, tam ekran pan/zoom, imlece sabit zoom ve klasik akışa dönüş davranışı referans alındı. Eski `com.reallife` kodu veya lisansı belirsiz raster tile/mask görselleri Earthward'a kopyalanmadı.

Earthward artık fresh vanilla dünya oluşturma ekranı açılırken tam ekran pilot haritayı gösterir. Recreate durumunda vanilla ayarlarının kaybolmaması için `recreated` bayrağı kontrol edilir; alan bulunamazsa fail-closed davranılır ve vanilla ekran değiştirilmez. Klasik dünya oluşturma düğmesi tek seferlik bypass ile vanilla akışı açar.

Harita yalnız doğrulanmış harici paketin ODbL yol ve bina geometrisini gösterir. Paket okuma, hash kontrolü, JSON doğrulama ve metre projeksiyonu arka plan görevinde yürür. Kamera sürükleme, imlece sabit tekerlek zoom'u, +/−, haritayı sığdırma ve tıklanan yatay metre konumu vardır. Görünmeyen özellikler bounds ile elenir; kare başına en fazla 50.000 segment işlenir ve segment örneklemesi 256 adımla sınırlıdır.

Tıklanan nokta yalnız `requested spawn` önizlemesidir; güvenli spawn değildir ve diske yazılmaz. Dünya oluşturma düğmesi kasıtlı olarak kapalıdır. Yükseklik, su/engel kontrolü, chunk rasterizasyonu ve generator hazır olmadan açılmayacaktır.

7 yeni saf JUnit kamera/projeksiyon testi eklenmiştir. Kaynakların temel yapısal kontrolü yerelde geçti; Java/Minecraft API derlemesi Actions ile doğrulanmalıdır. Oyun içi yükleme, mouse etkileşimi, GUI ölçeği ve görsel QA henüz çalıştırılmadı.
