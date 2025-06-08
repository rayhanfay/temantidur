# TemanTidur API 🌙✨

**TemanTidur** adalah AI companion khusus untuk remaja yang merasa kesepian di malam hari. API ini menyediakan layanan chat AI, deteksi emosi dari gambar, dan voice chat untuk membantu remaja mengatasi overthinking dan kesepian.

## 🚀 Fitur Utama

- **💬 Chat AI**: Percakapan empati dengan AI yang memahami perasaan remaja
- **😊 Deteksi Emosi**: Analisis emosi dari foto wajah menggunakan Azure Custom Vision
- **🎤 Voice Chat**: Komunikasi suara dengan AI (format WAV)
- **🌍 Multi-bahasa**: Mendukung Bahasa Indonesia dan English
- **🔐 Firebase Authentication**: Keamanan dengan token Firebase

## 📋 Prerequisites

- Python 3.8+
- Azure Cognitive Services Account
- Firebase Project dengan Authentication
- Azure OpenAI Access

## 🛠️ Installation

### 1. Clone Repository

```bash
git clone https://github.com/rayhanfay/temantidur.git
cd teman-tidur-api
```

### 2. Install Dependencies

```bash
pip install -r requirements.txt
```

### 3. Environment Configuration

Buat file `.env` di root project:

```env
# Azure Custom Vision
AZURE_CUSTOM_VISION_ENDPOINT=https://your-custom-vision.cognitiveservices.azure.com/customvision/v3.0/Prediction/your-project-id/classify/iterations/your-iteration/image
AZURE_CUSTOM_VISION_KEY=your_custom_vision_key

# Azure Speech Service
AZURE_SPEECH_KEY=your_speech_key
AZURE_SPEECH_REGION=your_region

# Azure OpenAI
AZURE_OPENAI_ENDPOINT=https://your-openai.openai.azure.com/
AZURE_OPENAI_KEY=your_openai_key
AZURE_API_VERSION=2024-02-15-preview
AZURE_DEPLOYMENT_NAME=your_deployment_name

# Firebase Service Account JSON (as stringified JSON)
FIREBASE_JSON={"type":"service_account","project_id":"your-project",...}
```

### 4. Run Server

**Development Mode** (tanpa Firebase Auth):

```bash
# Uncomment development endpoints di main.py
uvicorn main:app --reload
```

**Production Mode** (dengan Firebase Auth):

```bash
uvicorn main:app --host 0.0.0.0 --port 8000
```

## 📚 API Documentation

📖 **Dokumentasi lengkap**: https://documenter.getpostman.com/view/39192802/2sB2qi9JHZ

### Base URL

```
https://your-domain.com/
```

### Authentication

Semua endpoint memerlukan Firebase ID Token:

```http
Authorization: Bearer <firebase_id_token>
```

## 🎯 Endpoints

### 1. Root Endpoint

```http
GET /
```

Informasi dasar API dan endpoint yang tersedia.

### 2. Chat AI

```http
POST /chat
Content-Type: application/json
Authorization: Bearer <token>

{
  "messages": [
    {
      "role": "user",
      "content": "Aku merasa sedih malam ini"
    }
  ],
  "max_tokens": 200,
  "temperature": 0.9
}
```

**Response:**

```json
{
  "reply": "Aku mengerti kamu sedang merasa sedih malam ini. Aku di sini untuk mendengarkan. Mau cerita apa yang bikin kamu merasa seperti ini? 🌙💙"
}
```

### 3. Deteksi Emosi

```http
POST /detect-emotion
Content-Type: multipart/form-data
Authorization: Bearer <token>

image: <file.jpg>
```

**Response:**

```json
{
  "emotion": "sedih",
  "confidence": 0.85,
  "recommendation": "Aku bisa lihat kamu sedang merasa sedih malam ini. Coba dengarkan musik menenangkan atau tuliskan perasaanmu di journal. Aku di sini bersamamu. 💙",
  "message": "Aku bisa melihat dari ekspresi wajah kamu bahwa kamu sedang merasa sedih malam ini. Terima kasih sudah mau berbagi perasaanmu dengan aku! 🌙✨"
}
```

### 4. Voice Chat

```http
POST /voice-chat
Content-Type: multipart/form-data
Authorization: Bearer <token>

audio: <file.wav>
```

**Response:**

- Content-Type: `audio/wav`
- Headers:
  - `X-User-Text`: Teks yang diucapkan user
  - `X-AI-Text`: Respons AI dalam teks
- Body: Audio WAV dari respons AI

## 🏗️ Struktur Project

```
teman-tidur-api/
├── main.py                    # FastAPI app & routes
├── config/
│   ├── azure_config.py       # Azure services config
│   └── auth.py               # Firebase authentication
├── utils/
│   ├── chat_handler.py       # Text chat logic
│   ├── emotion_handler.py    # Emotion detection
│   ├── voice_handler.py      # Voice chat processing
│   ├── activity_recommender.py # Activity recommendations
│   └── error_handler.py      # Exception handling
├── requirements.txt
├── .env
└── README.md
```

## 🔧 Key Features Detail

### Multi-Language Support

- **Deteksi otomatis**: API mendeteksi bahasa dari input user

### Error Handling

- **Azure Content Filter**: Respons khusus saat konten difilter
- **Network Issues**: Fallback responses untuk masalah koneksi
- **Rate Limiting**: Handling untuk rate limit Azure
- **Validation**: Error handling untuk format file dan input

### Audio Processing

- **Format**: Hanya mendukung WAV format
- **Transcription**: Azure Speech-to-Text
- **Synthesis**: Azure Text-to-Speech dengan voice neural
- **Languages**:
  - Indonesian: `id-ID-GadisNeural`
  - English: `en-US-JennyNeural`

## 🔍 Monitoring & Logging

API menggunakan Python logging untuk monitoring:

- Info level untuk operasi normal
- Warning untuk content filtering
- Error untuk exceptions dan failures

## 🚀 Deployment

### Docker (Recommended)

```dockerfile
FROM python:3.12.10

WORKDIR /app
COPY requirements.txt .
RUN pip install -r requirements.txt

COPY . .
EXPOSE 8000

CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]
```

### Azure Services

- **Azure Container App**
- **Azure OpenAI**
- **Azure Custom Vision**
- **Azure Speech**

## 🛡️ Security

- **Firebase Authentication**: Token verification untuk setiap request
- **Input Validation**: Validasi format file dan ukuran
- **Content Filtering**: Azure Content Safety integration
- **Rate Limiting**: Handling built-in dari Azure services

## 🤝 Contributing

1. Fork repository
2. Create feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to branch (`git push origin feature/AmazingFeature`)
5. Create Pull Request

## 🙏 Acknowledgments

- **Azure Cognitive Services** untuk AI capabilities
- **Firebase** untuk authentication
- **FastAPI** untuk web framework
- **OpenAI** untuk conversational AI

---

**Made with ❤️ for Indonesian teenagers who need someone to talk to at night** 🌙✨
