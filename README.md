# Teman Tidur 🌙

<div align="center">
  
  [![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://android.com)
  [![Kotlin](https://img.shields.io/badge/Language-Kotlin-blue.svg)](https://kotlinlang.org)
  [![API](https://img.shields.io/badge/API-24%2B-brightgreen.svg)](https://android-arsenal.com/api?level=24)
  
  **Aplikasi Android inovatif yang dirancang sebagai teman curhat virtual dengan teknologi AI**
  
  *Mengatasi perasaan kesepian dan memberikan dukungan emosional 24/7*
  
</div>

---

## 📱 Tentang Aplikasi

**Teman Tidur** adalah aplikasi Android yang memanfaatkan teknologi AI untuk memberikan teman bicara virtual yang selalu siap mendengarkan. Aplikasi ini dilengkapi dengan fitur deteksi emosi, voice chat, dan rekapitulasi harian untuk membantu pengguna dalam refleksi diri.

### ✨ Fitur Utama

| Fitur | Deskripsi |
|-------|-----------|
| 🔐 **Authentication** | Registrasi dan login aman dengan Firebase Auth |
| 💬 **Chat Interaktif** | Komunikasi AI melalui pesan teks real-time |
| 🎤 **Voice Chat** | Interaksi suara yang natural dan responsif |
| 😊 **Deteksi Emosi** | Analisis ekspresi wajah menggunakan kamera |
| 📝 **Rekap Harian** | Rangkuman percakapan otomatis untuk refleksi |
| 🎯 **Rekomendasi** | Saran konten berdasarkan kondisi emosional |
| 👤 **Manajemen Profil** | Pengaturan username, password, dan foto profil |
| 🌍 **Multi-bahasa** | Dukungan Bahasa Indonesia dan Inggris |
| 📱 **Widget & Onboarding** | Akses cepat dan pengalaman pengguna yang ramah |

---

## 🏗️ Arsitektur

Aplikasi ini dibangun menggunakan **Clean Architecture** dengan pola **MVVM (Model-View-ViewModel)**.

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   PRESENTATION  │    │      DOMAIN     │    │      DATA       │
│                 │    │                 │    │                 │
│  • Activities   │◄──►│  • Use Cases    │◄──►│  • Repositories │
│  • Fragments    │    │  • Entities     │    │  • Data Sources │
│  • ViewModels   │    │  • Interfaces   │    │  • API Services │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

### 📂 Layer Responsibilities

- **🎨 Presentation**: UI components (Activities, Fragments, ViewModels)
- **💼 Domain**: Business logic (Use Cases, Entities, Repository interfaces)
- **🗄️ Data**: Data management (Repository implementations, API services, local storage)

---

## 🛠️ Tech Stack

### Core Technologies
- **Language**: [Kotlin](https://kotlinlang.org/) - 100% Kotlin
- **Architecture**: MVVM + Clean Architecture
- **Async**: [Coroutines](https://kotlinlang.org/docs/coroutines-overview.html) & [Flow](https://developer.android.com/kotlin/flow)

### UI & UX
- **UI Framework**: [View Binding](https://developer.android.com/topic/libraries/view-binding) & [Data Binding](https://developer.android.com/topic/libraries/data-binding)
- **Design System**: [Material Design Components](https://material.io/develop/android)
- **Animations**: [Lottie](https://github.com/airbnb/lottie-android)
- **Image Loading**: [Glide](https://github.com/bumptech/glide)

### Backend & Services
- **Authentication**: [Firebase Auth](https://firebase.google.com/docs/auth)
- **Database**: [Firebase Realtime Database](https://firebase.google.com/docs/database)
- **Storage**: [Cloud Storage for Firebase](https://firebase.google.com/docs/storage)
- **Local DB**: [Room](https://developer.android.com/jetpack/androidx/releases/room)

### Development Tools
- **DI**: [Hilt](https://dagger.dev/hilt/)
- **Networking**: [Retrofit2](https://square.github.io/retrofit/) + [OkHttp3](https://square.github.io/okhttp/)
- **JSON Parsing**: [Gson](https://github.com/google/gson)
- **HTTP Inspection**: [Chucker](https://github.com/ChuckerTeam/chucker)
- **Navigation**: [Navigation Component](https://developer.android.com/guide/navigation)

---

## 🚀 Quick Start

### Prerequisites
- **Android Studio**: Iguana 2023.2.1 atau lebih baru
- **JDK**: Version 17
- **Android SDK**: API level 24+

### 1️⃣ Clone Repository
```bash
git clone https://github.com/aripov21/TemanTidur.git
cd TemanTidur
```

### 2️⃣ Firebase Setup
1. Buka [Firebase Console](https://console.firebase.google.com/)
2. Buat proyek baru
3. Tambahkan Android app dengan package: `com.hackathon.temantidur`
4. Download `google-services.json` → letakkan di `app/`

### 3️⃣ API Configuration
Buat file `secrets.properties` di root directory:
```properties
# Chat & Recap API
API_BASE_URL="YOUR_CHAT_API_URL_HERE"

# Emotion Detection API  
EMOTION_API_BASE_URL="YOUR_EMOTION_API_URL_HERE"
```

### 4️⃣ Run Application
1. Buka project di Android Studio
2. Sync Gradle files
3. Run di emulator atau device

---

## 📁 Project Structure

```
temantidur/
└── app/
    └── src/main/java/com/hackathon/temantidur/
        ├── 🔧 common/           # Shared utilities & models
        ├── 🗄️ data/             # Data layer
        │   ├── auth/            # Authentication data source
        │   ├── chat/            # Chat data management  
        │   └── emotion/         # Emotion detection data
        ├── 💉 di/               # Dependency injection modules
        ├── 💼 domain/           # Business logic layer
        │   ├── entities/        # Data models
        │   ├── repositories/    # Repository interfaces
        │   └── usecases/        # Use case implementations
        ├── 🎨 presentation/     # UI layer
        │   ├── auth/            # Login/Register screens
        │   ├── chat/            # Chat interface
        │   ├── emotion/         # Emotion detection UI
        │   ├── mainmenu/        # Main navigation
        │   ├── onboarding/      # First-time user experience
        │   ├── recommendation/  # Content recommendations
        │   ├── sidemenu/        # Side navigation drawer
        │   ├── splash/          # App launch screen
        │   └── voicechat/       # Voice interaction UI
        ├── 📡 receivers/        # Broadcast receivers
        ├── 🔄 services/         # Background services
        ├── 🛠️ utils/            # Helper classes
        └── 📱 widgets/          # Home screen widgets
```

---

## 🤝 Contributing

Kami menyambut kontribusi dari developer lain! Silakan:

1. Fork repository ini
2. Buat feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit perubahan (`git commit -m 'Add some AmazingFeature'`)
4. Push ke branch (`git push origin feature/AmazingFeature`)
5. Buka Pull Request

---

## 👥 Team

| Role | Member |
|------|--------|
| 📋 **Product Manager** | [Rayhan Al Farassy](https://github.com/rayhanfay) |
| 🎨 **UI/UX Designer** | [Rayhan Al Farassy](https://github.com/rayhanfay), [Agus Syuhada](https://github.com/AgusSyuhada) |
| 📱 **Android Developer** | [Agus Syuhada](https://github.com/AgusSyuhada), [M Hashfi Fanny AYD](https://github.com/hashfiayd) |
| ☁️ **Cloud Engineer** | [Rayhan Al Farassy](https://github.com/rayhanfay) |

---

## 📞 Support

Jika Anda memiliki pertanyaan atau masalah:

- 📧 **Email**: yourcomfyshadow@gmail.com

---

<div align="center">
  
  **⭐ Jangan lupa berikan star jika project ini membantu! ⭐**
  
  Made with ❤️ in Indonesia
  
</div>
