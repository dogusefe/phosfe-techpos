# BKM sertifikasyon hazırlık matrisi

Kaynak: BKM TechPOS Sistemi Vendor Mesaj Spesifikasyonu V3.2.1. Bu liste sertifikasyon garantisi
değildir; geliştirme ve kanıt toplama kontrolüdür.

| Alan | Hedef | Mevcut durum |
|---|---|---|
| ISO 8583 temel codec | BCD MTI, bitmap, sabit/değişken alanlar | Temel codec ve birim testleri hazır |
| BER-TLV | F48/F55/F62/F63 verileri | Codec ve birim testleri hazır |
| Mesaj zarfı | Host header, uzunluk, encryption indicator, CRC | Codec ve birim testleri hazır |
| Mesaj şifreleme | 3DES-CBC, sıfır IV, PKCS#5, 16/24 byte MSK | Codec ve birim testleri hazır |
| Anahtar değişimi | RSA legacy taşıma + TR-31 key block | Bekliyor; HSM/SDK ve vendor key-injection sözleşmesi gerektirir; RKL/TR-34 kapsam dışı |
| Parametre yükleme | Tüm parametre tabloları, paket birleştirme, atomik kayıt | 900000/900001, chunk/CRC, tablo-özel binary/BER parser'ları, doğrulama ve atomik Room snapshot/projeksiyon aktivasyonu hazır |
| Kalıcı veri | İşlem, reversal, advice, batch ve gün sonu kayıtlarının yeniden başlatmada korunması | Room şema v2, WAL, migration, şifreli ISO/PAN zarfları, parametre projeksiyonları ve öncelikli durable outbox hazır |
| Handshake | 0800/0810 akışı, parametre versiyonları ve host sinyalleri | Temel mesaj/parser hazır |
| Otorizasyon | Başarılı/başarısız işlem, EMV, online PIN | Temel mesaj/parser ve güvenli gönderim kapısı hazır |
| Advice / TC upload | Offline advice ve sertifika yükleme | Bekliyor |
| Teknik iptal | Reversal saklama, tekrar deneme, idempotency | Şifreli journal, işlem engeli ve 0400/0410 dispatcher hazır |
| Gün sonu | EOD, batch upload, belge ve sayaç yönetimi | 0500/0510, 0320/0330 ve mutabakat durum makinesi hazır; kalıcı ledger ve belgeler bekliyor |
| Ek işlemler | DCC, Çiftçi Kart, puan, QR, taksit, iade, iptal | Bekliyor |
| Fiş | Mali alanlar, maskeleme, dijital slip | Bekliyor |
| Datecs adaptörü | EMV L2, PIN, güvenli anahtar, yazıcı | SDK entegrasyonu bekliyor |
| Newland adaptörü | EMV L2, PIN, güvenli anahtar, yazıcı | SDK entegrasyonu bekliyor |
| ECR modu | Güvenli dış uygulama sözleşmesi ve durum ilerlemesi | Bekliyor |
| Dayanıklılık | Ağ kesintisi, güç kaybı, tekrar başlatma, batch sınırı | Reversal güç-kesintisi sınırı hazır; diğerleri bekliyor |
| Kanıt paketi | Test logları, mesaj örnekleri, APK imzası, sürüm matrisi | Bekliyor |

## Sertifikasyon kapısı

Gerçek cihazda üretici L2 kernel onayı, test host terminal kaydı, vendor/üretici/cihaz kodları,
test anahtarları ve BKM test senaryoları sağlanmadan hiçbir sürüm sertifikasyona hazır kabul edilmez.
