# StudyBuddy — okul bildirim sistemi

StudyBuddy, PUMA benzeri kendi öğretmen–öğrenci sistemimizdir. Gerçek PUMA bağlantısı veya okul e-postası entegrasyonu yoktur. Projenin ana konusu, ders/ödev/duyuru olaylarının Android bildirimlerine dönüşmesidir.

**FCM Android bağlantısı kuruldu ve Firebase Console üzerinden emülatöre gerçek push mesajı alımı doğrulandı. Öğretmen panelinden cihazlar arası gönderim ve ortak veri senkronizasyonu henüz eklenmedi.** Öğretmen paneli gerçek bir yetkilendirme sistemi değildir; şu anda aynı uygulama içindeki arayüzdür.

## Çalışan özellikler

- Öğretmen panelinden ders, öğretmen, derslik, başlangıç ve bitiş zamanı kaydı.
- Başlangıçta ongoing ders bildirimi; bitişte kaldırma ve ayrı ders sonu bildirimi.
- Bildirimden ilgili dersin açılması; ders silinince alarm/bildirim iptali.
- Ödev seçimi, anlık/zamanlanmış hatırlatma, Tamamlandı ve 10 dk ertele.
- Birden fazla gerçek ödev için grup ve InboxStyle özet; BigTextStyle ayrıntı.
- Daily Reminder: günlük saat seçimi, aç/kapat, günlük yeniden kurma.
- Ek özellik olarak ders çalışma kronometresi, hedef bildirimi ve StudySessionService.
- Bildirim kanalları, PendingIntent ve Android 13+ bildirim izni.

Ana okul dersi akışı ile ek çalışma kronometresi birbirinden ayrıdır. SchoolSchedule ders saatlerini alarmla işler; StudySessionService yalnız kullanıcı tarafından başlatılan çalışma oturumunundur.

## Başlatma

1. Repoyu Android Studio ile aç; Gradle Sync tamamlanmalı.
2. Android SDK yolunu kendi bilgisayarında local.properties üzerinden ayarla. Bu dosya Git'e alınmaz.
3. JDK olarak Android Studio'nun uygun gömülü JDK'sını kullan; proje AGP 9.0.1, Gradle 9.1.0, compile/target SDK 36 ve minSdk 26 kullanır.
4. Emülatörü aç, uygulamayı Run ile yükle. Android 13+ bildirim izni ver.
5. Admin → Ders ekle ve yönet: cihaz saatinden 2 dakika sonrasına başlangıç, 4 dakika sonrasına bitiş gir. Alarm erişimini açıp kaydet.
6. Notifications → Derslerim: ders kaydını gör; panelde başlangıç ve bitiş bildirimlerini takip et.

Firebase projesi studybuddy-78253; Android paketi com.studybuddy.app. Firebase için app/google-services.json dosyasını ekipten edinip ekle. Dosya yokken yerel özellikler çalışır, FCM bağlantısı kurulmaz. .gitignore içindeki google-services.json ve hizmet hesabı anahtarı kuralları korunmalı; Admin SDK özel anahtarları Android uygulamasına konmamalı.

## Kontroller

Windows PowerShell, proje kökünde:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

Son çalıştırmada bu kontroller geçti. Cihazdaki tüm senaryoların geçtiği anlamına gelmez. 13 Kasım 2026 kullanıcı emülatör görüntülerinde Mobil Programlama 13.08 başlangıç/devam ve 13.10 bitiş bildirimi doğrulandı.

Bilgisayar belleği sınırlıysa derlemeden sonra ` .\gradlew.bat --stop ` ile Gradle daemon kapatılabilir. Emülatörde LOW_MEMORY kaynaklı kapanma görüldü; proje kod çökmesi olarak sınıflandırılmadı. Wipe Data kayıtları siler; sıradan yeniden başlatmada gerekli değildir.

## Devir ve araştırma

- [Ekibe devir notu ve Firebase yol haritası](HANDOFF_TR.md)
- [Okul ders akışı ve testleri](SCHOOL_LESSON_FLOW_TR.md)
- [Bildirim araştırması](NOTIFICATION_RESEARCH_TR.md)
- [Sunum ve kontrol listesi](NOTIFICATION_PRESENTATION_TR.md)
- [Kronometre/hatırlatma ayrıntıları](NOTIFICATION_DEMO.md)

Çelişen eski notlar varsa okul sisteminin kapsamı için bu README, HANDOFF_TR.md ve SCHOOL_LESSON_FLOW_TR.md esas alınmalıdır.

Firebase kurulum ve canlı test notları: [FIREBASE_SETUP_TR.md](FIREBASE_SETUP_TR.md). 13 Kasım 2026 testinde FCM tokenı alındı; Console test mesajı uygulamanın gelen duyuru kaydına yazıldı ve öğretmen duyuruları kanalında Android bildirimi oluştu.


Ekibe devam için ilk okunacak belge [HANDOFF_TR.md](HANDOFF_TR.md); gönderilecek kısa mesaj [TEAM_MESSAGE_TR.md](TEAM_MESSAGE_TR.md).
