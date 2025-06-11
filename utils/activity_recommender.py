from config.azure_config import client, DEPLOYMENT_NAME
from openai import BadRequestError
import logging

logger = logging.getLogger(__name__)

def recommend_activity_for_emotion(emotion: str, language: str = "id"):
    """
    Memberikan rekomendasi aktivitas untuk emosi tertentu, khusus untuk remaja
    yang membutuhkan teman curhat di malam hari
    Mendukung bahasa Indonesia (id) dan Inggris (en)
    """
    try:
        if language == "en":
            system_content = (
                "Provide three or four practical calming activities for nighttime. "
                "Each activity separated by a period (.). "
                "Each suggestion should be a complete sentence with brief explanation of how to do it. "
                "Don't use numbers or numbering in explanations. "
                "Add relevant emojis to each activity to make it more engaging. "
                "Don't use newline or enter characters in the response. "
                "Example: Listen to instrumental music while lying down to calm your mind 🎵. Write in your journal about today's feelings to release the burden ✍️. Drink warm tea slowly while enjoying the warmth ☕. "
                "Provide concrete activities without mentioning specific duration or counts."
            )
            user_content = f"Tonight I'm feeling {emotion}. What can I do to feel better?"
        else:
            system_content = (
                "Berikan tiga atau empat aktivitas praktis yang menenangkan untuk malam hari. "
                "Setiap aktivitas dipisahkan dengan titik (.). "
                "Setiap saran berupa kalimat lengkap dengan penjelasan singkat cara melakukannya. "
                "Jangan gunakan angka atau nomor dalam penjelasan. "
                "Tambahkan emoji yang relevan di setiap aktivitas untuk membuatnya lebih menarik. "
                "Jangan gunakan karakter newline atau enter dalam respons. "
                "Contoh: Dengarkan musik instrumental sambil rebahan untuk menenangkan pikiran 🎵. Tulis di jurnal tentang perasaan hari ini untuk melepas beban ✍️. Minum teh hangat perlahan sambil menikmati kehangatan ☕. "
                "Berikan aktivitas konkret tanpa menyebutkan durasi atau hitungan spesifik."
            )
            user_content = f"Malam ini aku merasa {emotion}. Apa yang bisa kulakukan untuk merasa lebih baik?"

        messages = [
            {
                "role": "system",
                "content": system_content
            },
            {
                "role": "user",
                "content": user_content
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
        logger.warning(f"Azure content filter triggered for emotion: {emotion}, language: {language} - {str(e)}")
        
        # Respons khusus berdasarkan bahasa saat konten difilter
        if language == "en":
            return "Try breathing techniques by inhaling slowly, holding briefly, then exhaling long to calm your mind 🌬️. Write a gratitude list of things you're thankful for today in your phone notes 📱. Listen to ASMR or white noise for deep relaxation while closing your eyes 🎧. Do light stretching in bed to release muscle tension throughout your body 🧘‍♀️."
        else:
            return "Coba teknik breathing dengan menghirup perlahan, tahan sebentar, lalu hembuskan panjang untuk menenangkan pikiran 🌬️. Tulis gratitude list beberapa hal yang kamu syukuri hari ini di notes HP 📱. Dengarkan ASMR atau white noise untuk relaksasi mendalam sambil memejamkan mata 🎧. Lakukan stretching ringan di kasur untuk melepas ketegangan otot seluruh tubuh 🧘‍♀️."
    
    except ConnectionError as e:
        logger.error(f"Network connection failed in activity recommender: {str(e)}")
        if language == "en":
            return "Write free journaling to express all feelings without censorship ✍️. Create a calm music playlist and listen while lying comfortably 🎶. Do simple yoga movements in bed like child's pose or gentle twist 🧘. Chat with close friends or family you trust to share your story 💬."
        else:
            return "Tulis journaling bebas untuk mengekspresikan semua perasaan tanpa sensor ✍️. Buat playlist musik tenang dan dengarkan sambil rebahan nyaman 🎶. Lakukan gerakan yoga sederhana di kasur seperti child's pose atau gentle twist 🧘. Chat dengan teman terdekat atau keluarga yang bisa kamu percaya untuk berbagi cerita 💬."
    
    except TimeoutError as e:
        logger.error(f"Request timeout in activity recommender: {str(e)}")
        if language == "en":
            return "Practice grounding techniques by naming things you can see, feel, and hear around you 🌟. Drink warm chamomile tea slowly to calm your nervous system 🍵. Read inspirational articles or motivational quotes on your phone to shift negative thoughts 📖. Do box breathing by inhaling, holding, exhaling, and holding in a comfortable rhythm 💨."
        else:
            return "Praktikkan teknik grounding dengan menyebutkan beberapa hal yang bisa kamu lihat, rasakan, dan dengar di sekitar 🌟. Minum teh chamomile hangat perlahan untuk menenangkan sistem saraf 🍵. Baca artikel inspiratif atau quotes motivasi di HP untuk mengalihkan pikiran negatif 📖. Lakukan box breathing dengan menghirup, tahan, hembuskan, dan tahan dalam ritme yang nyaman 💨."
    
    except Exception as e:
        logger.error(f"Unexpected error in activity recommender: {str(e)}")
        if language == "en":
            return "Do short meditation with calming guided meditation apps or YouTube videos 🧘‍♂️. Write a letter to yourself about hopes and positive plans for tomorrow 💌. Try progressive muscle relaxation by gradually tensing and relaxing each part of your body 💆. Prepare for earlier sleep by turning off gadgets some time before bed 😴."
        else:
            return "Lakukan meditasi singkat dengan aplikasi atau video YouTube guided meditation yang menenangkan 🧘‍♂️. Tulis surat untuk diri sendiri tentang harapan dan rencana positif untuk besok 💌. Coba progressive muscle relaxation dengan mengencangkan dan merilekskan setiap bagian tubuh secara bertahap 💆. Siapkan tidur lebih awal dengan matikan gadget beberapa saat sebelum tidur 😴."


def generate_emotion_recognition_message(emotion: str, confidence: float, language: str = "id"):
    """
    Generate pesan AI yang dinamis untuk mengenali emosi user
    Mendukung bahasa Indonesia (id) dan Inggris (en)
    """
    try:
        if language == "en":
            system_content = (
                "You are SleepBuddy, an empathetic and warm AI companion. "
                "The user just detected their emotion through a facial photo. "
                "Give a response that shows you understand their feelings and "
                "appreciate them for sharing. Use warm language, "
                "like a caring close friend. "
                "Add appropriate emojis to make the response warmer. "
                "Don't use newline characters in the response. "
                "IMPORTANT: Answer should be short and natural for mobile chat, maximum 2-3 sentences."
            )
            user_content = f"I just uploaded a photo and it detected that I'm feeling {emotion} with confidence level {confidence:.2f}"
        else:
            system_content = (
                "Kamu adalah TemanTidur, AI companion yang empati dan hangat. "
                "User baru saja mendeteksi emosinya melalui foto wajah. "
                "Berikan respon yang menunjukkan kamu memahami perasaannya dan "
                "apresiasi karena dia mau berbagi. Gunakan bahasa yang hangat, "
                "seperti teman dekat yang peduli. "
                "Tambahkan emoji yang sesuai untuk membuat respons lebih hangat. "
                "Jangan gunakan karakter newline dalam respons. "
                "PENTING: Jawaban harus singkat dan natural untuk mobile chat, maksimal 2-3 kalimat."
            )
            user_content = f"Aku baru upload foto dan terdeteksi sedang merasa {emotion} dengan tingkat kepercayaan {confidence:.2f}"

        messages = [
            {
                "role": "system",
                "content": system_content
            },
            {
                "role": "user",
                "content": user_content
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
        logger.warning(f"Azure content filter triggered for emotion message: {emotion}, language: {language} - {str(e)}")
        if language == "en":
            return f"I can see from your photo that you're feeling {emotion} tonight. Thank you for sharing your feelings with me! 🌙✨"
        else:
            return f"Aku bisa melihat dari foto kamu bahwa kamu sedang merasa {emotion} malam ini. Terima kasih sudah mau berbagi perasaanmu dengan aku! 🌙✨"
    
    except Exception as e:
        logger.error(f"Error generating emotion recognition message: {str(e)}")
        if language == "en":
            return f"I see you're feeling {emotion}. The important thing is that you've been brave to express your feelings. Tell me more! 🤗"
        else:
            return f"Aku melihat kamu sedang merasa {emotion}. Yang penting kamu sudah berani mengekspresikan perasaanmu. Ceritakan lebih lanjut ya! 🤗"