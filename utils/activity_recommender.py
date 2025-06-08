from config.azure_config import client, DEPLOYMENT_NAME
from openai import BadRequestError
import logging

logger = logging.getLogger(__name__)

def recommend_activity_for_emotion(emotion: str):
    """
    Memberikan rekomendasi aktivitas untuk emosi tertentu, khusus untuk remaja
    yang membutuhkan teman curhat di malam hari
    """
    try:
        messages = [
            {
                "role": "system",
                "content": (
                    "Kamu adalah TemanTidur, AI companion yang memahami perasaan remaja di malam hari. "
                    "Berikan rekomendasi aktivitas yang menenangkan, aman, dan cocok dilakukan malam hari "
                    "untuk membantu mengatasi overthinking dan kesepian. Gunakan bahasa yang hangat, "
                    "supportif, dan seperti teman sebaya yang peduli. "
                    "PENTING: Jawaban harus singkat, maksimal 4-5 kalimat saja untuk mobile interface."
                )
            },
            {
                "role": "user",
                "content": f"Malam ini aku merasa {emotion}. Apa yang bisa kulakukan untuk merasa lebih baik?"
            }
        ]

        response = client.chat.completions.create(
            messages=messages, 
            max_tokens=250,  # Reduced for shorter responses
            temperature=0.7,
            model=DEPLOYMENT_NAME,
        )

        return response.choices[0].message.content
    
    except BadRequestError as e:
        logger.warning(f"Azure content filter triggered for emotion: {emotion} - {str(e)}")
        
        # Respons khusus TemanTidur saat konten difilter - singkat untuk mobile
        return (
            "Aku tahu kamu sedang merasakan emosi yang berat malam ini. "
            "Bagaimana kalau kita coba teknik grounding: duduk nyaman, rasakan napasmu. "
            "Aku di sini bersamamu. 🌙💙"
        )
    
    except ConnectionError as e:
        logger.error(f"Network connection failed in activity recommender: {str(e)}")
        return (
            "Koneksi internet sedang tidak stabil malam ini. "
            "Coba journaling - tulis apa yang kamu rasakan, atau dengarkan playlist menenangkan. "
            "Kamu pasti bisa melewati malam ini! ✨"
        )
    
    except TimeoutError as e:
        logger.error(f"Request timeout in activity recommender: {str(e)}")
        return (
            "Permintaan membutuhkan waktu lebih lama dari biasanya. "
            "Coba teknik 5-4-3-2-1: sebutkan 5 hal yang kamu lihat, 4 yang bisa sentuh. "
            "Aku akan segera kembali! 🤗"
        )
    
    except Exception as e:
        logger.error(f"Unexpected error in activity recommender: {str(e)}")
        return (
            "Sepertinya aku mengalami kendala teknis malam ini. "
            "Tapi ingat: kamu lebih kuat dari yang kamu kira, besok adalah hari baru. "
            "Tidur yang nyenyak ya! 🌟💤"
        )


def generate_emotion_recognition_message(emotion: str, confidence: float):
    """
    Generate pesan AI yang dinamis untuk mengenali emosi user
    """
    try:
        messages = [
            {
                "role": "system",
                "content": (
                    "Kamu adalah TemanTidur, AI companion yang empati dan hangat. "
                    "User baru saja mendeteksi emosinya melalui foto wajah. "
                    "Berikan respon yang menunjukkan kamu memahami perasaannya dan "
                    "apresiasi karena dia mau berbagi. Gunakan bahasa yang hangat, "
                    "seperti teman dekat yang peduli. "
                    "PENTING: Jawaban harus singkat dan natural untuk mobile chat, maksimal 2-3 kalimat."
                )
            },
            {
                "role": "user",
                "content": f"Aku baru upload foto dan terdeteksi sedang merasa {emotion} dengan tingkat kepercayaan {confidence:.2f}"
            }
        ]

        response = client.chat.completions.create(
            messages=messages, 
            max_tokens=150,
            temperature=0.8,  # Slightly higher for more variety
            model=DEPLOYMENT_NAME,
        )

        return response.choices[0].message.content
    
    except BadRequestError as e:
        logger.warning(f"Azure content filter triggered for emotion message: {emotion} - {str(e)}")
        return f"Aku bisa melihat dari foto kamu bahwa kamu sedang merasa {emotion} malam ini. Terima kasih sudah mau berbagi perasaanmu dengan aku! 🌙✨"
    
    except Exception as e:
        logger.error(f"Error generating emotion recognition message: {str(e)}")
        return f"Aku melihat kamu sedang merasa {emotion}. Yang penting kamu sudah berani mengekspresikan perasaanmu. Ceritakan lebih lanjut ya! 🤗"