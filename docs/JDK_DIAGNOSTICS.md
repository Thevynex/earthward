# JDK tanı ve sürüm kontrolü — 2026-09-08

Son gözlenen koşu JDK kurulumunda başarısız; Gradle/testler atlanmıştı.

Kod incelemesinde önceki sürüm kontrolünün `21.0.12.1+1-LTS` değerini reddettiği görüldü. Bu davranış yerel testle doğrulandı ve aynı sürümün isteğe bağlı -LTS etiketi kabul edildi. Bu tespit önceki koşunun kesin kök nedeni olduğu anlamına gelmez; önceki ayrıntılı log elde edilemedi.

Kurulum tools/install_jdk.sh dosyasına taşındı. Tam JDK arşiv adresi ve SHA-256 değişmedi. İndirme, checksum, çıkarma, çalıştırma, derleyici ve ortam adımları ayrı tanımlanır. Hatalı alt adım GitHub error annotation içinde görünür. Günlük ci-output/jdk-install.log dosyasına en baştan yazılır; kurulum erken başarısız olsa da artifact yüklemesi dosyayı bulabilir. Gizli değerler veya ortamın tamamı yazdırılmaz.

Yerel test: YAML ve Bash sözdizimi geçti. Altı sürüm girdisi denenerek düz ve -LTS biçimleri kabul edildi; yanlış build numarası, erken erişim etiketi, başka ana sürüm ve boş girdi reddedildi. Gerçek JDK indirme/kurulum ve Actions derleme sonucu bunlardan ayrı kontrol edilmelidir. Mod sürümleri değişmedi. PR birleştirilmedi.
