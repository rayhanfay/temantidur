# TemanTidur API 🌙✨

**TemanTidur** is an AI companion specifically designed for teenagers who feel lonely at night. This API provides comprehensive AI chat services, emotion detection from images, voice chat, and conversation recap features to help teenagers overcome overthinking and loneliness.

## 🚀 Main Features

- **💬 AI Chat**: Empathetic conversations with AI that understands teenage feelings
- **😊 Emotion Detection**: Real-time emotion analysis from facial photos using Azure Custom Vision
- **🎤 Voice Chat**: Full voice communication with AI (WAV format input/output)
- **📝 Conversation Recap**: AI-generated summaries of chat conversations with date support
- **🚴 Activity Recommendations**: Personalized calming activities based on emotion analysis
- **🌍 Multi-language Support**: Full support for Indonesian and English with automatic detection
- **🔐 Firebase Authentication**: Secure access with Firebase tokens
- **🛡️ Content Filtering**: Built-in content safety with Azure Content Safety

## 📋 Prerequisites

- Python 3.8+
- Azure Cognitive Services Account (Custom Vision, Speech, OpenAI)
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
# Development endpoints are enabled by default in main.py
uvicorn main:app --reload --host 0.0.0.0 --port 8000
```

**Production Mode** (with Firebase Auth):

```bash
# Comment out development endpoints and uncomment production endpoints in main.py
uvicorn main:app --host 0.0.0.0 --port 8000
```

## 📚 API Documentation

📖 **Complete documentation**: https://documenter.getpostman.com/view/39192802/2sB2x5GsNs

### Base URL

**Development**: `http://localhost:8000`
**Production**: `https://your-domain.com`

### Authentication

**Production Mode**: All endpoints require Firebase ID Token:

```http
Authorization: Bearer <firebase_id_token>
```

**Development Mode**: No authentication required (for testing purposes)

## 🏗️ Project Structure

```
teman-tidur-api/
├── main.py                    # FastAPI app & routes
├── config/
│   ├── azure_config.py       # Azure services configuration
│   └── auth.py               # Firebase authentication
├── utils/
│   ├── chat_handler.py       # Text chat logic & processing
│   ├── emotion_handler.py    # Emotion detection & validation
│   ├── voice_handler.py      # Voice chat processing
│   ├── recap_handler.py      # Conversation recap generation
│   ├── activity_recommender.py # Activity recommendations & emotion messages
│   └── error_handler.py      # Exception handling & error responses
├── requirements.txt          # Python dependencies
├── Dockerfile               # Docker container configuration
├── .env                     # Environment variables
└── README.md               # Project documentation
```

## 🚀 Deployment

### Docker Deployment (Recommended)

```dockerfile
FROM python:3.12.10

WORKDIR /app

COPY requirements.txt .
RUN pip install -r requirements.txt

COPY . .

EXPOSE 8000

CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]
```

**Build and Run:**

```bash
docker build -t temantidur-api .
docker run -p 8000:8000 --env-file .env temantidur-api
```

### Azure Deployment Options

- **Azure Container Apps**: Recommended for scalability
- **Azure App Service**: For simpler deployments
- **Azure Container Instances**: For development/testing

### Required Azure Services

- **Azure OpenAI**: For AI chat capabilities
- **Azure Custom Vision**: For emotion detection
- **Azure Speech Services**: For voice chat functionality

## 📊 Dependencies

```txt
fastapi                    # Web framework
uvicorn                   # ASGI server
python-dotenv            # Environment variable management
requests                 # HTTP client for Azure APIs
firebase-admin           # Firebase authentication
python-multipart         # File upload support
azure-cognitiveservices-speech  # Speech services
openai>=1.82.1          # Azure OpenAI client
```

## 🧪 Testing

### Development Mode

```bash
# Start development server
uvicorn main:app --reload

# Test endpoints without authentication
curl http://localhost:8000/
curl -X POST http://localhost:8000/chat -H "Content-Type: application/json" -d '{"messages": [{"role": "user", "content": "Hello"}]}'
```

### Production Testing

```bash
# Requires Firebase token
curl -X POST https://your-domain.com/chat \
  -H "Authorization: Bearer YOUR_FIREBASE_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"messages": [{"role": "user", "content": "Hello"}]}'
```

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 🙏 Acknowledgments

- **Azure Cognitive Services** for comprehensive AI capabilities
- **Firebase** for secure authentication infrastructure
- **FastAPI** for modern, fast web framework
- **OpenAI** for conversational AI technology
- **Python Community** for excellent libraries and tools

**Made with ❤️ for Indonesian teenagers who need someone to talk to at night** 🌙✨
