from fastapi import UploadFile, HTTPException, Form
from config.azure_config import AZURE_CUSTOM_VISION_ENDPOINT, AZURE_CUSTOM_VISION_KEY
from utils.activity_recommender import recommend_activity_for_emotion, generate_emotion_recognition_message
from openai import BadRequestError
import logging
import requests
from requests.exceptions import ConnectionError, Timeout, RequestException

logger = logging.getLogger(__name__)

def detect_emotion_from_image(image_data: bytes):
    """Deteksi emosi dari gambar menggunakan Azure Custom Vision"""
    url = AZURE_CUSTOM_VISION_ENDPOINT
    headers = {
        'Prediction-Key': AZURE_CUSTOM_VISION_KEY,
        'Content-Type': 'application/octet-stream'
    }

    try:
        response = requests.post(url, headers=headers, data=image_data, timeout=30)
        
        if response.status_code != 200:
            return {
                "error": "Azure Vision Error", 
                "detail": f"Status: {response.status_code}, Response: {response.text}"
            }

        predictions = response.json().get("predictions", [])
        if not predictions:
            return {"error": "No emotion detected"}

        top_emotion = max(predictions, key=lambda x: x["probability"])
        return {
            "emotion": top_emotion["tagName"],
            "confidence": top_emotion["probability"]
        }
    
    except ConnectionError as e:
        logger.error(f"Connection error to Azure Vision: {str(e)}")
        return {
            "error": "Connection Failed",
            "detail": "Tidak bisa terhubung ke layanan deteksi emosi. Coba lagi nanti ya!"
        }
    
    except Timeout as e:
        logger.error(f"Timeout error to Azure Vision: {str(e)}")
        return {
            "error": "Request Timeout", 
            "detail": "Proses deteksi emosi membutuhkan waktu terlalu lama. Coba foto yang lebih kecil atau coba lagi."
        }
    
    except RequestException as e:
        logger.error(f"Request exception to Azure Vision: {str(e)}")
        return {
            "error": "Request Failed",
            "detail": "Gagal memproses permintaan ke layanan deteksi emosi."
        }

def get_error_messages(language: str = "id"):
    """Get error messages based on language"""
    messages = {
        "id": {
            "invalid_file_type": "Maaf, aku hanya bisa membaca foto. Coba upload file gambar (JPG, PNG) ya!",
            "file_too_large": "Foto terlalu besar. Coba compress atau pilih foto yang lebih kecil ya!",
            "empty_file": "File foto kosong. Coba upload foto lain ya!",
            "connection_failed": "Tidak bisa terhubung ke layanan deteksi emosi. Coba lagi nanti ya!",
            "timeout": "Proses deteksi emosi membutuhkan waktu terlalu lama. Coba foto yang lebih kecil atau coba lagi.",
            "request_failed": "Gagal memproses permintaan ke layanan deteksi emosi.",
            "fallback_message": "Maaf, {detail} Tapi tidak apa-apa, kamu bisa ceritakan langsung apa yang kamu rasakan malam ini. Aku tetap di sini untuk mendengarkan! 🌙",
            "invalid_data": "Ada masalah dengan data foto yang kamu kirim. Coba foto lain ya, atau ceritakan langsung perasaan kamu ke aku! 😊",
            "memory_limit": "Foto terlalu besar untuk diproses. Coba compress foto atau gunakan foto yang lebih kecil ya!",
            "processing_failed": (
                "Maaf, aku mengalami kendala teknis saat memproses foto kamu. "
                "Tapi jangan khawatir! Kamu tetap bisa ceritakan langsung apa yang kamu rasakan, "
                "dan aku akan tetap mendengarkan dengan baik. TemanTidur selalu siap untuk kamu! 🌟"
            )
        },
        "en": {
            "invalid_file_type": "Sorry, I can only read photos. Please upload an image file (JPG, PNG)!",
            "file_too_large": "Photo is too large. Try compressing it or choose a smaller photo!",
            "empty_file": "Photo file is empty. Please try uploading another photo!",
            "connection_failed": "Unable to connect to emotion detection service. Please try again later!",
            "timeout": "Emotion detection is taking too long. Try a smaller photo or try again.",
            "request_failed": "Failed to process request to emotion detection service.",
            "fallback_message": "Sorry, {detail} But that's okay, you can tell me directly what you're feeling tonight. I'm still here to listen! 🌙",
            "invalid_data": "There's an issue with the photo data you sent. Try another photo, or just tell me directly how you're feeling! 😊",
            "memory_limit": "Photo is too large to process. Try compressing the photo or use a smaller one!",
            "processing_failed": (
                "Sorry, I'm experiencing technical difficulties processing your photo. "
                "But don't worry! You can still tell me directly what you're feeling, "
                "and I'll listen carefully. SleepBuddy is always ready for you! 🌟"
            )
        }
    }
    return messages.get(language, messages["id"])

