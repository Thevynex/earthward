# Doğrulama kaydı — 2026-09-08

Ortam: asistanın Linux çalışma alanı; kullanıcının bilgisayarı değildir. Java çalışma zamanı: Corretto OpenJDK 25.0.4. javac ve Gradle bulunamadı. Hedef Java 21 olduğu için mevcut Java çalışma zamanı yeterli değildir.

## Geçti

Python ile üretilen 10 yerel kaynak/dokümantasyon dosyasında 9 statik kontrol çalıştırıldı: TOML mod kimliği, mod sürümü, Minecraft/NeoForge tam sürüm aralıkları, ModDevGradle sabitlemesi, Gradle sabitlemesi, mod kimliği deseni, Java mod kimliği eşleşmesi, 15 test çağrısının varlığı ve MDK telif bildiriminin varlığı. Bunlar Java derleyicisi, API uyumluluğu veya oyun testi değildir.

Çıktı:
```
PASS: 9 static source/metadata checks; 10 files written. Java tests/build/game NOT RUN.
```

## Başarısız

Gradle dağıtımına bağlantı kontrolü gerçekten denendi: `curl -I -L --connect-timeout 10 --max-time 25 https://services.gradle.org/distributions/gradle-9.2.1-bin.zip`. curl hatası: `(6) Could not resolve host: services.gradle.org`. Bu ortamın ağ/DNS engelidir; kaynak kodunun derleme hatası değildir.

## Engellendi

`gradle --no-daemon contractTest build` gerçekten denendi; çıkış kodu 127, `gradle: command not found`. Derleyici çalışmadı. Gradle ve JDK 21 kurulumu/erişimi gerekiyor. 15 Java testi yazıldı fakat yürütülemedi. Gradle Wrapper henüz yok.

## Çalıştırılmadı

- Mod yükleme, Minecraft istemcisinin açılması, kayıt oluşturma/yeniden giriş: önce build, sonra docs/BOOTSTRAP.md adımları.
- Gerçek koordinat/mesafe, chunk birleşimi, iç mekân geçişi, mülk/NPC kalıcılığı, bozuk paket/kota senaryoları: ilgili sistemler ve pilot verisi henüz yok.
- Hedef donanım FPS, frametime, tick süresi, yükleme, RAM/VRAM/disk ölçümleri: hedef bilgisayarda test yapılmadı. Test senaryosu, çözünürlük/grafik/görüş mesafesi ve NPC sayısı kaydedilerek ileride ölçülmeli; eşikler kullanıcıyla kararlaştırılmadı.

B0 kabulü açık; B ve A aşamaları tamamlanmış sayılmaz. Dağıtılabilir JAR/release yayımlanmadı.
