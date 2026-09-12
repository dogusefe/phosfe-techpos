# Müşteri profilleri

Her müşteri `customers/<id>/profile.properties` dosyasıyla tanımlanır. Build seçimi:

```powershell
.\gradlew.bat :app:assembleStandaloneDatecsRelease -PCUSTOMER=phosfe
```

Yeni müşteri eklerken mevcut profili kopyalamak yerine aşağıdaki alanlarla yeni bir profil oluşturun:

- `CUSTOMER_ID`: build profil anahtarı
- `APP_NAME`: launcher ve uygulama içi görünen ad
- `APPLICATION_ID`: Android paket/application kimliği
- `ARTIFACT_NAME`: APK dosya adı öneki
- `VERSION_CODE`, `VERSION_NAME`: müşteriye ait sürüm dizisi
- `PRODUCER_CODE`, `DEVICE_TYPE`, `SERIAL_HEADER`: müşteriye BKM tarafından atanan değerler

İsteğe bağlı ikon ve diğer Android kaynakları `customers/<id>/res/` altında tutulabilir. Kaynak adları
ana uygulamadaki adlarla çakışmamalı; profil/manifest üzerinden müşteriye ait kaynak adı seçilmelidir.

Gizli veya ortama bağlı bilgiler aynı dizindeki, Git tarafından yok sayılan dosyalara konur:

- `bkm-environment.properties`: test/canlı host, vendor ID, IMMK ve RSA değerleri
- `keystore.properties`: müşterinin release signing keystore bilgileri

Donanım seçimi müşteriden bağımsızdır. Aynı müşteri profili Datecs, Newland veya simulator ile;
standalone veya ECR modunda derlenebilir.
