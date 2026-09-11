# Phosfe BKM TechPOS

Bağımsız olarak geliştirilen, çoklu ödeme cihazı SDK'sı destekleyen Android TechPOS uygulaması.

- Uygulama kimliği: `com.phosfe.bkmtechpos`
- UI: Kotlin + Jetpack Compose + Material 3
- Varyantlar: `standalone|ecr` x `datecs|newland|simulator` x `debug|release`
- Minimum Android: API 24
- Derleme JDK'sı: 11+

## Hızlı başlangıç

```powershell
.\gradlew.bat :app:testStandaloneSimulatorDebugUnitTest :app:assembleStandaloneSimulatorDebug
```

Gerçek cihaz varyantları, üretici SDK lisansı ve cihaza özel güvenli anahtar API'leri sağlandıktan sonra
`app/src/datecs` ve `app/src/newland` adaptörlerinde tamamlanmalıdır. Ortak ödeme ve protokol katmanı
üretici SDK sınıflarını doğrudan kullanmaz.

Host, port ve vendor değerleri `gradle.properties` ya da komut satırı `-P` parametreleriyle verilir.
Üretim sırları ve anahtarları kaynak depoya eklenmemelidir.

