from fastapi import UploadFile, HTTPException
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

async def handle_emotion_detection(image: UploadFile):
    """
    Handler utama untuk deteksi emosi - fitur visual TemanTidur
    untuk memahami perasaan remaja melalui ekspresi wajah
    """
    try:
        # Validasi tipe file
        if not image.content_type or not image.content_type.startswith('image/'):
            raise HTTPException(
                status_code=400, 
                detail={
                    "error": "Invalid File Type",
                    "message": "Maaf, aku hanya bisa membaca foto. Coba upload file gambar (JPG, PNG) ya!"
                }
            )
        
        # Validasi ukuran file (max 10MB)
        image_data = await image.read()
        if len(image_data) > 10 * 1024 * 1024:  # 10MB
            raise HTTPException(
                status_code=413,
                detail={
                    "error": "File Too Large", 
                    "message": "Foto terlalu besar. Coba compress atau pilih foto yang lebih kecil ya!"
                }
            )
        
        if len(image_data) == 0:
            raise HTTPException(
                status_code=400,
                detail={
                    "error": "Empty File",
                    "message": "File foto kosong. Coba upload foto lain ya!"
                }
            )

        emotion_result = detect_emotion_from_image(image_data)

        if "error" in emotion_result:
            # Return error dengan konteks TemanTidur
            error_message = emotion_result.get("detail", "Gagal mendeteksi emosi dari foto")
            return {
                "error": emotion_result["error"],
                "message": f"Maaf, {error_message} Tapi tidak apa-apa, kamu bisa ceritakan langsung apa yang kamu rasakan malam ini. Aku tetap di sini untuk mendengarkan! 🌙"
            }

        emotion = emotion_result["emotion"]
        confidence = emotion_result["confidence"]
        
        # Log untuk monitoring kesehatan aplikasi
        logger.info(f"Emotion detected: {emotion} with confidence: {confidence}")
        
        # Generate recommendation dengan AI
        try:
            recommendation = recommend_activity_for_emotion(emotion)
        except BadRequestError as e:
            logger.warning(f"Content filter in emotion recommendation: {str(e)}")
            recommendation = (
                f"Aku bisa lihat kamu sedang merasa {emotion} malam ini. "
                "Coba aktivitas menenangkan seperti mendengarkan musik atau teknik pernapasan. "
                "Aku di sini bersamamu. 💙"
            )
        except Exception as e:
            logger.error(f"Error getting recommendation for emotion {emotion}: {str(e)}")
            recommendation = (
                f"Aku melihat kamu sedang merasa {emotion}. "
                "Yang penting kamu sudah berani mengekspresikan perasaanmu. "
                "Ceritakan lebih lanjut ya! 🤗"
            )

        # Generate dynamic recognition message dengan AI
        try:
            ai_message = generate_emotion_recognition_message(emotion, confidence)
        except Exception as e:
            logger.error(f"Error generating emotion recognition message: {str(e)}")
            ai_message = f"Aku bisa melihat dari ekspresi wajah kamu bahwa kamu sedang merasa {emotion} malam ini. Terima kasih sudah mau berbagi perasaanmu dengan aku! 🌙✨"

        return {
            "emotion": emotion,
            "confidence": round(confidence, 2),
            "recommendation": recommendation,
            "message": ai_message  
        }
    
    except HTTPException:
        # Re-raise HTTPException yang sudah ditangani di atas
        raise
    
    except ValueError as e:
        logger.error(f"Value error in emotion detection: {str(e)}")
        raise HTTPException(
            status_code=400, 
            detail={
                "error": "Invalid Data",
                "message": "Ada masalah dengan data foto yang kamu kirim. Coba foto lain ya, atau ceritakan langsung perasaan kamu ke aku! 😊"
            }
        )
    
    except MemoryError as e:
        logger.error(f"Memory error processing image: {str(e)}")
        raise HTTPException(
            status_code=413,
            detail={
                "error": "Memory Limit",
                "message": "Foto terlalu besar untuk diproses. Coba compress foto atau gunakan foto yang lebih kecil ya!"
            }
        )
    
    except Exception as e:
        logger.error(f"Unexpected error in emotion detection: {str(e)}")
        raise HTTPException(
            status_code=500, 
            detail={
                "error": "Processing Failed",
                "message": (
                    "Maaf, aku mengalami kendala teknis saat memproses foto kamu. "
                    "Tapi jangan khawatir! Kamu tetap bisa ceritakan langsung apa yang kamu rasakan, "
                    "dan aku akan tetap mendengarkan dengan baik. TemanTidur selalu siap untuk kamu! 🌟"
                )
            }
        )