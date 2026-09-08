# B0 — Kaynak iskeleti; B aşaması tamamlanmadı

## Bu dalda bulunanlar

- NeoForge mod giriş noktası ve yükleme günlüğü işaretçisi.
- Dünya/istemci API'lerinden bağımsız SpawnSelection girdi modeli.
- 15 sentetik girdi sözleşmesi testi. Bunlar gerçek konum, spawn güvenliği veya projeksiyon testi değildir.
- Tam sürümle sınırlandırılmış metadata ve build ayarları.

Harita UI, dünya oluşturma müdahalesi, gerçek veri paketi okuyucusu, ağ/önbellek, arazi/yol/bina/iç mekân, mülk, NPC ve kayıt kodu YOK. Eski kayıtlara dokunacak kod eklenmedi; kayıt güvenliği yine de oyun içinde test edilmedi.

SpawnSelection yalnızca sözdizimsel kontrol yapar. Enlem [-90,90], boylam [-180,180) kabul edilir. +180 sessizce dönüştürülmez; reddedilir. Kutupların girdi olarak kabulü orada üretim desteği demek değildir. Paket varlığı, kapsama, idari hiyerarşi, yükseklik ve güvenli spawn ayrıca doğrulanmalıdır. Projeksiyon seçilmedi.

## Gereksinimler ve komutlar

- Minecraft 1.21.1, NeoForge 21.1.249.
- 64-bit JDK 21 (yalnız JRE değil); JAVA_HOME bunu göstermeli. JDK üretici/yama sürümü henüz test edilip sabitlenmedi.
- Gradle 9.2.1; ModDevGradle 2.0.146 build içinde sabit.
- İlk geliştirme kurulumu ücretsiz bağımlılıkları indirebilmek için internet gerektirir. Bu, oyun içi coğrafi veri indirme izni değildir.

```sh
git clone --branch dev/bootstrap https://github.com/Thevynex/earthward.git
cd earthward
java -version
javac -version
gradle --version
gradle --no-daemon contractTest build
gradle --no-daemon runClient
```

Bu komutlar JDK 21 ve Gradle 9.2.1 PATH üzerinde kuruluysa Windows PowerShell ve Linux/macOS terminalinde kullanılabilir. Dağıtılabilir JAR ancak build başarılıysa build/libs/earthward-0.1.0-dev.1.jar yolunda oluşur. O JAR, Minecraft 1.21.1 + NeoForge 21.1.249 profilinin mods klasörüne konur. Kaynak ZIP mod olarak kurulmaz.

Gradle Wrapper henüz teslim edilmedi: ortam Gradle indiremiyor. Ağ erişimli geliştirme ortamında `gradle wrapper --gradle-version 9.2.1 --distribution-type bin` ile resmi wrapper üretilip checksum doğrulaması ve lisans bildirimi tamamlandıktan sonra ayrı commit yapılmalı. Şu anda ./gradlew komutu varmış gibi kullanılmamalı.

Gradle heap üst sınırı 2 GiB'dir; bu oyun RAM tahsisi veya hedef donanım performans ayarı değildir. 32 GB belleğin tamamını Java'ya ayırmayın. Oyun bellek bütçesi ölçümden sonra seçilecek.

## Kabul ve doğrulama

1. contractTest başarılı olmalı ve 15 sentetik sözleşme yazdırmalı.
2. build başarılı olmalı; JAR metadata/giriş noktası kontrol edilmeli.
3. runClient açılmalı; Mods listesinde Earthward ve latest.log içinde `Earthward bootstrap loaded` görülmeli.
4. Ayrı geçici vanilla test dünyası oluşturma, çıkma ve tekrar açma denenmeli. Önemli kayıtlarla test etmeyin; önce yedek alın.

Derleme başarılı olsa bile 3 ve 4 otomatik geçmiş sayılmaz. Harita UI henüz yoktur; B aşaması bu nedenle açık kalır.

## Sonraki adım

JDK/Gradle erişimli ortamda derleme ve mod yükleme kontrolü. Ardından veri yok durumunu da içeren seçim UI akışı; gerçek pilot sınırı ve lisans incelemesi paralelde netleştirilecek. Dünya kayıt formatına dokunmadan önce geçiş/yeniden giriş testleri yazılacak.
