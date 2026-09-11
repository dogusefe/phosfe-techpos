# Paketleme ve release güvenliği

## Paket kimlikleri

- Release: `com.phosfe.bkmtechpos`
- Debug: `com.phosfe.bkmtechpos.debug`
- Demo: `com.phosfe.bkmtechpos.demo`

Standalone/ECR ve Datecs/Newland/Simulator ayrımı flavor boyutlarıyla yapılır. APK dosya adı
`PhosfeTechPOS-<MODE>-<VENDOR>-<BUILD>-v<VERSION>.apk` biçimindedir.

## İmzalama

`keystore.properties.example` dosyası `keystore.properties` adıyla kopyalanıp Phosfe release
keystore bilgileri girilir. Gerçek property ve JKS/keystore dosyaları `.gitignore` kapsamındadır.
Dosya bulunmazsa yerel release/demo doğrulaması debug anahtarıyla derlenebilir; sertifikasyona veya
dağıtıma gönderilen APK mutlaka Phosfe release anahtarıyla üretilmelidir.

## Build politikası

- Debug: debuggable, host wire log açık, düz Room DB.
- Demo: debugger kapalı, R8 obfuscation/resource shrink açık, test ayarları ve düz Room DB.
- Release: debugger/log/DB araçları kapalı, R8 obfuscation/resource shrink açık, SQLCipher DB.

SQLCipher parolası ilk açılışta 256 bit rastgele üretilir. Parola Android Keystore içindeki
`phosfe.techpos.database.wrapper.v1` anahtarıyla AES-GCM olarak sarılıp uygulama özel alanında tutulur.
ISO mesajları ve tam PAN ayrıca alan seviyesinde AES-GCM zarfıyla korunur.
