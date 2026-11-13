Güncel kapsam: StudyBuddy kendi okul sistemimizdir. Ana ders akışı artık öğretmenin kaydettiği dersin başlangıç ve bitiş saatlerinden oluşur. Bu akış için SCHOOL_LESSON_FLOW_TR.md geçerlidir. Aşağıdaki kronometre bölümleri korunan ek çalışma özelliğini anlatır; okul dersiyle karıştırılmamalıdır. FCM ve cihazlar arası öğretmen–öğrenci paylaşımı henüz eklenmemiştir.

# StudyBuddy — Bildirim araştırması

13 Kasım 2026 · Hocanın ders olayı ve Android bildirimleri hakkındaki beklentisine göre hazırlanmıştır.

## Amaç ve kapsam

StudyBuddy bir ders çalışma ve takip uygulamasıdır. Öğrenci dersini seçer, çalışma oturumu başlatır, duraklatır ve bitirir. Bildirim sistemi bu olaylara tepki verir. Ödev ve günlük hatırlatmalar da öğrencinin planını destekler. Buradaki ders başlangıcı, öğrencinin başlattığı çalışma oturumudur; okul ders programından otomatik yoklama/başlangıç algılanmaz.

## Bildirim türleri ve özellikleri

Bu tablo tek bir sınıflandırma değildir: kullanım senaryosu, stil, etkileşim ve gönderim yöntemi ayrı kavramlardır. “Uygun değil” teknik olarak yasak demek değil; mevcut özelliklere kullanım gerekçesi yok demektir.

| Özellik | Ne işe yarar? | StudyBuddy'de kullanım / gerekçe | Güncel durum |
|---|---|---|---|
| Standart yerel bildirim | Cihazdaki bilgiyi duyurur. | Ödev ve Daily Reminder. | Var. |
| Event notification | Bir olayın bildirime dönüşmesi senaryosudur. | Çalışmayı başlat, duraklat, devam et, bitir; hedefe ulaş. | Var; ayrı bir Android bildirim sınıfı değildir. |
| Background notification | Uygulama görünmezken bildirim gösterme senaryosudur. | Ödev/günlük/hedef alarmı; mevcut çalışma bildiriminin panelde kalması. | Var; teslim davranışı cihazda test edilmeli. |
| Ongoing notification | Devam eden durumu panelde temsil eder. | Seçilen dersin çalışma oturumu ve sayacı. | Var; bitişte iptal edilir. |
| PendingIntent | Sistemin daha sonra belirli bir işlemi yürütmesini sağlar. | Dokununca kronometre; aksiyonlarda BroadcastReceiver. | Var; bildirim türü değildir. |
| Aksiyonlar | Uygulamayı açmadan işlem yaptırır. | Duraklat/Devam et/Bitir, Tamamlandı/Ertele. | Var. |
| RemoteInput | Bildirimden metin alır. | İleride ders sonu kısa tekrar cevabı. | Koşullu; uygulanmadı. |
| BigTextStyle | Uzun metni genişleterek gösterir. | Gerçek ödev hatırlatmasının ayrıntısı. | Var; bağımsız deneme düğmesi kaldırıldı. |
| InboxStyle | Birkaç kısa satırlık özet sunar. | Gerçek ödev bildirimlerinin özeti. | Eklendi. |
| Gruplama | İlişkili bildirimleri bir araya toplar. | İki veya daha fazla gerçek ödev bildirimi özel grup ve InboxStyle özet kullanır. | Eklendi; tamamlama ve kaldırmada özet güncellenir. |
| BigPictureStyle | Genişletilmiş görsel sunar. | Görselli öğretmen duyurusu eklenirse. | Koşullu; yok. |
| Channels | Kategorileri kullanıcıya ayrı yönetilebilir sunar. | Çalışma süresi, ödev, günlük hatırlatma, hedef. | Dört kanal var; HIGH önemle oluşturuluyor. |
| Heads-up | Uygun koşullarda kısa üst uyarı gösterimidir. | Önemli hatırlatmalar. | HIGH kanal var; görünmesi kullanıcı ayarlarına bağlı. |
| Zamanlanmış bildirim | Zamana bağlı olaydan bildirim üretir. | Ödev saati, günlük saat, hedef süresi. | AlarmManager var; alarm bir bildirim stili değildir. |
| Foreground Service bildirimi | Kullanıcıya görünür devam eden işi servisle yönetir. | StudySessionService çalışma oturumunu sahiplenir ve hedefi izler. | Eklendi; cihaz testi bekliyor. |
| FCM push | Uzak sunucudan cihazlara mesaj ulaştırır. | Öğretmenden öğrencinin cihazına duyuru için uygun. | Henüz yok; Admin yalnız aynı cihazda çalışıyor. |
| MessagingStyle | Gerçek konuşma mesajlarını sunar. | Öğretmen–öğrenci mesajlaşması olursa. | Koşullu; mesajlaşma yok. |
| MediaStyle | Medya oynatma kontrolleri sunar. | Ses/video oynatıcı olmadığı için mevcut kapsamda uygun değil. | Yok. |
| CallStyle | Gelen/devam eden aramayı sunar. | Görüşme özelliği olmadığı için uygun değil. | Yok. |
| Full-screen intent | Acil alarm/arama için tam ekran açma mekanizmasıdır. | Sıradan çalışma hatırlatması için müdahaleci ve gereksiz. | Yok; özel erişim koşulları da bulunur. |
| RemoteViews | Özel bildirim düzeni oluşturur. | İleride özel şablon gereksinimi olursa; şu anda standart görünüm yeterli. | Araştırma kapsamında; uygulanmadı. |

