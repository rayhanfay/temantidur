from fastapi import FastAPI, Request, UploadFile, File, HTTPException, Depends
from fastapi.responses import StreamingResponse
from utils.chat_handler import handle_chat_request
from utils.emotion_handler import handle_emotion_detection
from utils.voice_handler import handle_voice_chat
from utils.error_handler import register_exception_handlers
from config.auth import verify_firebase_token
import logging

app = FastAPI(
    title="Teman Tidur API",
    description="API untuk chat AI, deteksi emosi, dan voice chat",
    version="1.0.0"
)

# Setup logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Register exception handlers
register_exception_handlers(app)

# ================================
# ========= PRODUCTION ===========
# (Dengan Firebase Authentication)
# ================================

@app.get("/")
async def root():
    return {
        "message": "Selamat datang di Teman Tidur API 🌙✨",
        "endpoints": {
            "/chat": "Chat dengan AI",
            "/detect-emotion": "Deteksi emosi dari gambar",
            "/voice-chat": "Voice chat dengan AI (WAV only)"
        }
    }

@app.post("/chat")
async def chat_endpoint(
    request: Request,
    user=Depends(verify_firebase_token)
):
    """Chat dengan AI menggunakan text input"""
    try:
        return await handle_chat_request(request)
    except Exception as e:
        logger.error(f"Chat error: {str(e)}")
        raise HTTPException(status_code=500, detail="Gagal memproses chat")

@app.post("/detect-emotion")
async def emotion_detection_endpoint(
    image: UploadFile = File(..., description="Gambar untuk deteksi emosi"),
    user=Depends(verify_firebase_token)
):
    """Deteksi emosi dari gambar yang diupload"""
    try:
        return await handle_emotion_detection(image)
    except Exception as e:
        logger.error(f"Emotion detection error: {str(e)}")
        raise HTTPException(status_code=500, detail="Gagal mendeteksi emosi")
    
@app.post("/voice-chat")
async def voice_chat_endpoint(
    audio: UploadFile = File(..., description="File audio WAV untuk voice chat"),
    user=Depends(verify_firebase_token)
):
    """Voice chat dengan AI - input audio WAV, output audio WAV"""
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
        raise HTTPException(status_code=500, detail="Gagal memproses voice chat")

# ================================
# ========= DEVELOPMENT ==========
# (Tanpa Firebase - untuk testing)
# ================================

# Uncomment blok di bawah untuk development mode

# @app.get("/")
# async def root():
#     return {
#         "message": "Teman Tidur API - Development Mode 🌙✨",
#         "note": "Firebase authentication disabled"
#     }

# @app.post("/chat")
# async def chat_dev(request: Request):
#     return await handle_chat_request(request)

# @app.post("/detect-emotion")
# async def detect_emotion_dev(image: UploadFile = File(...)):
#     return await handle_emotion_detection(image)

# @app.post("/voice-chat")
# async def voice_chat_dev(audio: UploadFile = File(..., description="File audio WAV")):
#     """Voice chat dengan AI - input audio WAV, output audio WAV"""
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
#         raise HTTPException(status_code=500, detail="Gagal memproses voice chat")