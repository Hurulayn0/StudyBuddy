# StudyBuddy — Sunum akışı ve test planı

## 60–90 saniyelik anlatım

“StudyBuddy'de öğrencinin ders çalışma oturumunu bir olay akışı olarak ele aldık. Başlatınca devam bildirimi oluşuyor. Uygulama arka plana geçse de çalışma bilgisi panelde bulunuyor. PendingIntent ile bildirime dokununca kronometre açılıyor; Duraklat, Devam et ve Bitir aksiyonları uygulama açılmadan da çalışıyor. Bitirince toplam süre saklanıyor ve devam bildirimi kaldırılıyor.

Ödev için Tamamlandı ve Ertele, günlük çalışma için Daily Reminder kullandık. Çalışma hedefine ulaşmayı da bir olay bildirimiyle gösteriyoruz. Çalışma oturumunu foreground servis ile yönetiyoruz; Duraklat ve Bitir servis yaşam döngüsünü de değiştiriyor. Bildirimleri kanallara ayırdık. Android 13+ izni ve kullanıcı tercihlerini dikkate aldık.

Araştırma tablomuzda kullanılabilecek ve mevcut kapsamda uygun olmayan özellikleri gerekçeleriyle ayırdık. Firebase gibi henüz tamamlamadığımız işleri tamamlanmış olarak sunmuyoruz.”

## Canlı gösterim — ana akış

1. Notifications → Kronometreyi aç. Bir ders seç; istersen Ders ekle ile yeni ad gir. 1 dakika hedef belirle.
2. Başlat. Paneli aç; ders adı ve ilerleyen sayacı göster. Bu, başlangıç olayının bildirim karşılığıdır.
3. Ana ekrana dön; panelde bildirim kaldığını göster. Üst balonun kaybolması, panel bildiriminin kaldırıldığı anlamına gelmez.
4. Bildirimden Duraklat. 5 saniye bekle; sayaç ilerlememeli.
5. Bildirimden Devam et. Ardından bildirimin gövdesine dokun; kronometreye dönmeli.
6. Bitir. Panelde devam bildirimi kalkmalı; ekranda toplam süre kalmalı. Bekleme süresi çalışma süresine sayılmaz.

## İsteğe bağlı kısa ek gösterimler

- Hedef: ayrı oturumda 1 dakika çalış; hedef bildirimi bir kez gelsin. Başarı bildirimi sonrası sayaç çalışmayı sürdürür.
- Ödev: tamamlanmamış ödev seç → Bildirim gönder → Tamamlandı. Ödevler listesinde yalnız o ödev işaretlenmeli.
- Ertele: farklı bekleyen ödev bildirimi → 10 dk ertele. En erken 10 dk sonra; güç yönetimi geciktirebilir.
- Daily Reminder: açık olduğunu ve seçilen günlük saati göster. Test için cihaz saatinden birkaç dakika sonrası seçilebilir; sunumdan sonra tercih edilen saate döndür.

## Hocanın sorabileceği üç soru

**PendingIntent neden bildirim türü değil?** Tıklama/aksiyon sonrasında çalıştırılacak işlemi sisteme veren mekanizma; stil veya teslim yöntemi değil.

**Foreground Service ne yapıyor?** Kullanıcının başlattığı çalışma oturumunu sahipleniyor ve hedefi izliyor. Android 14+ specialUse türüyle tanımlı. Duraklat ve Bitir servis işini durduruyor.

**Bu bildirim silinemez mi?** Hayır. Ongoing devam eden durumu belirtir; Android 14'te kullanıcı birçok ongoing bildirimi kaldırabilir. “Kesinlikle silinemez” demiyoruz.

## Cihaz test tablosu

Yeni sürümün cihaz testleri henüz yapılmadı. Aşağıdaki sonuç alanlarını ancak gözlemledikten sonra doldurun.

| Test | Beklenen | Sonuç |
|---|---|---|
| Başlat | Seçilen dersle devam bildirimi | Bekliyor |
| Arka plana geç | Sayaç/süre doğru kalır | Bekliyor |
| Bildirimden Duraklat/Devam et | Bekleme süresi toplama eklenmez | Bekliyor |
| Bildirime dokun | Kronometre açılır | Bekliyor |
| Bildirimden Bitir | Bildirim kalkar; toplam korunur | Bekliyor |
| Uygulamayı yeniden aç | Son toplam veya devam durumu korunur | Bekliyor |
| İzin reddi | Kronometre çalışır; bildirim gösterilmez, açıklama var | Bekliyor |
| Kanal kapalı | O kanalda bildirim yok | Bekliyor |
| Bir dakika hedef / araya mola | Yalnız çalışma süresi hedefe sayılır; tek başarı bildirimi | Bekliyor |
| Hedeften önce Bitir | Sonradan başarı bildirimi gelmez | Bekliyor |
| Ödev Tamamlandı | Doğru kayıt işaretlenir | Önceki sürümde gözlemlendi; yeni sürümde tekrarla |
| Ödev erteleme / zamanlama | Cihaz saatine göre teslim | Bekliyor |
| Daily Reminder aç/kapat | Açıkken günlük tekrar; kapalıyken alarm yok | Bekliyor |

## Kod kontrollerinin kapsamı

Derleme, birim testleri ve lint cihazdaki bildirim teslimini doğrulamaz. Süre testleri mola süresini dışlama, tekrar eden aksiyonlar ve bitmiş oturum davranışını denetler. Emülatör Android sürümü, saat dilimi, bildirim izni ve alarm erişimi sunumdan önce not edilmelidir.

## 13 Kasım güncellemesi

Önceki anlatımdaki “servis eklemedik” cümlesi geçersizdir. Yeni ifade: “Çalışma oturumunu StudySessionService ile foreground olarak yönetiyoruz. Duraklat servis işini durdurur, Devam et yeniden başlatır, Bitir hem servisi hem devam bildirimini kaldırır. Android 14+ specialUse türünü gerekçesiyle tanımladık; mağaza onayı ayrıca değerlendirilir.”

Yeni cihaz kontrolleri (henüz yapılmadı):

1. Başlat → uygulamayı arka plana al → sayaç ve servis bildirimi devam etsin.
2. Bildirimden Duraklat → servis dursun; duraklama bildirimi kalsın.
3. Bildirimden Devam et → servis yeniden başlasın.
4. Bildirimden Bitir → servis ve çalışma bildirimi kalksın.
5. Bir dakika hedefle arka planda bekle → tek başarı bildirimi gelsin.
6. İki gerçek bekleyen ödev ekle → Tüm ödevleri hatırlat → grup ve özet görünsün.
7. Birini tamamla → doğru kayıt değişsin; tek bildirim kalınca özet kalksın. Kaydırarak kaldırmayı da kontrol et.

Android Studio emülatörü bağlı olmadığından bu yeni servis akışının cihaz testi yapılmadı. Daily Reminder ve ödev zamanlaması korunmuştur.
