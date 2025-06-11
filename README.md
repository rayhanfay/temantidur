# Teman Tidur 🌙✨

<div align="center">
  
  [![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://android.com)
  [![Kotlin](https://img.shields.io/badge/Language-Kotlin-blue.svg)](https://kotlinlang.org)
  [![API](https://img.shields.io/badge/API-24%2B-brightgreen.svg)](https://android-arsenal.com/api?level=24)
  
  **Innovative Android application designed as a virtual companion with AI technology**
  
  *Overcoming loneliness and providing emotional support 24/7*
  
</div>

---

## 📱 About the App

**Teman Tidur** is an Android application that utilizes AI technology to provide a virtual companion that's always ready to listen. This app is equipped with emotion detection, voice chat, and daily recap features to help users in self-reflection.

### ✨ Key Features

| Feature | Description |
|---------|-------------|
| 🔐 **Authentication** | Secure registration and login with Firebase Auth |
| 💬 **Interactive Chat** | AI communication through real-time text messages |
| 🎤 **Voice Chat** | Responsive voice interaction |
| 😊 **Emotion Detection** | Facial expression analysis using camera |
| 📝 **Daily Recap** | Automatic conversation summary for reflection |
| 🎯 **Recommendations** | Activity recommendations based on emotional state |
| 👤 **Profile Management** | Username, password, and profile picture settings |
| 🌍 **Multi-language** | Support for Indonesian and English |
| 📱 **Widget & Onboarding** | Quick access and user-friendly experience |

---

## 🏗️ Architecture

This application is built using **Clean Architecture** with **MVVM (Model-View-ViewModel)** pattern.

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
- **Android Studio**: Iguana 2023.2.1 or newer
- **JDK**: Version 17
- **Android SDK**: API level 24+

### 1️⃣ Clone Repository
```bash
git clone https://github.com/aripov21/TemanTidur.git
cd TemanTidur
```

### 2️⃣ Firebase Setup
1. Open [Firebase Console](https://console.firebase.google.com/)
2. Create a new project
3. Add Android app with package: `com.hackathon.temantidur`
4. Download `google-services.json` → place in `app/`

### 3️⃣ API Configuration
Create `secrets.properties` file in root directory:
```properties
# Chat & Recap API
API_BASE_URL="YOUR_CHAT_API_URL_HERE"

# Emotion Detection API  
EMOTION_API_BASE_URL="YOUR_EMOTION_API_URL_HERE"
```

### 4️⃣ Run Application
1. Open project in Android Studio
2. Sync Gradle files
3. Run on emulator or device

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

We welcome contributions from other developers! Please:

1. Fork this repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

---

## 👥 Team

| Role | Member |
|------|--------|
| 📋 **Product Manager** | [Rayhan Al Farassy](https://github.com/rayhanfay) |
| 🎨 **UI/UX Designer** | [Rayhan Al Farassy](https://github.com/rayhanfay), [Agus Syuhada](https://github.com/AgusSyuhada) |
| 📱 **Android Developer** | [Agus Syuhada](https://github.com/AgusSyuhada), [M Hashfi Fanny AYD](https://github.com/hashfiayd) |
| ☁️ **Cloud Engineer** | [Rayhan Al Farassy](https://github.com/rayhanfay) |

---

## 📚 Documentation

- 📖 **GitHub Repository**: [https://github.com/rayhanfay/temantidur.git](https://github.com/rayhanfay/temantidur.git)
- 🚀 **API Documentation**: [Postman Collection](https://documenter.getpostman.com/view/39192802/2sB2x5GsNs)
- 🎥 **Video Documentation**: [Coming Soon](https://example.com) *(Link will be updated)*

---

## 📞 Support

If you have any questions or issues:

- 📧 **Email**: yourcomfyshadow@gmail.com

---

<div align="center">
  
  **⭐ Don't forget to give a star if this project helps you! ⭐**
  
  Made with ❤️ in Indonesia
  
</div>