async def handle_emotion_detection(image: UploadFile, language: str = "id"):
    """
    Handler utama untuk deteksi emosi - fitur visual TemanTidur
    untuk memahami perasaan remaja melalui ekspresi wajah
    """
    try:
        # Validasi bahasa
        if language not in ["id", "en"]:
            language = "id"  
            
        error_msgs = get_error_messages(language)
        
        # Validasi tipe file
        if not image.content_type or not image.content_type.startswith('image/'):
            raise HTTPException(
                status_code=400, 
                detail={
                    "error": "Invalid File Type",
                    "message": error_msgs["invalid_file_type"]
                }
            )
        
        # Validasi ukuran file (max 10MB)
        image_data = await image.read()
        if len(image_data) > 10 * 1024 * 1024:  # 10MB
            raise HTTPException(
                status_code=413,
                detail={
                    "error": "File Too Large", 
                    "message": error_msgs["file_too_large"]
                }
            )
        
        if len(image_data) == 0:
            raise HTTPException(
                status_code=400,
                detail={
                    "error": "Empty File",
                    "message": error_msgs["empty_file"]
                }
            )

        emotion_result = detect_emotion_from_image(image_data)

        if "error" in emotion_result:
            # Return error dengan konteks berdasarkan bahasa
            error_message = emotion_result.get("detail", error_msgs["request_failed"])
            return {
                "error": emotion_result["error"],
                "message": error_msgs["fallback_message"].format(detail=error_message)
            }

        emotion = emotion_result["emotion"]
        confidence = emotion_result["confidence"]
        
        # Log untuk monitoring kesehatan aplikasi
        logger.info(f"Emotion detected: {emotion} with confidence: {confidence}, language: {language}")
        
        # Generate recommendation dengan AI berdasarkan bahasa
        try:
            recommendation = recommend_activity_for_emotion(emotion, language)
        except BadRequestError as e:
            logger.warning(f"Content filter in emotion recommendation: {str(e)}")
            if language == "en":
                recommendation = (
                    f"I can see you're feeling {emotion} tonight. "
                    "Try calming activities like listening to music or breathing techniques. "
                    "I'm here with you. 💙"
                )
            else:
                recommendation = (
                    f"Aku bisa lihat kamu sedang merasa {emotion} malam ini. "
                    "Coba aktivitas menenangkan seperti mendengarkan musik atau teknik pernapasan. "
                    "Aku di sini bersamamu. 💙"
                )
        except Exception as e:
            logger.error(f"Error getting recommendation for emotion {emotion}: {str(e)}")
            if language == "en":
                recommendation = (
                    f"I see you're feeling {emotion}. "
                    "The important thing is that you've been brave to express your feelings. "
                    "Tell me more! 🤗"
                )
            else:
                recommendation = (
                    f"Aku melihat kamu sedang merasa {emotion}. "
                    "Yang penting kamu sudah berani mengekspresikan perasaanmu. "
                    "Ceritakan lebih lanjut ya! 🤗"
                )

        # Generate dynamic recognition message dengan AI berdasarkan bahasa
        try:
            ai_message = generate_emotion_recognition_message(emotion, confidence, language)
        except Exception as e:
            logger.error(f"Error generating emotion recognition message: {str(e)}")
            if language == "en":
                ai_message = f"I can see from your facial expression that you're feeling {emotion} tonight. Thank you for sharing your feelings with me! 🌙✨"
            else:
                ai_message = f"Aku bisa melihat dari ekspresi wajah kamu bahwa kamu sedang merasa {emotion} malam ini. Terima kasih sudah mau berbagi perasaanmu dengan aku! 🌙✨"

        return {
            "emotion": emotion,
            "confidence": round(confidence, 2),
            "recommendation": recommendation,
            "message": ai_message,
            "language": language
        }
    
    except HTTPException:
        # Re-raise HTTPException yang sudah ditangani di atas
        raise
    
    except ValueError as e:
        logger.error(f"Value error in emotion detection: {str(e)}")
        error_msgs = get_error_messages(language)
        raise HTTPException(
            status_code=400, 
            detail={
                "error": "Invalid Data",
                "message": error_msgs["invalid_data"]
            }
        )
    
    except MemoryError as e:
        logger.error(f"Memory error processing image: {str(e)}")
        error_msgs = get_error_messages(language)
        raise HTTPException(
            status_code=413,
            detail={
                "error": "Memory Limit",
                "message": error_msgs["memory_limit"]
            }
        )
    
    except Exception as e:
        logger.error(f"Unexpected error in emotion detection: {str(e)}")
        error_msgs = get_error_messages(language)
        raise HTTPException(
            status_code=500, 
            detail={
                "error": "Processing Failed",
                "message": error_msgs["processing_failed"]
            }
        )