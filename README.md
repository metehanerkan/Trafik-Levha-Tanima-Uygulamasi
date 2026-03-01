# 🚦 Trafik Levha Tanıma ve Sürüş Asistanı

Bu proje, bir uç cihazdan (Raspberry Pi/PC) gelen yapay zeka verilerini WebSocket üzerinden anlık olarak işleyen, sürücüye sesli uyarılar veren ve sürüş istatistiklerini harita üzerinde takip eden modern bir Android uygulamasıdır.

## 🚀 Öne Çıkan Özellikler

* **Gerçek Zamanlı Veri Akışı:** WebSocket protokolü ile düşük gecikmeli levha tespiti ve görüntü aktarımı.
* **Sesli Geri Bildirim (TTS):** Tespit edilen levhaların sürücüye Türkçe seslendirilmesi.
* **Modern UI/UX:** Material Design 3 (Material You) standartlarında, dinamik temalı (Gece/Gündüz) arayüz.
* **Gelişmiş Navigasyon:** Google Maps SDK entegrasyonu ile rota çizimi ve anlık hız takibi.
* **Veri Analitiği:** MPAndroidChart ile haftalık katedilen mesafe ve levha tespit oranlarının görselleştirilmesi.
* **Hız Stabilizasyonu:** GPS sapmalarını (Drift) engelleyen özel gürültü filtresi algoritması.

## 🛠 Kullanılan Teknolojiler

* **Dil:** Kotlin
* **Mimari:** MVVM (Model-View-ViewModel)
* **UI:** ViewBinding, ConstraintLayout, Material Components
* **Networking:** OkHttp (WebSocket), Google Maps SDK, Places API
* **Veri Yönetimi:** LiveData, SharedViewModel (Activity-scoped)
* **Analitik:** MPAndroidChart

## 📸 Ekran Görüntüleri

| Ana Ekran (Loglar) | Analiz Paneli | Harita |
| :---: | :---: | :---: |
| ![Home](https://via.placeholder.com/200x400) | ![Dashboard](https://via.placeholder.com/200x400) | ![Map](https://via.placeholder.com/200x400) |

*(Not: Buradaki placeholder linkleri yerine kendi ekran görüntülerini `screenshots/` klasörüne atıp yollarını ekleyebilirsin.)*

## ⚙️ Kurulum ve Çalıştırma

1. Projeyi bilgisayarınıza clone'layın: `git clone https://github.com/kullaniciadin/proje-adin.git`
2. `local.properties` dosyanıza Google Maps API anahtarınızı ekleyin.
3. WebSocket sunucu IP adresini `Yardım` butonuna tıklayarak yardım sayfasından güncelleyin.
4. Android Studio üzerinden cihazınıza yükleyin.

## 👨‍💻 Geliştirici
**Metehan Erkan**
- [LinkedIn](https://www.linkedin.com/in/metehan-erkan-b9a52a1b8/)
- [GitHub](https://github.com/metehanerkan)
