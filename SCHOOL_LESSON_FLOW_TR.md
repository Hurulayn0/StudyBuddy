# Okul ders akışı — 13 Kasım 2026

Amaç: PUMA benzeri kendi okul sistemimizde öğretmenin kaydettiği dersin saatinden bildirim üretmek. Gerçek PUMA bağlantısı yoktur.

## Kullanım

1. Öğretmen: Admin → Ders ekle ve yönet → Ders ekle.
2. Ders, öğretmen, derslik, başlangıç tarihi/saati ve bitiş tarihi/saati girilir.
3. Bildirim ve kesin alarm erişimi açıkken Dersi kaydet seçilir.
4. Öğrenci: Notifications → Derslerim → Dersleri görüntüle.
5. Başlangıçta “... dersi devam ediyor” bildirimi oluşur; alt bilgide dersin başladığı ve öğretmen görünür. Bildirim bitiş saatine kadar sürer.
6. Bitişte devam bildirimi kaldırılır ve ayrı “... dersi bitti” bildirimi gösterilir.
7. Bildirime dokunmak ilgili ders kaydını listede öne getirir. Ders kaldırılırsa bekleyen alarmlar ve bildirimleri iptal edilir.

Birden fazla bağımsız ders kaydı tutulur. Tarihler cihazın yerel saatine göre seçilir. Öğrenci ekranı ders ekleme/silme formunu göstermez.

## Sınırlar

Öğretmen modu şu aşamada aynı uygulamadaki giriş noktasıdır; kimlik doğrulama/yetkilendirme sistemi değildir. Veriler aynı cihazda saklanır. Gerçek öğretmen cihazından farklı öğrenci cihazına paylaşım, ders abonelikleri ve FCM henüz yoktur. Bu ilk ders akışı uzaktan gönderim yapılmış gibi sunulmamalıdır.

Başlangıç setExactAndAllowWhileIdle, bitiş setExact kullanır. Ekran kapalı Doze durumunda bitiş alıcısı ve bitiş bildirimi gecikebilir; ongoing bildirim için ayrıca bitiş süresine timeout konmuştur. Exact erişimi sonradan geri alınırsa gelecekteki kurulumlar inexact çalışabilir. Kullanıcı izinleri, zorla durdurma ve üretici güç yönetimi teslimi etkiler.

Yeniden başlatma ve saat değişikliklerinde gelecek dersler yeniden zamanlanır; devam eden dersin bildirimi yeniden oluşturulur. Geçmiş derslerin bitiş bildirimleri topluca tekrar gönderilmez. Dersi uygulamada tamamlandı görmek alıcının zamanında çalıştığının tek başına kanıtı değildir.

Ders akışı kısa alarm alıcıları ve ongoing bildirim kullanır, kendi foreground servisi yoktur. Kronometreye ait StudySessionService ayrı bir ek özelliktir; okul dersinin otomatik devamını gösteren servis olarak sunulmamalıdır. Daily Reminder, kronometre ve ödev özellikleri korunmuştur.

## Emülatörde doğrulama

13 Kasım 2026 kullanıcı emülatöründe Mobil Programlama 13.08 başlangıç/devam ve 13.10 bitiş bildirimleri görüntülerle doğrulandı. Diğer durumlar aşağıdaki kontrol listesine göre ayrıca test edilmelidir.

- Cihaz saatine göre başlangıcı 2 dakika, bitişi 4 dakika sonrasına ayarla. Uygulamayı arka plana al; başlangıç bildirimi gelsin, ilgili ders açılsın, bitişte bildirim değişsin.
- İki ders ekle; birbirinin bildirimini/alarmlarını değiştirmemeli.
- Gelecek dersi sil; sonradan bildirim gelmemeli.
- Bildirim izni/kanalı kapalı durumunu kontrol et.
- Alarm erişimi kapalıyken kayıt açıklama vermeli; izin verip yeniden kaydet.
- Bitiş başlangıçtan önce veya başlangıç geçmişte ise kayıt reddedilmeli.

Birim testleri yalnız ders durumunun başlangıç/bitiş sınırlarını ve kayıt doğrulamasını test eder; alarm teslimini test etmez.
