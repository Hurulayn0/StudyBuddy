# Çalışma kronometresi ve bildirim testi

Notifications → Çalışma kronometresi.

1. Başlat: süre ilerler, bildirim panelinde çalışma süresi görünür.
2. Duraklat: süre donar; bekleme süresi toplam süreye eklenmez.
3. Devam et: kaldığı süreden ilerler.
4. Bitir: toplam çalışma süresi saat:dakika:saniye olarak ekranda kalır; devam bildirimi kaldırılır.
5. Yeni oturum: sıfırdan başlar.

Bildirimde de Duraklat / Devam et / Bitir butonları vardır. Bildirime dokununca kronometre açılır. Uygulamayı arka plana almak ölçümü durdurmaz; süre elapsedRealtime üzerinden hesaplanır. Bildirim sayacını Android'in chronometer görünümü ilerletir, arka planda her saniye çalışan servis eklenmemiştir.

İzin reddedilse bile kronometre kullanılabilir; bildirim için izin gerekir. Kullanıcı Android ayarlarıyla bildirimi kaldırabilir. Durum ve son toplam cihazda saklanır. Cihaz yeniden başlatılırsa çalışan oturum, son kaydedilmiş süreyle duraklatılmış kabul edilir; yeniden başlatma öncesindeki kesintisiz ölçüm garanti edilmez.

Kontrol: 5 sn çalış → duraklat → 5 sn bekle → devam et → 5 sn çalış → bitir. Toplam yaklaşık 10 sn olmalı. Aynı işlemleri bildirimden de deneyin; uygulamayı kapatıp yeniden açınca toplamı kontrol edin. Cihaz testleri henüz tamamlanmadı.

## Çalışma hedefi

Kronometrede dersini seç, 1–240 dakika arasında hedef gir veya 15/25/45 dakika seçeneklerinden birini kullan. Başlat; hedefe ulaşınca ders adıyla başarı bildirimi oluşur. Kronometre kendiliğinden bitmez. Bitir toplam süreyi korur. Duraklatma süresi hedefe sayılmaz.

Hedef alarmı yalnız çalışan oturum için kurulur; duraklatmada ve bitişte iptal edilir. Devam et kalan süreye göre yeniden kurar. Ekran açıkken hedef ayrıca süre hesabından kontrol edilir; aynı hedef iki kez bildirilmez. Android 12+ kesin alarm erişimi yoksa arka plan bildirimi gecikebilir. Android 13+ bildirim izni ve Çalışma hedefleri kanalı açık olmalıdır.

Cihaz kontrolü: 1 dakika hedefle başlat, 20 sn sonra duraklat, 20 sn bekle, devam et. Yaklaşık 40 sn daha çalışınca hedef bildirimi gelmeli; bekleme hedefe sayılmamalı. Yeni oturumda bildirim bir kez daha oluşabilmeli. Bitir'e hedeften önce basılırsa sonradan hedef bildirimi gelmemeli.

Bildirim denemeleri ve sabit örnek ödev bildirimleri kullanıcı arayüzünden kaldırıldı. Teknik araştırma tablosundaki uzun metin/gruplama incelemeleri araştırma olarak saklıdır; güncel sürümde ayrı deneme butonları yoktur.
