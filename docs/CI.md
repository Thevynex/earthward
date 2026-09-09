# GitHub Actions — Earthward CI

## Kapsam

.github/workflows/ci.yml dosyası main/dev/bootstrap push olaylarında, main hedefli pull request'lerde ve workflow_dispatch ile çalışacak şekilde tanımlandı. Dosya dev/bootstrap dalındadır; GitHub'ın Run workflow düğmesinin görünmesi için workflow'un varsayılan dala da alınması gerekir. Dosya yükleme başarısı, koşunun başlaması veya geçmesi değildir.

Actions sayfası: https://github.com/Thevynex/earthward/actions

- Standart ubuntu-24.04 runner; ücretli büyük runner veya dış sunucu yok.
- Eclipse Temurin JDK 21.0.12.1+1; Gradle 9.2.1; Minecraft 1.21.1; NeoForge 21.1.249; ModDevGradle 2.0.146.
- JDK yayını: https://github.com/adoptium/temurin21-binaries/releases/tag/jdk-21.0.12.1%2B1
- JDK, resmi OpenJDK21U-jdk_x64_linux_hotspot_21.0.12.1_1.tar.gz arşivinden indirilir. Yayın varlığı metadata'sından doğrulanan SHA-256: ce79869e1307ed8ee1e2baa86a412b1eb5b75d10a01006d788a6f968bcfaee94. Çıkartmadan önce özet doğrulanır; sonra gerçek çalışma zamanı sürümü tam olarak kontrol edilir ve JAVA_HOME/PATH ayarlanır.
- Kullanılan GitHub action'ları tam commit kimliklerine sabitlendi. Runner imajı ve transitif bağımlılıkların tümü bit düzeyinde sabitlenmiş değildir.
- İş süresi en çok 45 dakika, Gradle en çok 2 worker, Gradle heap üst sınırı 2 GiB.
- Workflow token'ı yalnız contents:read. Otomatik commit, merge, release veya Build Scan yok. PAT sırrı gerekmez. Coğrafi veri indirilmez. Gradle cache kapalı; artifact saklama 7 gün.

## Adımlar

1. SHA-256 doğrulamalı tam sürüm JDK ve Gradle kurulumu; ortam sürümlerinin kaydı.
2. Python 3.11+ ile 9 statik kaynak kontrolü.
3. Minecraft bağımlılıkları olmadan javac/java ile 15 sentetik girdi sözleşmesi.
4. Resmi Gradle 9.2.1 dağıtım SHA-256 değeriyle geçici Wrapper üretimi.
5. clean contractTest build. Bash pipefail sayesinde tee loglaması test/derleme hatalarını gizlemez.
6. JAR bütünlüğü, metadata, tam bağımlılık aralıkları, lisans bildirimleri ve Java 21 bytecode kontrolü. Beklenmeyen/test sınıfları hata verir.
7. Yalnız başarılı kontrollerden sonra JAR ve SHA-256 dosyasını earthward-bootstrap-<commit> artifact'ına yükleme.
8. Hata halinde de mevcut diagnostic loglarını earthward-ci-logs-<commit> artifact'ında saklama.

## İndirme ve kurulum

Actions → ilgili koşu → Artifacts → earthward-bootstrap-<commit>. İndirmek için GitHub oturumu gerekebilir. ZIP'i açıp JAR'ı Minecraft 1.21.1 / NeoForge 21.1.249 profilinin mods klasörüne koy. Bu yalnız bootstrap modudur; oynanabilir gerçek dünya sürümü değildir. Başarısız koşuda mod artifact'ı yoktur. Eski başarılı koşunun artifact'ını yeni commit'in sonucu sanma.

Wrapper geçici runner üzerinde üretilir, depoya geri yazılmaz. Yerel checkout hâlâ kurulu Gradle 9.2.1 gerektirir.

## 2026-09-08 JDK kurulum düzeltmesi

İlk push ve PR kontrolleri Setup JDK adımında başarısız oldu. Kullanıcı günlüğü: `The string '21.0.12.1+1' is not valid SemVer notation for a Java version.` Dört bileşenli Java sürümü setup-java java-version girişinde kullanılmıştı. Hata mod derlemesine ulaşmadan oluştu.

setup-java sürüm çözümleme adımı kaldırıldı. JDK'yı belirsiz bir '21' aralığına gevşetmek yerine aynı resmi sürümün sabit URL/özetli arşivi kuruluyor. Minecraft/NeoForge sürümleri değişmedi. YAML/shell sözdizimi ve tam çalışma zamanı sürüm kontrolünün doğru girdiyi kabul edip iki yanlış girdiyi reddetmesi yerel ortamda test edildi. JDK kurulumu ve yeni Actions koşusunun başarı sonucu bu yerel testlerle doğrulanmış değildir.

## Doğrulama sınırı

Gerçek durum koşu adımlarından okunmalı; atlanan adımlar geçti sayılmaz. Minecraft istemcisi/mod yükleme, harita akışı, kayıt dönüşü, gerçek koordinatlar, chunk birleşimleri, NPC/mülk durumları ve hedef bilgisayar FPS/VRAM testleri bu CI'da YOK. Henüz uygulanmamış özellikler test edilmiş sayılmaz. docs/TEST_RESULTS.md önceki yerel ortam denemesinin tarihsel kaydıdır.
