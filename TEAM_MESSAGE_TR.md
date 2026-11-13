# Arkadaşa gönderilecek mesaj

StudyBuddy'nin güncel hâlini Hurulayn0/StudyBuddy reposuna hazırladık. Önce README.md, sonra HANDOFF_TR.md'yi oku.

Amacımız PUMA benzeri öğretmen–öğrenci sistemi; ana konu bildirimler. Yerel ders kaydı ve ders saatine göre başlangıç/devam/bitiş bildirimleri çalışıyor; emülatörde başlangıç ve bitişi doğruladık. Ödev hatırlatması, Tamamlandı/Ertele, gruplama, Daily Reminder ve ek kronometre var.

Firebase de bağlandı: Console'dan emülatöre gerçek FCM mesajı gönderdik; bildirim ve uygulama içi kayıt oluştu. Ancak öğretmen panelindeki duyuru formu henüz sunucuya göndermiyor. Sıradaki ana iş gerçek öğretmen/öğrenci girişi ve yetkileri, ortak ders/ödev/duyuru verisi ve panelden yetkili backend üzerinden öğrenci cihazlarına FCM gönderimi.

Firebase projesi studybuddy-78253, Android paketi com.studybuddy.app. google-services.json Git'te yok; app/ içine ayrıca koyman gerekiyor. Dosyayı benden alabilir veya Firebase erişimi verilince Console'dan indirebilirsin. Firebase erişimi için Google e-posta adresini ilet; GitHub hesabı tek başına Firebase erişimi sağlamıyor.

Daily Reminder'ı ve çalışan özellikleri koruyalım. HANDOFF_TR.md'de dosya haritası, kalan işler, kurulum ve doğrulanması gereken testler ayrıntılı yazıyor.
