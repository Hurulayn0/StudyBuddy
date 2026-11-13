# Firebase bağlantısı — mevcut durum ve kullanım

Firebase Console üzerinde StudyBuddy projesi oluşturuldu: studybuddy-78253. Android uygulaması com.studybuddy.app (StudyBuddy Android) olarak kaydedildi. Google Analytics ve Gemini eklenmedi. Ücretli plan veya faturalandırma açılmadı.

## Kodda hazırlananlar

- Firebase BoM 35.2.0, messaging SDK ve Google Services Gradle eklentisi 4.6.0.
- app/google-services.json varsa eklenti uygulanır; dosya yokken yerel özellikler derlenmeye devam eder ve bağlantı ekranı yapılandırmanın eksik olduğunu belirtir.
- StudyBuddyMessagingService: ön plan notification mesajları ve data mesajlarını alır; eventId/messageId ile tekrarları önler.
- Öğretmen duyuruları kanalı; BigTextStyle duyuru bildirimi; dokununca gelen duyurular ekranı.
- Gelen mesajların cihazda saklanması ve okundu durumu.
- Admin → Firebase bağlantısı: FCM cihaz tokenını alma/kopyalama, genel school_announcements topic aboneliği.
- Mevcut ödev, ders, Daily Reminder ve kronometre kodları korunur.

## Yapılandırma

Firebase Console → StudyBuddy → Android uygulaması com.studybuddy.app → google-services.json indir → app/google-services.json konumuna koy → uygulamayı yeniden derle. Dosya .gitignore ile dışarıda kalır; aynı projeye bağlanacak ekip üyesinin bu dosyayı ayrıca edinmesi gerekir. Admin SDK özel anahtarı bu dosya değildir; servis hesabı anahtarını Android uygulamasına veya Git'e koyma.

## İlk canlı test

1. Uygulamayı Play Services içeren emülatör veya Android telefona yükle.
2. Admin → Firebase bağlantısı → Cihaz bağlantısını kontrol et.
3. Başarı mesajı gelirse cihaz tokenını kopyala; duyuru bildirim iznini aç.
4. Firebase Console Messaging → notification kampanyası → yalnız bu tokena test mesajı gönder.
5. Uygulama açıkken onMessageReceived mesajı kaydetmeli ve bildirim oluşturmalı. Notifications → Öğretmen duyuruları bölümünde görünmeli.
6. Uygulama arka plandayken Firebase Console notification mesajını Android sistemi gösterir. Böyle bir mesaj onMessageReceived üzerinden geçmez. Özel title/body veya announcement_title/announcement_body data alanları eklendiyse tıklamada yerel kayıt oluşturulur; alanlar yoksa mesajın uygulama içi kopyasının kaydedildiği varsayılmamalı.

Sunucudan data-only mesaj gönderildiğinde örnek data alanları:

```json
{
  "eventId": "announcement-001",
  "title": "Ödev teslim tarihi",
  "body": "Mobil Programlama ödevi pazartesiye kadar teslim edilebilir.",
  "courseName": "Mobil Programlama"
}
```

Data-only mesajları foreground/background alıcıda işlenir; Doze, öncelik, uygulamanın zorla durdurulması ve platform teslim koşulları hâlâ geçerlidir. FCM kesin dakika zamanlayıcısı değildir.

## Henüz tamamlanmayanlar

Bu istemci kurulumu, öğretmen panelinin başka telefona doğrudan gönderdiği anlamına gelmez. Paneldeki eski duyuru formu hâlâ yereldir. Authentication/öğretmen rolü, Firestore ortak veri, yetkili backend gönderimi ve ders üyeliği henüz uygulanmadı. Genel topic yalnız herkese açık okul duyurusu içindir; özel ders içeriğinde erişim kontrolü olarak kullanılamaz. Canlı gönderim doğrulanmadan “uçtan uca tamamlandı” denmemeli.

Kaynaklar: https://firebase.google.com/docs/android/setup, https://firebase.google.com/docs/cloud-messaging/android/get-started, https://firebase.google.com/docs/cloud-messaging/android/receive-messages, https://firebase.google.com/docs/cloud-messaging/server-environment.

## Canlı bağlantı doğrulaması — 13 Kasım 2026

app/google-services.json eklendi ve Google Services görevi gerçek StudyBuddy yapılandırmasını işledi. Derleme, mevcut birim testleri ve Android lint geçti. Yeni APK kayıtlar korunarak emülatöre yüklendi. Firebase'den gerçek FCM tokenı alındı. Console üzerinden yalnız bu tokena gönderilen “StudyBuddy bağlantı kontrolü” mesajı push_inbox içinde saklandı ve teacher_announcements kanalında bildirim olarak oluştu.

Sonuç: Firebase → Android mesaj alımı doğrulandı. Öğretmen paneli → yetkili sunucu → öğrenci gönderimi henüz bağlı değildir. İkinci telefon ve arka plan akışları ayrıca doğrulanmalıdır. Service account anahtarı üretilmedi, faturalandırma/ücretli plan açılmadı.
