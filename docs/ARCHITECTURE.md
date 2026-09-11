# Mimari kararlar

Bu depo, mevcut bir ödeme uygulamasının kaynak kodunu dönüştürmek yerine BKM Vendor Mesaj
Spesifikasyonu V3.2.1 ve cihaz üreticilerinin yayımladığı SDK sözleşmelerinden bağımsız olarak
oluşturulmuştur.

## Varyant modeli

`mode` boyutu terminalin bağımsız veya ECR tarafından yönetilen çalışma biçimini; `vendor` boyutu
ise derlemeye bağlanan donanım adaptörünü seçer. Böylece tek APK yalnızca bir üreticinin SDK'sını
taşır ve ortak ödeme katmanı sertifikasyon sonrası donanım değişikliklerinden korunur.

## Katman sınırları

- `domain`: Üreticiden ve Android SDK'sından bağımsız ödeme kavramları.
- `protocol`: ISO 8583 BCD/bitmap/alan kodlaması ve BER-TLV altyapısı.
- `hardware`: Kart, PIN, güvenli anahtar ve yazıcı portları.
- `src/<vendor>`: Üretici SDK adaptörleri. Ortak katmana ters bağımlılık kurulamaz.
- `ui`: Büyük dokunma hedefli Material 3 terminal arayüzü.

## Güvenlik sınırı

PIN, açık PAN, terminal anahtarları ve kriptografik ara değerler uygulama loglarına veya kalıcı
depolamaya yazılmaz. Üretim anahtarları BuildConfig içinde tutulmamalı; cihazın PCI uyumlu güvenli
işlemcisine RKL/TR-31/TR-34 akışıyla yüklenmelidir.