Temel bildirim, PendingIntent ve aksiyonlar: [Android — bildirim oluşturma](https://developer.android.com/develop/ui/compose/notifications/create-notification). Stiller: [genişletilmiş bildirimler](https://developer.android.com/develop/ui/compose/notifications/expanded). Kategori ve önem: [kanallar](https://developer.android.com/develop/ui/compose/notifications/channels). Uzak mesaj: [Firebase mesaj alma](https://firebase.google.com/docs/cloud-messaging/android/receive-messages).

## Foreground Service ve ongoing ayrımı — güncel uygulama

Ongoing bildirim durum göstergesidir. Foreground Service kullanıcıya görünür devam eden oturumun servis yaşam döngüsüdür. StudySessionService çalışan oturumu sahiplenir, hedefi izler ve servis bildirimini sunar. Başlat ve Devam et servis başlatır; Duraklat servisi durdurur fakat devam etme bildirimini korur; Bitir servisle birlikte bildirimi kaldırır. Süre monotonic başlangıç anından hesaplanır.

Android 14+ için specialUse tipi, FOREGROUND_SERVICE ve FOREGROUND_SERVICE_SPECIAL_USE izinleri, çalışma oturumu/hedef izleme gerekçesini açıklayan subtype tanımı eklendi. MediaPlayback veya dataSync gibi yapılmayan bir işin türü seçilmedi. SpecialUse otomatik mağaza onayı değildir; Play dağıtımında gerekçe ayrıca değerlendirilir. Üretimde FGS gerektirmeyen alternatifler de karşılaştırılmalıdır. [Servis türleri](https://developer.android.com/develop/background-work/services/fgs/service-types#special-use)

Servis kullanıcı işlemiyle başlatılır ve hemen startForeground çağırır. Çalışırken hedef yaklaşık saniyelik kontrolle izlenir. FGS tek başına ekran kapalıyken CPU uyanıklığını garanti etmez; hedef alarmı da korunmuştur. [FGS başlatma](https://developer.android.com/develop/background-work/services/fgs/launch)

Kalıcı ifadesi panelde devam eden durumu anlatır; silinemez veya zorla durdurulamaz anlamında değildir. Android 14 birçok ongoing bildirimin kullanıcı tarafından kaldırılmasına izin verir. [Android 14 değişikliği](https://developer.android.com/about/versions/14/behavior-changes-all#non-dismissable-notifications)

## Ders olayının çalışan karşılığı

Başlat → durum kaydedilir, seçilen ders adıyla ongoing bildirim oluşturulur.

Duraklat → birikmiş süre saklanır, bildirim güncellenir, servis ve hedef alarmı durdurulur.

Devam et → servis yeniden başlar; kalan hedef alarmı kurulur ve bildirim güncellenir.

Bitir → toplam süre korunur, servis durur, devam bildirimi ve hedef alarmı kaldırılır.

Dokunma → PendingIntent kronometre ekranını açar. Teknik olay geçmişi öğrenci arayüzünde bulunmaz.

## İzinler ve gerçek sınırlar

- Android 13+ bildirim izni ve ilgili kanal açık olmalı. İzin reddedilirse kronometre kullanılabilir, bildirim gösterilemez. [İzin rehberi](https://developer.android.com/develop/ui/compose/notifications/notification-permission)
- Ödev zamanlaması kesin alarm erişimi ister. Hedef ve günlük hatırlatma erişim varsa kesin, yoksa gecikebilen alarm kullanır. Erteleme kesin değildir. WorkManager kesin dakika garantisi vermez. [Alarm rehberi](https://developer.android.com/develop/background-work/services/alarms)
- Daily Reminder her tetiklemede ertesi günü kurar; yeniden başlatma/saat dilimi değişikliği için alıcısı vardır. Ödev ve hedef alarmları yeniden başlatma sonrası geri kurulmaz.
- Kronometre elapsedRealtime kullanır; telefonun saatini değiştirmek ölçümü bozmaz. Yeniden başlatmada çalışan oturum son kaydedilmiş birikimle duraklatılmış kabul edilir; kesintisiz süre garantisi yoktur. [SystemClock](https://developer.android.com/reference/android/os/SystemClock#elapsedRealtime())
- Tüm kanallar şu anda HIGH oluşturuluyor. Farklı önem seviyeleriyle kapsamlı öncelik yönetimi henüz uygulanmadı.
- Firebase, doğrudan cevap, özel RemoteViews ve etkileşim analiz paneli tamamlanmış değildir. Bunlar genel Project 14 listesinin sonraki aşamasıdır.

## Kodla eşleştirme

- LessonEvents.kt: durum geçişleri, ongoing bildirim, hedef alarmı ve başarı bildirimi.
- StopwatchState.kt: süre ve duraklama hesabı.
- LessonActivity.kt: ders seçimi, kronometre, hedef ve toplam süre.
- NotificationActionReceiver.kt / TaskAlarmReceiver.kt: ödev aksiyonları ve zamanlanmış ödev.
- DailyReminder.kt: günlük hatırlatma, saat değişimi ve yeniden kurma.
- NotificationDemo.kt: güncel bildirim kartları ve kanal tanımları; dosya adındaki Demo eski iç isimdir, kullanıcıya deneme ekranı sunulmaz.

Sunum akışı ve test tablosu: NOTIFICATION_PRESENTATION_TR.md. Kodda var olması, cihazda teslim testinin geçtiği anlamına gelmez.

## Gerçek ödev gruplaması

En az iki bekleyen ödev varsa Tüm ödevleri hatırlat seçeneği görünür. Bildirimlerin kendi Tamamlandı/Ertele aksiyonları korunur. Aynı group anahtarı altında InboxStyle özet oluşturulur. Tek bildirim kalınca özet kaldırılır. Tamamlama, silme ve bildirimi kaydırarak kaldırmada özet güncellenir. Sabit örnek kayıt kullanılmaz. [Android gruplama](https://developer.android.com/develop/ui/views/notifications/group)

Yeni kod: StudySessionService.kt. Cihazda foreground servis başlatma/duraklatma/devam/bitirme ve özet yenileme ayrıca test edilmeli. Kod derlemesi cihaz yaşam döngüsü testinin yerine geçmez.
