# Kalıcı veri sözleşmesi

Uygulama veritabanı `phosfe-techpos.db` adıyla uygulamanın özel veritabanı dizininde tutulur.
Room şeması uygulama açılışında `PhosfeTechposApplication` tarafından açılır ve WAL günlükleme kullanır.
Üretimde şema yükseltmeleri açık migration ile yapılmalıdır; sessiz veri silen destructive migration kullanılmaz.

## Tablolar

- `payment_record`: Açık veya kapanmış batch içindeki finansal işlemler ve host korelasyon alanları.
- `delivery_debt`: Reversal, offline advice, TC advice ve batch upload gönderim borçları.
- `batch_cycle`: Açık/kapanmış batch numarası, işlem sıra sayacı ve settlement referansı.
- `parameter_table`: CRC doğrulaması tamamlanmış aktif BKM parametre tabloları.
- `parameter_projection`: VTERM/VBIN/VEOD/VCOMM2/EMV tablolarının doğrulanmış kernel görünümü; ham tablo ile aynı Room transaction'ında güncellenir.
- `settlement_receipt`: Kalıcı gün sonu sonucu ve yeniden yazdırma kaynağı.

## Güvenlik

Tam PAN ve ISO8583 istek/cevapları düz kolonlarda tutulmaz. Bu değerler Android Keystore'da seçilen
uygulamanın applicationId değerinden türetilen `<applicationId>.persistence.payloads.v1` anahtarıyla
AES-256-GCM zarfı olarak BLOB kolonlara yazılır.
Arama ve raporlama için yalnız maskeli PAN, STAN, RRN, batch, tutar ve cevap kodu açık kolonlardadır.

## Atomiklik ve gönderim sırası

Bir işlem ile o işlemden doğan offline advice ve TC advice kayıtları tek SQLite transaction'ında
yazılır. Reversal en yüksek outbox önceliğine sahiptir; daha sonra offline advice, TC advice ve batch
upload gelir. Bir borç yalnız korelasyonlu host cevabı alındığında `DELIVERED` yapılır. Timeout veya
uygulama kapanması halinde kayıt veritabanında kalır ve tekrar gönderilebilir.
`IN_FLIGHT` durumundaki bir kayıt da yeniden başlatmada tekrar seçilir; yalnız `DELIVERED` kalıcı
olarak tamamlanmış sayılır. Aynı batch içinde işlem sıra numarası benzersiz indeksle korunur.

Parametre aktivasyonu da tek Room transaction'ıdır: sıfır uzunluklu tablolar silinir, pakette olmayan
mevcut tablolar korunur ve gelen tablolar birlikte devreye alınır.
