# Teman Tidur - Your Comfy Shadow🌙✨

<div align="center">
  
  ![Teman Tidur Logo](images/cover.png)
  
  [![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://android.com)
  [![Python](https://img.shields.io/badge/Backend-Python-blue.svg)](https://python.org)
  [![API](https://img.shields.io/badge/API-24%2B-brightgreen.svg)](https://android-arsenal.com/api?level=24)
  [![FastAPI](https://img.shields.io/badge/Framework-FastAPI-009688.svg)](https://fastapi.tiangolo.com)
  
  **Your Comfy Shadow - AI Companion for Teenagers**
  
  *Innovative AI-powered virtual companion designed to overcome loneliness and provide emotional support 24/7*
  
</div>

---

## 🎯 About Teman Tidur

**Teman Tidur** is a comprehensive AI companion solution specifically designed for teenagers who feel lonely at night. Our project combines cutting-edge Android mobile application with a robust cloud-based API to provide empathetic conversations, emotion detection, voice interaction, and personalized recommendations.

**APK Download**: [TemanTidur_Version_1.0.apk](https://github.com/rayhanfay/temantidur/releases/latest/download/TemanTidur_Version_1.0.apk)

### 🌟 Key Features

| Feature                         | Description                                                             |
| ------------------------------- | ----------------------------------------------------------------------- |
| 🔐 **Authentication**           | Secure registration and login with Firebase Auth                        |
| 💬 **AI Chat**                  | Empathetic conversations with AI that understands teenage feelings      |
| 🎤 **Voice Chat**               | Full voice communication with AI (WAV format input/output)              |
| 😊 **Emotion Detection**        | Real-time emotion analysis from facial photos using Azure Custom Vision |
| 📝 **Daily Recap**              | AI-generated summaries of chat conversations with date support          |
| 🎯 **Activity Recommendations** | Personalized calming activities based on emotion analysis               |
| 🌍 **Multi-language Support**   | Full support for Indonesian and English with automatic detection        |
| 📱 **Mobile Widgets**           | Quick access through home screen widgets                                |

---

### Mockup Overview

![Mockup](images/mockup.png)

## 🏗️ Architecture & Tech Stack

### System Architecture

![Application Workflow](images/workflow.png)

The application follows a modern microservices architecture with:

1. **Android Mobile App** - Clean Architecture with MVVM pattern
2. **Python FastAPI Backend** - RESTful API with Azure integration
3. **Firebase Services** - Authentication and real-time database
4. **Azure AI Services** - OpenAI, Custom Vision, and Speech services

### Tech Stack Overview

![Tech Stack](images/tech_stack.png)

#### 📱 Mobile Application

- **Language**: [Kotlin](https://kotlinlang.org/) - 100% Kotlin
- **Architecture**: MVVM + Clean Architecture
- **Async**: [Coroutines](https://kotlinlang.org/docs/coroutines-overview.html) & [Flow](https://developer.android.com/kotlin/flow)
- **UI Framework**: [View Binding](https://developer.android.com/topic/libraries/view-binding) & [Data Binding](https://developer.android.com/topic/libraries/data-binding)
- **Design System**: [Material Design Components](https://material.io/develop/android)
- **DI**: [Hilt](https://dagger.dev/hilt/)
- **Networking**: [Retrofit2](https://square.github.io/retrofit/) + [OkHttp3](https://square.github.io/okhttp/)

#### ☁️ Backend API

- **Language**: [Python 3.8+](https://python.org)
- **Framework**: [FastAPI](https://fastapi.tiangolo.com)
- **AI Services**: [Azure OpenAI](https://azure.microsoft.com/en-us/products/ai-services/openai-service), [Azure Custom Vision](https://azure.microsoft.com/en-us/products/ai-services/custom-vision), [Azure Speech](https://azure.microsoft.com/en-us/products/ai-services/speech-to-text)
- **Authentication**: [Firebase Admin SDK](https://firebase.google.com/docs/admin/setup)
- **Deployment**: [Docker](https://docker.com) + [Azure Container Apps](https://azure.microsoft.com/en-us/products/container-apps)

#### 🔧 Services & Infrastructure

- **Authentication**: [Firebase Auth](https://firebase.google.com/docs/auth)
- **Database**: [Firebase Realtime Database](https://firebase.google.com/docs/database)
- **Storage**: [Cloud Storage for Firebase](https://firebase.google.com/docs/storage)
- **AI Services**: [Azure Cognitive Services](https://azure.microsoft.com/en-us/products/ai-services)
- **Content Safety**: [Azure Content Safety](https://azure.microsoft.com/en-us/products/ai-services/content-safety)

---

## 👥 Team & Responsibilities

![Jobdesk](images/jobdesk.png)

| Role                     | Member                                                                                              | Responsibilities                                         |
| ------------------------ | --------------------------------------------------------------------------------------------------- | -------------------------------------------------------- |
| 📋 **Product Manager**   | [Rayhan Al Farassy](https://github.com/rayhanfay)                                                   | Project management, Azure integration, API documentation |
| 🎨 **UI/UX Designer**    | [Rayhan Al Farassy](https://github.com/rayhanfay) & [Agus Syuhada](https://github.com/AgusSyuhada)  | App design, user experience, visual identity             |
| 📱 **Android Developer** | [Agus Syuhada](https://github.com/AgusSyuhada) & [M Hashfi Fanny AYD](https://github.com/hashfiayd) | Mobile app development, Firebase integration             |
| ☁️ **Cloud Engineer**    | [Rayhan Al Farassy](https://github.com/rayhanfay)                                                   | Backend API, Azure services, deployment                  |

### Individual Contributions

#### Rayhan Al Farassy

- Creating base app mockup, color palette and font
- Integrating Azure OpenAI Chat Completion
- Integrating Azure Custom Vision AI
- Integrating Azure Speech services
- AI backend API implementation
- Integrating API with Azure Container App
- Postman and GitHub documentation

#### Agus Syuhada

- Creating logo application and onboarding images
- Implementing profile, about, and settings sections
- Creating layout shift with transition animation
- Creating daily reminder notification system
- Implementing hold-to-talk voice feature
- Creating widget for easy navigation

#### M Hashfi Fanny AYD

- Creating mobile application based on mockup design
- Integrating Firebase Authentication for login/register
- Implementing chat feature with AI real-time messaging
- Developing emotion detection feature integration
- Creating activity recommendation system based on user emotion
- Implementing chat recap functionality

---

## 🚀 Getting Started

### Prerequisites

- **Android Studio**: Iguana 2023.2.1+
- **Python**: 3.8+
- **JDK**: Version 17
- **Android SDK**: API level 24+
- **Azure Account**: For AI services
- **Firebase Project**: For authentication and database

### 📱 Mobile App Setup

1. **Clone Repository**

   ```bash
   git clone https://github.com/rayhanfay/temantidur.git
   cd temantidur
   git checkout mobile
   ```

2. **Firebase Configuration**

   - Create Firebase project at [Firebase Console](https://console.firebase.google.com/)
   - Add Android app with package: `com.hackathon.temantidur`
   - Download `google-services.json` → place in `app/`

3. **API Configuration**

   ```properties
   # Create secrets.properties in root directory
   API_BASE_URL="YOUR_CHAT_API_URL_HERE"
   EMOTION_API_BASE_URL="YOUR_EMOTION_API_URL_HERE"
   ```

4. **Run Application**
   - Open project in Android Studio
   - Sync Gradle files
   - Run on emulator or device

### ☁️ Backend API Setup

1. **Switch to Backend Branch**

   ```bash
   git checkout backend-api
   ```

2. **Install Dependencies**

   ```bash
   pip install -r requirements.txt
   ```

3. **Environment Configuration**

   ```env
   # Create .env file
   AZURE_CUSTOM_VISION_ENDPOINT=your_custom_vision_endpoint
   AZURE_CUSTOM_VISION_KEY=your_custom_vision_key
   AZURE_SPEECH_KEY=your_speech_key
   AZURE_SPEECH_REGION=your_region
   AZURE_OPENAI_ENDPOINT=your_openai_endpoint
   AZURE_OPENAI_KEY=your_openai_key
   FIREBASE_JSON={"type":"service_account",...}
   ```

4. **Run Server**

   ```bash
   # Development mode
   uvicorn main:app --reload --host 0.0.0.0 --port 8000

   # Production mode
   uvicorn main:app --host 0.0.0.0 --port 8000
   ```

---

## 🔄 Application Workflow

The application follows a sophisticated workflow integrating multiple AI services:

1. **User Authentication** - Firebase handles secure login/register
2. **Chat Interaction** - Users engage with AI through text or voice
3. **API Processing** - Backend routes requests to appropriate Azure services
4. **AI Analysis** - Azure OpenAI processes conversations and generates responses
5. **Emotion Detection** - Azure Custom Vision analyzes facial expressions
6. **Response Delivery** - Mobile app receives and displays AI responses
7. **Personalization** - System provides recommendations based on user emotions

---

## 🛣️ Future Development Plans

![Future Plans](images/futuredevelopment.png)

Our roadmap includes exciting enhancements:

- **Improved Emotion Detection** - Enhanced accuracy and more emotion categories
- **Statistics & Predictions** - Emotional patterns and mood tracking
- **Diverse AI Chat Styles** - Multiple personality options for the AI companion
- **Activity Recommendations** - Screen time-based suggestions
- **Background Recognition** - Context-aware conversations
- **Cloud Storage Integration** - Cross-device synchronization
- **Web Application** - Browser-based access
- **3D Avatar Visualization** - Immersive AI companion experience
- **Interactive Animations** - Enhanced user interface engagement

---

## 📁 Project Structure

```
temantidur/
├── 📱 mobile/                    # Android application
│   └── app/src/main/java/com/hackathon/temantidur/
│       ├── common/           # Shared utilities & models
│       ├── data/             # Data layer implementation
│       ├── di/               # Dependency injection modules
│       ├── domain/           # Business logic layer
│       ├── presentation/     # UI components
│       └── utils/            # Helper classes
├── ☁️ backend-api/              # Python FastAPI backend
│   ├── main.py                  # FastAPI app & routes
│   ├── config/                  # Azure services configuration
│   ├── utils/                   # Backend utilities
│   └── requirements.txt         # Python dependencies
├── 📸 images/                   # Project documentation images
└── 📋 README.md                # This documentation
```

---

## 📖 Documentation

- **📱 Mobile App**: Detailed setup and development guide in `/mobile` branch
- **☁️ Backend API**: Complete API documentation in `/backend-api` branch
- **🔗 API Reference**: [Postman Documentation](https://documenter.getpostman.com/view/39192802/2sB2x5GsNs)
- **🎨 Design Assets**: Available in `/images` folder

---

## 🤝 Contributing

We welcome contributions from the community! Here's how you can help:

1. **Fork** the repository
2. **Create** a feature branch (`git checkout -b feature/AmazingFeature`)
3. **Commit** your changes (`git commit -m 'Add some AmazingFeature'`)
4. **Push** to the branch (`git push origin feature/AmazingFeature`)
5. **Open** a Pull Request

### Development Guidelines

- Follow the existing code style and architecture patterns
- Write comprehensive tests for new features
- Update documentation when adding new functionality
- Ensure all CI/CD checks pass before submitting PR

---

## ⭐ Acknowledgments

- **Azure Cognitive Services** for comprehensive AI capabilities
- **Firebase** for secure authentication infrastructure
- **FastAPI** for modern, fast web framework
- **Android Community** for excellent development tools
- **Open Source Community** for inspiration and support

---

<div align="center">
  
  **⭐ Don't forget to give a star if this project helps you! ⭐**
  
  **Made with ❤️ in Indonesia for teenagers who need someone to talk to at night** 🌙✨
  
  ---
  
  © 2025 Teman Tidur Team. All rights reserved.
  
</div>
