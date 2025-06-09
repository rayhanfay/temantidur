# TemanTidur API 🌙✨

**TemanTidur** is an AI companion specifically designed for teenagers who feel lonely at night. This API provides AI chat services, emotion detection from images, and voice chat to help teenagers overcome overthinking and loneliness.

## 🚀 Main Features

- **💬 AI Chat**: Empathetic conversations with AI that understands teenage feelings
- **😊 Emotion Detection**: Emotion analysis from facial photos using Azure Custom Vision
- **🚴 Activity Recommendations**: Provides positive activities based on emotion analysis results
- **🎤 Voice Chat**: Voice communication with AI (WAV format)
- **🌍 Multi-language**: Supports Indonesian and English
- **🔐 Firebase Authentication**: Security with Firebase tokens

## 📋 Prerequisites

- Python 3.8+
- Azure Cognitive Services Account
- Firebase Project with Authentication
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

Create a `.env` file in the project root:

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

**Development Mode** (without Firebase Auth):

```bash
# Uncomment development endpoints in main.py
uvicorn main:app --reload
```

**Production Mode** (with Firebase Auth):

```bash
uvicorn main:app --host 0.0.0.0 --port 8000
```

## 📚 API Documentation

📖 **Complete documentation**: https://documenter.getpostman.com/view/39192802/2sB2qi9JHZ

### Base URL

```
https://your-domain.com/
```

### Authentication

All endpoints require Firebase ID Token:

```http
Authorization: Bearer <firebase_id_token>
```

## 🎯 Endpoints

### 1. Root Endpoint

```http
GET /
```

Basic API information and available endpoints.

### 2. AI Chat

```http
POST /chat
Content-Type: application/json
Authorization: Bearer <token>

{
  "messages": [
    {
      "role": "user",
      "content": "I feel sad tonight"
    }
  ],
  "max_tokens": 200,
  "temperature": 0.9
}
```

**Response:**

```json
{
  "reply": "I understand you're feeling sad tonight. I'm here to listen. Would you like to share what's making you feel this way? 🌙💙"
}
```

### 3. Emotion Detection

```http
POST /detect-emotion
Content-Type: multipart/form-data
Authorization: Bearer <token>

image: <file.jpg>
```

**Response:**

```json
{
  "emotion": "sad",
  "confidence": 0.85,
  "recommendation": "I can see you're feeling sad tonight. Try listening to calming music or writing your feelings in a journal. I'm here with you. 💙",
  "message": "I can see from your facial expression that you're feeling sad tonight. Thank you for sharing your feelings with me! 🌙✨"
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
  - `X-User-Text`: Text spoken by user
  - `X-AI-Text`: AI response in text
- Body: WAV audio of AI response

## 🏗️ Project Structure

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

- **Automatic detection**: API automatically detects language from user input

### Error Handling

- **Azure Content Filter**: Specific responses when content is filtered
- **Network Issues**: Fallback responses for connection problems
- **Rate Limiting**: Handling for Azure rate limits
- **Validation**: Error handling for file formats and input

### Audio Processing

- **Format**: Only supports WAV format
- **Transcription**: Azure Speech-to-Text
- **Synthesis**: Azure Text-to-Speech with neural voice
- **Languages**:
  - Indonesian: `id-ID-GadisNeural`
  - English: `en-US-JennyNeural`

## 🔍 Monitoring & Logging

API uses Python logging for monitoring:

- Info level for normal operations
- Warning for content filtering
- Error for exceptions and failures

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

- **Firebase Authentication**: Token verification for every request
- **Input Validation**: File format and size validation
- **Content Filtering**: Azure Content Safety integration
- **Rate Limiting**: Built-in handling from Azure services

## 🤝 Contributing

1. Fork repository
2. Create feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to branch (`git push origin feature/AmazingFeature`)
5. Create Pull Request

## 🙏 Acknowledgments

- **Azure Cognitive Services** for AI capabilities
- **Firebase** for authentication
- **FastAPI** for web framework
- **OpenAI** for conversational AI

---

**Made with ❤️ for Indonesian teenagers who need someone to talk to at night** 🌙✨
