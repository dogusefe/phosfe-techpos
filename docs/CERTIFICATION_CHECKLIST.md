# BKM sertifikasyon hazırlık matrisi

Kaynak: BKM TechPOS Sistemi Vendor Mesaj Spesifikasyonu V3.2.1. Bu liste sertifikasyon garantisi
değildir; geliştirme ve kanıt toplama kontrolüdür.

| Alan | Hedef | Mevcut durum |
|---|---|---|
| ISO 8583 temel codec | BCD MTI, bitmap, sabit/değişken alanlar | Temel codec ve birim testleri hazır |
| BER-TLV | F48/F55/F62/F63 verileri | Codec ve birim testleri hazır |
| Mesaj zarfı | Host header, uzunluk, encryption indicator, CRC | Codec ve birim testleri hazır |
| Mesaj şifreleme | 3DES-CBC, sıfır IV, PKCS#5, 16/24 byte MSK | Codec ve birim testleri hazır |
| Anahtar değişimi | RSA ile legacy akış, RKL, TR-31 ve TR-34 | Bekliyor; HSM/SDK gerektirir |
| Parametre yükleme | Tüm parametre tabloları, paket birleştirme, atomik kayıt | Bekliyor |
| Handshake | 0800/0810 akışı ve host sinyalleri | Bekliyor |
| Otorizasyon | Başarılı/başarısız işlem, EMV, online PIN | Bekliyor |
| Advice / TC upload | Offline advice ve sertifika yükleme | Bekliyor |
| Teknik iptal | Reversal saklama, tekrar deneme, idempotency | Bekliyor |
| Gün sonu | EOD, batch upload, belge ve sayaç yönetimi | Bekliyor |
| Ek işlemler | DCC, Çiftçi Kart, puan, QR, taksit, iade, iptal | Bekliyor |
| Fiş | Mali alanlar, maskeleme, dijital slip | Bekliyor |
| Datecs adaptörü | EMV L2, PIN, güvenli anahtar, yazıcı | SDK entegrasyonu bekliyor |
| Newland adaptörü | EMV L2, PIN, güvenli anahtar, yazıcı | SDK entegrasyonu bekliyor |
| ECR modu | Güvenli dış uygulama sözleşmesi ve durum ilerlemesi | Bekliyor |
| Dayanıklılık | Ağ kesintisi, güç kaybı, tekrar başlatma, batch sınırı | Bekliyor |
| Kanıt paketi | Test logları, mesaj örnekleri, APK imzası, sürüm matrisi | Bekliyor |

## Sertifikasyon kapısı

Gerçek cihazda üretici L2 kernel onayı, test host terminal kaydı, vendor/üretici/cihaz kodları,
test anahtarları ve BKM test senaryoları sağlanmadan hiçbir sürüm sertifikasyona hazır kabul edilmez.
