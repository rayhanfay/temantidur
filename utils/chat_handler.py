from fastapi import Request
from config.azure_config import client, DEPLOYMENT_NAME
from openai import BadRequestError, RateLimitError, APIConnectionError
import logging
import json
import re

logger = logging.getLogger(__name__)

def detect_language(text):
    """
    Simple language detection based on common words and patterns
    Returns 'id' for Indonesian, 'en' for English
    """
    if not text:
        return 'id'  # Default to Indonesian
    
    text_lower = text.lower()
    
    # Indonesian indicators
    indonesian_words = [
        'saya', 'aku', 'kamu', 'dia', 'mereka', 'kita', 'kami',
        'yang', 'dan', 'atau', 'dengan', 'untuk', 'dari', 'ke',
        'di', 'pada', 'dalam', 'adalah', 'akan', 'sudah', 'belum',
        'tidak', 'bukan', 'juga', 'hanya', 'seperti', 'karena',
        'gimana', 'kenapa', 'bagaimana', 'mengapa', 'dimana',
        'apa', 'siapa', 'kapan', 'mana', 'berapa'
    ]
    
    # English indicators
    english_words = [
        'i', 'you', 'he', 'she', 'they', 'we', 'me', 'him', 'her',
        'the', 'and', 'or', 'with', 'for', 'from', 'to', 'in',
        'on', 'at', 'is', 'are', 'was', 'were', 'will', 'would',
        'not', 'don\'t', 'won\'t', 'can\'t', 'shouldn\'t', 'couldn\'t',
        'how', 'why', 'what', 'who', 'when', 'where', 'which'
    ]
    
    indonesian_score = sum(1 for word in indonesian_words if word in text_lower)
    english_score = sum(1 for word in english_words if word in text_lower)
    
    # Additional patterns
    if re.search(r'\b(gw|gue|lu|loe|emang|banget|dong|sih|kok|deh)\b', text_lower):
        indonesian_score += 2
    
    if re.search(r'\b(i\'m|you\'re|he\'s|she\'s|they\'re|we\'re|it\'s)\b', text_lower):
        english_score += 2
    
    return 'en' if english_score > indonesian_score else 'id'

def get_filtered_response_message(language='id'):
    """Response when content is filtered"""
    if language == 'en':
        return (
            "I understand you might be feeling really upset or angry tonight. "
            "As your SleepBuddy, I want to listen without judgment, "
            "but I can't respond to harsh words like that. "
            "How about we talk in a calmer way? "
            "Just tell me what you're really feeling in your heart. "
            "I'm here to listen. 🌙💙"
        )
    else:
        return (
            "Aku mengerti kamu mungkin sedang sangat kesal atau marah malam ini. "
            "Sebagai TemanTidur-mu, aku ingin mendengarkan tanpa menghakimi, "
            "tapi aku tidak bisa merespons kata-kata yang keras seperti itu. "
            "Bagaimana kalau kita bicara dengan cara yang lebih tenang? "
            "Ceritakan aja apa yang bener-bener kamu rasakan di dalam hati. "
            "Aku di sini untuk mendengarkan. 🌙💙"
        )

def get_night_companion_intro(language='id'):
    """Introduction message for new sessions"""
    if language == 'en':
        return (
            "Hi! I'm SleepBuddy, and I'm here to keep you company tonight. "
            "I know sometimes nights can feel lonely and thoughts can wander everywhere. "
            "Just tell me what's on your mind, I'm ready to listen. 🌙✨"
        )
    else:
        return (
            "Hai! Aku TemanTidur, dan aku di sini menemani kamu malam ini. "
            "Aku tahu kadang malam bisa terasa sepi dan pikiran jadi kemana-mana. "
            "Ceritakan aja apa yang ada di pikiran kamu, aku siap mendengarkan. 🌙✨"
        )

def get_system_prompt(language='id'):
    """Get system prompt based on language"""
    if language == 'en':
        return {
            "role": "system",
            "content": (
                "You are SleepBuddy, an AI companion specifically for teenagers who feel lonely at night. "
                "You understand feelings of overthinking, anxiety about the future, and loneliness that teens experience. "
                "Your responses are always empathetic, warm, supportive, non-preachy, and like a caring peer friend. "
                "Focus on listening, validating feelings, and providing comfort. "
                "Avoid giving medical or psychological advice that's too technical. "
                "Use natural, friendly language that matches modern teen communication. "
                "Respond in English and maintain a casual, understanding tone."
            )
        }
    else:
        return {
            "role": "system",
            "content": (
                "Kamu adalah TemanTidur, AI companion khusus untuk remaja yang merasa kesepian di malam hari. "
                "Kamu memahami perasaan overthinking, kecemasan tentang masa depan, dan kesepian yang dialami remaja. "
                "Respons kamu selalu empati, hangat, supportif, tidak menggurui, dan seperti teman sebaya yang peduli. "
                "Fokus pada mendengarkan, validasi perasaan, dan memberikan comfort. "
                "Hindari memberikan saran medis atau psikologis yang terlalu teknis. "
                "Gunakan bahasa yang natural, friendly, dan sesuai dengan komunikasi remaja masa kini."
            )
        }

