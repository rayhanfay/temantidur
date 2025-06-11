from fastapi import FastAPI, Request, UploadFile, File, HTTPException, Depends, Form
from fastapi.responses import StreamingResponse
from utils.chat_handler import handle_chat_request
from utils.emotion_handler import handle_emotion_detection
from utils.voice_handler import handle_voice_chat
from utils.recap_handler import handle_recap_request  
from utils.error_handler import register_exception_handlers
from config.auth import verify_firebase_token
import logging

app = FastAPI(
    title="Teman Tidur API",
    description="API for AI chat, emotion detection, voice chat, and conversation recap",
    version="1.0.0"
)

# Setup logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Register exception handlers
register_exception_handlers(app)

# ================================
# ========= PRODUCTION ===========
# (With Firebase Authentication)
# ================================

@app.get("/")
async def root():
    return {
        "message": "Welcome to Teman Tidur API 🌙✨",
        "endpoints": {
            "/chat": "Chat with AI",
            "/detect-emotion": "Detect emotion from image",
            "/voice-chat": "Voice chat with AI (WAV only)",
            "/recap": "Generate conversation recap"
        }
    }

@app.post("/chat")
async def chat_endpoint(
    request: Request,
    user=Depends(verify_firebase_token)
):
    """Chat with AI using text input"""
    try:
        return await handle_chat_request(request)
    except Exception as e:
        logger.error(f"Chat error: {str(e)}")
        raise HTTPException(status_code=500, detail="Failed to process chat")

@app.post("/detect-emotion")
async def emotion_detection_endpoint(
    image: UploadFile = File(..., description="Image for emotion detection"),
    language: str = Form("id", description="Response language (id/en)", regex="^(id|en)$"),
    user=Depends(verify_firebase_token)
):
    """Detect emotion from uploaded image with multi-language support"""
    try:
        return await handle_emotion_detection(image, language)
    except Exception as e:
        logger.error(f"Emotion detection error: {str(e)}")
        raise HTTPException(status_code=500, detail="Failed to detect emotion")
    
@app.post("/voice-chat")
async def voice_chat_endpoint(
    audio: UploadFile = File(..., description="WAV audio file for voice chat"),
    user=Depends(verify_firebase_token)
):
    """Voice chat with AI - input WAV audio, output WAV audio"""
    try:
        result = await handle_voice_chat(audio)
        
        return StreamingResponse(
            content=iter([result["audio_bytes"]]),
            media_type="audio/wav",
            headers={
                "X-User-Text": result["user_text"],
                "X-AI-Text": result["ai_text"]
            }
        )
    except Exception as e:
        logger.error(f"Voice chat error: {str(e)}")
        raise HTTPException(status_code=500, detail="Failed to process voice chat")

@app.post("/recap")
async def recap_endpoint(
    request: Request,
    user=Depends(verify_firebase_token)
):
    """Generate conversation recap from chat messages"""
    try:
        return await handle_recap_request(request)
    except Exception as e:
        logger.error(f"Recap error: {str(e)}")
        raise HTTPException(status_code=500, detail="Failed to generate recap")

# ================================
# ========= DEVELOPMENT ==========
# (Without Firebase - for testing)
# ================================

# Uncomment the block below for development mode

# @app.get("/")
# async def root():
#     return {
#         "message": "Teman Tidur API - Development Mode 🌙✨",
#         "note": "Firebase authentication disabled",
#         "supported_languages": {
#             "id": "Bahasa Indonesia (default)",
#             "en": "English"
#         },
#         "endpoints": {
#             "/chat": "Chat with AI companion",
#             "/detect-emotion": "Detect emotion from image",
#             "/voice-chat": "Voice chat with AI (WAV only)",
#             "/recap": "Generate conversation recap"
#         }
#     }

# @app.post("/chat")
# async def chat_dev(request: Request):
#     return await handle_chat_request(request)

# @app.post("/detect-emotion")
# async def detect_emotion_dev(
#     image: UploadFile = File(..., description="Image for emotion detection"),
#     language: str = Form("id", description="Response language: 'id' for Indonesian, 'en' for English")
# ):

#     if language not in ["id", "en"]:
#         language = "id" 
    
#     return await handle_emotion_detection(image, language)

# @app.post("/voice-chat")
# async def voice_chat_dev(audio: UploadFile = File(..., description="WAV audio file")):
#     """Voice chat with AI - input WAV audio, output WAV audio"""
#     try:
#         result = await handle_voice_chat(audio)
#         return StreamingResponse(
#             content=iter([result["audio_bytes"]]),
#             media_type="audio/wav",
#             headers={
#                 "X-User-Text": result["user_text"],
#                 "X-AI-Text": result["ai_text"]
#             }
#         )
#     except Exception as e:
#         logger.error(f"Voice chat dev error: {str(e)}")
#         raise HTTPException(status_code=500, detail="Failed to process voice chat")

# @app.post("/recap")
# async def recap_dev(request: Request):
#     """Generate conversation recap from chat messages"""
#     try:
#         return await handle_recap_request(request)
#     except Exception as e:
#         logger.error(f"Recap dev error: {str(e)}")
#         raise HTTPException(status_code=500, detail="Failed to generate recap")