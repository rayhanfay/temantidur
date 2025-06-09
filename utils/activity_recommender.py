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
                    "Berikan tiga atau empat aktivitas praktis yang menenangkan untuk malam hari. "
                    "Setiap aktivitas dipisahkan dengan titik (.). "
                    "Setiap saran berupa kalimat lengkap dengan penjelasan singkat cara melakukannya. "
                    "Jangan gunakan angka atau nomor dalam penjelasan. "
                    "Tambahkan emoji yang relevan di setiap aktivitas untuk membuatnya lebih menarik. "
                    "Jangan gunakan karakter newline atau enter dalam respons. "
                    "Contoh: Dengarkan musik instrumental sambil rebahan untuk menenangkan pikiran 🎵. Tulis di jurnal tentang perasaan hari ini untuk melepas beban ✍️. Minum teh hangat perlahan sambil menikmati kehangatan ☕. "
                    "Berikan aktivitas konkret tanpa menyebutkan durasi atau hitungan spesifik."
                )
            },
            {
                "role": "user",
                "content": f"Malam ini aku merasa {emotion}. Apa yang bisa kulakukan untuk merasa lebih baik?"
            }
        ]

        response = client.chat.completions.create(
            messages=messages, 
            max_tokens=250, 
            temperature=0.7,
            model=DEPLOYMENT_NAME,
        )

        # Remove any newline characters and clean up the response
        content = response.choices[0].message.content.replace('\n', ' ').replace('  ', ' ').strip()
        return content
    
    except BadRequestError as e:
        logger.warning(f"Azure content filter triggered for emotion: {emotion} - {str(e)}")
        
        # Respons khusus TemanTidur saat konten difilter - langsung aktivitas
        return "Coba teknik breathing dengan menghirup perlahan, tahan sebentar, lalu hembuskan panjang untuk menenangkan pikiran 🌬️. Tulis gratitude list beberapa hal yang kamu syukuri hari ini di notes HP 📱. Dengarkan ASMR atau white noise untuk relaksasi mendalam sambil memejamkan mata 🎧. Lakukan stretching ringan di kasur untuk melepas ketegangan otot seluruh tubuh 🧘‍♀️."
    
    except ConnectionError as e:
        logger.error(f"Network connection failed in activity recommender: {str(e)}")
        return "Tulis journaling bebas untuk mengekspresikan semua perasaan tanpa sensor ✍️. Buat playlist musik tenang dan dengarkan sambil rebahan nyaman 🎶. Lakukan gerakan yoga sederhana di kasur seperti child's pose atau gentle twist 🧘. Chat dengan teman terdekat atau keluarga yang bisa kamu percaya untuk berbagi cerita 💬."
    
    except TimeoutError as e:
        logger.error(f"Request timeout in activity recommender: {str(e)}")
        return "Praktikkan teknik grounding dengan menyebutkan beberapa hal yang bisa kamu lihat, rasakan, dan dengar di sekitar 🌟. Minum teh chamomile hangat perlahan untuk menenangkan sistem saraf 🍵. Baca artikel inspiratif atau quotes motivasi di HP untuk mengalihkan pikiran negatif 📖. Lakukan box breathing dengan menghirup, tahan, hembuskan, dan tahan dalam ritme yang nyaman 💨."
    
    except Exception as e:
        logger.error(f"Unexpected error in activity recommender: {str(e)}")
        return "Lakukan meditasi singkat dengan aplikasi atau video YouTube guided meditation yang menenangkan 🧘‍♂️. Tulis surat untuk diri sendiri tentang harapan dan rencana positif untuk besok 💌. Coba progressive muscle relaxation dengan mengencangkan dan merilekskan setiap bagian tubuh secara bertahap 💆. Siapkan tidur lebih awal dengan matikan gadget beberapa saat sebelum tidur 😴."


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
                    "Tambahkan emoji yang sesuai untuk membuat respons lebih hangat. "
                    "Jangan gunakan karakter newline dalam respons. "
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

        # Remove any newline characters and clean up the response
        content = response.choices[0].message.content.replace('\n', ' ').replace('  ', ' ').strip()
        return content
    
    except BadRequestError as e:
        logger.warning(f"Azure content filter triggered for emotion message: {emotion} - {str(e)}")
        return f"Aku bisa melihat dari foto kamu bahwa kamu sedang merasa {emotion} malam ini. Terima kasih sudah mau berbagi perasaanmu dengan aku! 🌙✨"
    
    except Exception as e:
        logger.error(f"Error generating emotion recognition message: {str(e)}")
        return f"Aku melihat kamu sedang merasa {emotion}. Yang penting kamu sudah berani mengekspresikan perasaanmu. Ceritakan lebih lanjut ya! 🤗"