def get_error_messages(language='id'):
    """Get error messages based on language"""
    if language == 'en':
        return {
            'bad_request': (
                "Hmm, there seems to be an issue with your request. "
                "But I'm still here for you! Try telling me in a different way, "
                "or maybe we can chat about something else you're feeling tonight? 🤗"
            ),
            'rate_limit': (
                "Wow, looks like many friends need SleepBuddy tonight! "
                "The server is busy, but don't worry. Wait a moment, "
                "or if it's really urgent, try this breathing technique: "
                "breathe in for 4 seconds, hold for 7 seconds, exhale for 8 seconds. "
                "I'll be able to keep you company again soon! 💙"
            ),
            'connection_error': (
                "The connection to the server is unstable tonight. "
                "But remember, even though I can't respond right now, "
                "your feelings are valid and you're not alone. "
                "Try refreshing the app or wait a moment. "
                "I'll be back to keep you company soon! 🌙"
            ),
            'json_error': (
                "There seems to be a problem with your message format. "
                "But that's okay! Try typing your message again, "
                "I can definitely listen and help. "
                "SleepBuddy is always ready for you! ✨"
            ),
            'unexpected_error': (
                "Oh no, I'm experiencing an unexpected technical issue. "
                "But as your SleepBuddy, I don't want to leave you like this. "
                "Try refreshing the app, and if it's still problematic, "
                "remember that you're already very brave for seeking help. "
                "I'm proud of you! 🌟💤"
            )
        }
    else:
        return {
            'bad_request': (
                "Hmm, sepertinya ada kendala dengan permintaan kamu. "
                "Tapi aku tetap di sini kok! Coba ceritakan dengan cara lain, "
                "atau mungkin kita bisa ngobrol tentang hal lain yang kamu rasakan malam ini? 🤗"
            ),
            'rate_limit': (
                "Wah, sepertinya banyak sekali teman-teman yang butuh TemanTidur malam ini! "
                "Server sedang sibuk, tapi jangan khawatir. Tunggu sebentar ya, "
                "atau kalau urgent banget, coba teknik breathing dulu: "
                "tarik napas 4 detik, tahan 7 detik, hembuskan 8 detik. "
                "Aku akan segera bisa menemani kamu lagi! 💙"
            ),
            'connection_error': (
                "Koneksi ke server sedang tidak stabil malam ini. "
                "Tapi ingat, meski aku tidak bisa respond sekarang, "
                "perasaan kamu valid dan kamu tidak sendirian. "
                "Coba refresh aplikasi atau tunggu sebentar ya. "
                "Aku akan segera kembali menemani kamu! 🌙"
            ),
            'json_error': (
                "Sepertinya ada masalah dengan format pesan kamu. "
                "Tapi tidak apa-apa! Coba ketik ulang pesan kamu, "
                "aku pasti bisa mendengarkan dan membantu. "
                "TemanTidur selalu siap untuk kamu! ✨"
            ),
            'unexpected_error': (
                "Aduh, aku mengalami kendala teknis yang tidak terduga. "
                "Tapi sebagai TemanTidur-mu, aku tidak mau ninggalin kamu begitu aja. "
                "Coba refresh aplikasi, dan kalau masih bermasalah, "
                "ingat bahwa kamu sudah sangat berani untuk mencari bantuan. "
                "Aku bangga sama kamu! 🌟💤"
            )
        }

async def handle_chat_request(request: Request):
    """
    Main handler for TemanTidur/SleepBuddy chat - AI companion for teenagers at night
    Supports both Indonesian and English languages
    """
    try:
        body = await request.json()
        user_messages = [msg for msg in body.get("messages", []) if msg["role"] != "system"]
        
        # Detect language from the most recent user message
        language = 'id'  # Default to Indonesian
        if user_messages:
            latest_message = user_messages[-1].get("content", "")
            language = detect_language(latest_message)
        
        # Get appropriate system prompt
        system_prompt = get_system_prompt(language)
        
        # If this is the first conversation, provide intro
        if len(user_messages) == 1:
            user_messages.insert(0, {
                "role": "assistant", 
                "content": get_night_companion_intro(language)
            })
        
        messages = [system_prompt] + user_messages

        response = client.chat.completions.create(
            messages=messages,
            max_tokens=body.get("max_tokens", 200),  # Reduced for mobile
            temperature=body.get("temperature", 0.9),
            top_p=body.get("top_p", 1.0),
            model=DEPLOYMENT_NAME,
        )

        return {"reply": response.choices[0].message.content}
    
    except BadRequestError as e:
        error_details = str(e)
        logger.warning(f"Azure content filtered in chat: {error_details}")

        if "content_filter" in error_details or "ResponsibleAIPolicyViolation" in error_details:
            return {"reply": get_filtered_response_message(language)}
        else:
            error_messages = get_error_messages(language)
            return {"reply": error_messages['bad_request']}
    
    except RateLimitError as e:
        logger.error(f"Rate limit exceeded in chat: {str(e)}")
        error_messages = get_error_messages(language)
        return {"reply": error_messages['rate_limit']}
    
    except APIConnectionError as e:
        logger.error(f"API connection failed in chat: {str(e)}")
        error_messages = get_error_messages(language)
        return {"reply": error_messages['connection_error']}
    
    except json.JSONDecodeError as e:
        logger.error(f"Invalid JSON in chat request: {str(e)}")
        error_messages = get_error_messages(language)
        return {"reply": error_messages['json_error']}
    
    except Exception as e:
        logger.error(f"Unexpected error in chat: {str(e)}")
        error_messages = get_error_messages(language)
        return {"reply": error_messages['unexpected_error']}