# Devredilen kararlar — 2026-09-08

Kullanıcı rutin proje kararlarını asistana devretti; tamamlanmamış işler bitmiş sayılmaz. Sabit Minecraft 1.21.1/NeoForge platformu, sıfır bütçe ve harici backend gerektirmeme korunur.

## Pilot

İstanbul / Kadıköy / Caferağa çevresinde yaklaşık 1 km² hedefleniyor. Kesin paket kutusu ve idari sınırlar veri incelemesinde doğrulanacak; şu anda veri yok, üretim veya gerçek kapsama iddiası yok. Pilot son hedefi küçültmez. Başlangıçta yerel paket akışı; oyun içinde otomatik coğrafi indirme kapalı. Ücretsiz açık verinin geliştirme sırasında alınması ve lisans değerlendirmesi ayrı yapılır. NPC ve ilan/doluluk bilgileri kurgusal kalır.

## Derleme engeli

9065a8a commit'inin Actions bildirimi gerçek hatayı gösterdi: Gradle :test görevi test kaynakları bulunmasına rağmen keşfedilebilir test bulamıyordu. Mevcut 15 kontrol main tabanlı bağımsız sözleşmelerdi. JUnit Jupiter 5.11.4 ve platform launcher 1.11.4 eklendi; JUnit adaptörü aynı sözleşmeleri standart test görevinde de yürütür. Test kapatılmadı, failOnNoDiscoveredTests gevşetilmedi. Bağımlılıklar yalnız test kapsamındadır ve mod JAR'ına dahil edilmez; kendi üçüncü taraf lisansları geçerlidir (JUnit EPL-2.0).

Bu değişiklik yeni CI koşusunda doğrulanmalıdır. JDK/Gradle kurulumu, Wrapper ve bağımsız sözleşmeler önceki koşuda geçti; mod yükleme/oynanabilirlik doğrulanmadı.

## Sonraki kapı

Önce CI/JAR kontrolü; sonra kayıt akışına zarar vermeyen istemci seçim ekranı artımı. Gerçek veri paketi yokken dünya oluşturma etkinleştirilmez. Doğrulanmamış özellikler, tam Dünya kapsamı veya hedef donanım performansı vaat edilmez.
