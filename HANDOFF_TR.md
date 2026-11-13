# StudyBuddy — ekip devir notu

Güncelleme: 13 Kasım 2026. Repo: https://github.com/Hurulayn0/StudyBuddy

## Önce bunu bil

StudyBuddy, PUMA benzeri kendi okul sistemimizdir. Gerçek PUMA veya okul e-posta hesabına bağlı değildir. Ana proje konusu Android bildirimleridir. Hedef, öğretmenin yayınladığı ders/ödev/duyuru bilgisinin başka cihazdaki öğrenciye ulaşmasıdır.

**Firebase Android bağlantısı ve Firebase Console → emülatör push alımı çalışıyor. Öğretmen paneli → yetkili sunucu → öğrenci gönderimi henüz bağlı değil.**

## Yapılanlar

| Bölüm | Şu anki durum |
|---|---|
| Ders yönetimi | Öğretmen panelinde ders adı, öğretmen, derslik, başlangıç/bitiş tarihi kaydedilir; çoklu ders listesi vardır. Veriler yereldir. |
| Ders başlangıç/devam/bitiş | AlarmManager başlangıçta ongoing bildirim oluşturur, bitişte kaldırıp ders sonu bildirimi gönderir. Kullanıcı emülatöründe Mobil Programlama 13.08 başlangıç ve 13.10 bitişi görüntülerle doğrulandı. |
| Ders yönlendirmesi | Bildirim ilgili ders kaydını açar. Ders silinince alarmları ve bildirimleri iptal edilir. |
| Ödevler | Yerel kayıt, tamamlanma, seçilen ödeve anlık/zamanlanmış bildirim, Tamamlandı ve 10 dk ertele aksiyonları. |
| Zengin metin ve gruplama | BigTextStyle; gerçek ödevlere grup anahtarı ve InboxStyle özet. İki veya daha çok bekleyen ödevde Tüm ödevleri hatırlat seçeneği vardır. |
| Daily Reminder | Günlük saat, aç/kapat ve yeniden kurma alıcısı. Kullanıcı tarafından bildirim alındığı görüldü. Bu özellik korunmalı. |
| Çalışma kronometresi | Ek özellik: ders seçimi/ekleme, süre hedefi, mola hariç toplam süre, başarı bildirimi. Okul dersinin otomatik başlangıcından ayrıdır. |
| Foreground Service | StudySessionService çalışma oturumunu/hedefi izler. Android 14+ specialUse. Son servis yaşam döngüsü ayrıca cihazda test edilmeli; mağaza onayı varsayılmamalı. |
| Kanallar ve izinler | Ders, ödev, günlük hatırlatma, hedef ve öğretmen duyuruları; Android 13+ izni ve alarm erişimi kontrolleri. |
| Firebase | studybuddy-78253, Android com.studybuddy.app. SDK, Google Services yapılandırması, FirebaseMessagingService, token kaydı ve genel topic aboneliği kodu hazır. |
| Push gelen kutusu | Gelen mesajı cihazda saklar, yinelenen kimliği süzer, bildirimden duyuruyu açar, okundu durumu tutar. |
| Canlı FCM testi | Firebase Console'dan yalnız emülatör tokenına gönderilen mesaj hem push_inbox kaydına hem Android öğretmen duyuruları bildirimine dönüştü. Ön plan alımı doğrulandı. |
| Araştırma/sunum | Uygunluk tablosu, kaynaklar, kullanım gerekçeleri ve canlı gösterim notları repoda. |

## Henüz yapılmayanlar — öncelik sırasıyla

1. **Öğretmen/öğrenci kimliği ve roller:** gerçek giriş yok. Mevcut Admin ekranına ulaşmak öğretmen yetkisi değildir. Firebase Authentication ve sunucu tarafında rol/üyelik kontrolü kurulmalı.
2. **Ortak veri:** dersler, öğrencinin ders kayıtları, ödev ve duyurular Firestore gibi ortak veriye taşınmalı. Yerel SharedPreferences kayıtları başka telefona gitmez.
3. **Güvenilir gönderim:** paneldeki yayınlama isteğini doğrulayan backend/Cloud Functions, Admin SDK veya HTTP v1 ile FCM göndermeli. Mevcut duyuru formu sadece aynı uygulamanın geçici listesine ekler; push göndermez.
4. **Cihaz/abonelik yönetimi:** onNewToken kullanıcı/cihaz kaydına bağlanmalı. school_announcements genel topic kodu var; ders topic'leri, ders üyeliği ve cihaz grupları tamamlanmadı. Topic tek başına yetkilendirme değildir.
5. **İki cihaz testi:** öğretmen A cihazından yayınlasın; öğrenci B cihazında uygulama arka plandayken alsın; dokununca doğru kayıt açılsın. Yetkisiz öğrenci özel içeriğe erişememeli.
6. **Project 14 ekleri:** RemoteInput/direct reply, görsel/medya stilleri, RemoteViews şablonları, kapsamlı öncelik yönetimi ve etkileşim analizi paneli yok. Gönderildi, teslim edildi, açıldı ve okundu ölçümleri ayrı tasarlanmalı.
7. **Duyuru vs mail:** uygulama içi öğretmen duyurusu planlanıyor. Gerçek okul mail kutusunu okuma/gönderme entegrasyonu yapılmadı.

FCM bir veri deposu veya kesin saat zamanlayıcısı değildir. Ders bilgisi öğrenciye senkronize edildikten sonra mevcut yerel başlangıç/bitiş alarmları kurulabilir; yeni ödev ve duyuru için push kullanılabilir.

