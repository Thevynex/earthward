# GitHub Actions — Earthward CI

## Kapsam

.github/workflows/ci.yml dosyası main/dev/bootstrap push olaylarında, main hedefli pull request'lerde ve workflow_dispatch ile çalışacak şekilde tanımlandı. Dosya dev/bootstrap dalındadır; GitHub'ın Run workflow düğmesinin görünmesi için workflow'un varsayılan dala da alınması gerekir. İlk çalıştırma push olayı üzerinden beklenir; dosya yükleme başarısı, koşunun başlaması veya geçmesi değildir.

Actions sayfası: https://github.com/Thevynex/earthward/actions

- Standart ubuntu-24.04 runner; ücretli büyük runner veya dış sunucu yok.
- Eclipse Temurin JDK 21.0.12.1+1; Gradle 9.2.1; Minecraft 1.21.1; NeoForge 21.1.249; ModDevGradle 2.0.146.
- JDK yayını doğrulandı: https://github.com/adoptium/temurin21-binaries/releases/tag/jdk-21.0.12.1%2B1
- Kullanılan GitHub action'ları sağlayıcı depolarında doğrulanan tam commit kimliklerine sabitlendi. Runner imajı ve transitif bağımlılıkların tümü bit düzeyinde sabitlenmiş değildir.
- İş süresi en çok 45 dakika, Gradle en çok 2 worker, Gradle heap üst sınırı proje ayarında 2 GiB.
- Workflow token'ı yalnız contents:read. Depoya otomatik commit, merge, release veya Build Scan gönderimi yok. PAT sırrı eklemek gerekmez. Coğrafi veri indirilmez. Gradle cache kapalı; artifact saklama 7 gün.

## Adımlar

1. Java/Gradle sürümlerini kur ve ortam sürümlerini kaydet.
2. Python 3.11+ ile 9 statik kaynak kontrolünü çalıştır (runner Python'u kullanılır).
3. Minecraft bağımlılıkları olmadan javac/java ile 15 sentetik girdi sözleşmesini çalıştır.
4. Gradle 9.2.1 dağıtımının resmi SHA-256 değerini al; geçici runner üzerinde Wrapper üret ve onunla devam et.
5. clean contractTest build çalıştır. Bash pipefail sayesinde tee loglaması test/derleme hatalarını gizlemez.
6. JAR ZIP bütünlüğü, mod kimliği/sürümü, tam Minecraft/NeoForge bağımlılık aralıkları, lisans bildirimleri ve Java 21 bytecode denetimi. Test sınıfları veya beklenmeyen üçüncü taraf sınıfları mod JAR'ında bulunursa hata verilir.
7. Yalnız başarılı kontrollerden sonra JAR ve SHA-256 dosyasını earthward-bootstrap-<commit> artifact'ına yükle.
8. Hata durumunda da mevcut diagnostic loglarını earthward-ci-logs-<commit> artifact'ında sakla.

## İndirme ve kurulum

Actions → ilgili koşu → Artifacts → earthward-bootstrap-<commit>. İndirmek için GitHub oturumu gerekebilir. ZIP'i açıp JAR'ı Minecraft 1.21.1 / NeoForge 21.1.249 profilinin mods klasörüne koy. Bu yalnız bootstrap modudur; oynanabilir gerçek dünya sürümü değildir. Başarısız koşuda mod artifact'ı yoktur; adım ve logları incele. Eski başarılı koşunun artifact'ını yeni commit'in sonucu sanma.

Wrapper bu akışta geçici runner üzerinde üretilir, depoya geri yazılmaz. Yerel checkout hâlâ kurulu Gradle 9.2.1 gerektirir; ./gradlew repoda varmış gibi kabul edilmemeli.

## Doğrulama sınırı

Workflow YAML yapısı, salt okunur izinler, timeout, action commit sabitlemeleri, shell ve Python sözdizimi yerel ortamda kontrol edildi. Bu belge ilk Actions koşusunun başarılı olduğunu iddia etmez. Gerçek durum koşu adımlarından okunmalı; atlanan adımlar geçti sayılmaz.

Minecraft istemcisinin açılması/mod yüklenmesi, harita seçim akışı, kayıt dönüşü, gerçek koordinatlar, chunk birleşimleri, NPC/mülk durumları ve hedef bilgisayar FPS/VRAM testleri bu CI'da YOK. Henüz uygulanmamış özelliklerin testleri varmış gibi sayılmaz. Bunlar ayrı oyun içi test artımlarında eklenecek. docs/TEST_RESULTS.md önceki yerel ortam denemesinin tarihsel kaydıdır; yeni CI koşularının yerine geçmez.