## Kendi bilgisayarında çalıştır

1. Repoyu klonla veya mevcut kopyanda main dalını güncelle:

```powershell
git clone https://github.com/Hurulayn0/StudyBuddy.git
```

2. Android Studio'da proje klasörünü aç. SDK 36'yı ve Gradle 9.1.0/AGP 9.0.1 ile uyumlu gömülü JDK'yı kullan. minSdk 26.
3. SDK yolunu kendi bilgisayarında local.properties ile ayarla. Başkasının SDK yolunu kopyalama.
4. FCM için Firebase Console → studybuddy-78253 → Project settings → Android com.studybuddy.app → google-services.json indir; **app/google-services.json** konumuna koy.
5. Bu dosya Git'e dahil değil. Proje sahibinden ayrıca alınabilir veya Firebase erişimi olan Google hesabınla indirilebilir. GitHub erişimi Firebase erişimi vermez; Firebase daveti için Google e-posta adresi gerekir.
6. Dosya olmadan yerel uygulama derlenir; FCM ekranı eksik yapılandırma gösterir. Firebase canlıymış gibi değerlendirme.
7. Play Services içeren emülatör/telefonda Run yap. Bildirim iznini ver; ders/ödev zamanlaması için alarm erişimini kontrol et.

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --no-daemon
```

Son kontroller gerçek Firebase yapılandırmasıyla başarılıydı. Birim testleri mola süresi ve ders zaman sınırlarını kontrol eder; alarm/FCM tesliminin yerine geçmez.

## İlk inceleme ve test

- Admin → Ders ekle ve yönet: cihaz saatinden 2 dk sonra başlangıç, 4 dk sonra bitiş kaydet. Notifications → Derslerim'den izle.
- Admin → Firebase bağlantısı → Cihaz bağlantısını kontrol et → tokenı kopyala. Console Messaging'den yalnız o tokena test gönder.
- Notifications → Öğretmen duyuruları: ön planda gelen mesajı gör.
- Console notification mesajı arka planda OS tarafından gösterilebilir ve onMessageReceived çalışmayabilir. Uygulama içi kopya için özel data alanları veya sunucudan senkronizasyon gerekir. FIREBASE_SETUP_TR.md'yi oku.
- Gerçek grup/özetin tamamlama, silme ve kaydırarak kaldırma davranışını test et.
- FGS: Başlat → arka plan → Duraklat → Devam et → Bitir; servis ve bildirim yaşam döngüsünü doğrula.

## Kod haritası

- MainActivity.kt: navigasyon, yerel ödevler, öğretmen formu, bildirim kartları.
- school/SchoolLesson.kt, SchoolSchedule.kt, SchoolLessonsActivity.kt: okul ders akışı.
- notifications/FirebaseConnection.kt: Firebase init/token/topic, PushInbox yerel deposu.
- notifications/StudyBuddyMessagingService.kt: gerçek FCM alıcısı ve token yenileme callback'i.
- notifications/PushInboxActivity.kt: cihaz bağlantısı ve gelen öğretmen duyuruları.
- StudyBuddyApplication.kt: varsayılan FCM kanalı ve Firebase başlangıcı.
- notifications/NotificationActionReceiver.kt: ödev bildirimi, aksiyonlar, grup/özet.
- notifications/DailyReminder.kt: günlük hatırlatma.
- notifications/LessonEvents.kt, StopwatchState.kt, StudySessionService.kt: ek çalışma oturumu.
- notifications/NotificationDemo.kt: adı eski; güncel kartlar/kanallar. Kullanıcıya deneme paneli sunmaz.
- TaskStore.kt / CourseStore.kt: yerel kayıtlar.

## Koruma ve gerçek sınırlar

- Daily Reminder ve mevcut ödev/kronometre özelliklerini kullanıcı onayı olmadan kaldırma.
- Okul ders başlangıcı alarm + ongoing bildirimi kullanır. FGS çalışma kronometresine aittir; ikisini karıştırma.
- FGS specialUse gerekçesi mağaza yayını için ayrıca değerlendirilmeli.
- Android 14 ongoing bildirimi kullanıcı tarafından kaldırılabilir; silinemez diye sunma.
- Ders bitiş alıcısı Doze altında gecikebilir; devam bildiriminin timeout'u ayrıca vardır.
- Daily Reminder yeniden başlatmada kurulur; tüm diğer alarmlar için aynı garanti yoktur. Ayrıntı SCHOOL_LESSON_FLOW_TR.md ve NOTIFICATION_DEMO.md'dedir.
- Emülatör LOW_MEMORY yüzünden kapanmıştı; gereksiz Gradle daemon durdurulunca gözlem sırasında açık kaldı. Wipe Data kullanıcının yerel kayıtlarını siler.
- google-services.json, local.properties, build çıktıları ve yerel doğrulama ekran görüntüsü Git dışında. Admin SDK özel anahtarı Android'e veya repoya konmamalı.
- Spark planı kullanıldı; ücretli plan/faturalandırma açılmadı. Backend seçimi ek plan/ücret gerektirebilir; ekip sahibiyle kararlaştırılmalı.

## Belgeler

README.md, FIREBASE_SETUP_TR.md, SCHOOL_LESSON_FLOW_TR.md, NOTIFICATION_RESEARCH_TR.md ve NOTIFICATION_PRESENTATION_TR.md. Eski notlarda çalışma kronometresi ana ders sistemi gibi anlatılmışsa bu devir notundaki güncel kapsam esas alınmalı.
