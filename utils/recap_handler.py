from fastapi import Request, HTTPException
from config.azure_config import client, DEPLOYMENT_NAME
from openai import BadRequestError, RateLimitError, APIConnectionError
from datetime import datetime
import logging
import json
import re

logger = logging.getLogger(__name__)

def clean_text(text):
    """
    Clean text by removing escape sequences and normalizing whitespace
    """
    if not text:
        return text
    
    # Remove escape sequences like \n\, \\n, etc.
    text = re.sub(r'\\n\\?|\\\\n', ' ', text)
    # Remove other common escape sequences
    text = re.sub(r'\\[rtn]', ' ', text)
    # Normalize multiple spaces to single space
    text = re.sub(r'\s+', ' ', text)
    # Strip leading/trailing whitespace
    text = text.strip()
    
    return text

def ensure_complete_sentences(text, max_sentences=6):
    """
    Ensure text ends with complete sentences and limit to max_sentences
    """
    if not text:
        return text
    
    # Clean the text first
    text = clean_text(text)
    
    # Split into sentences (looking for sentence endings)
    sentences = re.split(r'[.!?]+(?=\s|$)', text)
    
    # Filter out empty sentences and clean each one
    clean_sentences = []
    for sentence in sentences:
        cleaned = sentence.strip()
        if cleaned:
            clean_sentences.append(cleaned)
    
    # Limit to max_sentences
    if len(clean_sentences) > max_sentences:
        clean_sentences = clean_sentences[:max_sentences]
    
    # Rejoin sentences with proper punctuation
    result = ""
    for i, sentence in enumerate(clean_sentences):
        sentence = sentence.strip()
        if sentence:
            # Add period if sentence doesn't end with punctuation
            if not sentence.endswith(('.', '!', '?')):
                sentence += '.'
            
            result += sentence
            # Add space between sentences (except for the last one)
            if i < len(clean_sentences) - 1:
                result += ' '
    
    return result

def detect_language(text):
    """
    Simple language detection based on common words and patterns
    Returns 'id' for Indonesian, 'en' for English
    """
    if not text:
        return 'id'  
    
    # Clean text first
    text = clean_text(text)
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

def get_recap_system_prompt(language='id', date=None):
    """Get system prompt for recap generation based on language"""
    if language == 'en':
        return {
            "role": "system",
            "content": (
                f"You are SleepBuddy 😴, creating a thoughtful recap of a conversation from {date or 'today'}. "
                "Your recap should be empathetic, warm, and supportive - matching the tone of a caring friend 💙. "
                "Focus on:\n"
                "1. Main emotions and feelings expressed by the user 💭\n"
                "2. Key topics or concerns discussed 🗣️\n"
                "3. Support and advice given 🤗\n"
                "4. Overall mood and progress of the conversation 📈\n"
                "5. Any positive moments or breakthroughs ✨\n\n"
                "IMPORTANT: Keep your recap to exactly 5-6 complete sentences. "
                "Write in a caring, understanding tone that shows you remember and value the conversation 💕. "
                "Use relevant emojis naturally throughout your recap to make it more engaging and warm 🌟. "
                "Each sentence should be complete and meaningful. "
                "End with an encouraging sentence about the user's strength or progress 🌈."
            )
        }
    else:
        return {
            "role": "system",
            "content": (
                f"Kamu adalah TemanTidur 😴, membuat recap yang thoughtful dari percakapan tanggal {date or 'hari ini'}. "
                "Recap kamu harus empati, hangat, dan supportif - sesuai dengan tone teman yang peduli 💙. "
                "Fokus pada:\n"
                "1. Emosi dan perasaan utama yang diungkapkan user 💭\n"
                "2. Topik atau kekhawatiran utama yang dibahas 🗣️\n"
                "3. Dukungan dan saran yang diberikan 🤗\n"
                "4. Mood dan progress keseluruhan percakapan 📈\n"
                "5. Momen positif atau breakthrough yang terjadi ✨\n\n"
                "PENTING: Buat recap kamu tepat 5-6 kalimat lengkap. "
                "Tulis dengan tone yang caring dan understanding yang menunjukkan kamu ingat dan menghargai percakapan tersebut 💕. "
                "Gunakan emoji yang relevan secara natural di seluruh recap untuk membuatnya lebih engaging dan hangat 🌟. "
                "Setiap kalimat harus lengkap dan bermakna. "
                "Akhiri dengan kalimat yang encouraging tentang kekuatan atau progress user 🌈."
            )
        }

def format_date_for_recap(date_str):
    """Format date string for better readability in recap"""
    try:
        # Parse date from DD.MM.YYYY format
        date_obj = datetime.strptime(date_str, "%d.%m.%Y")
        
        # Format for Indonesian
        months_id = [
            "Januari", "Februari", "Maret", "April", "Mei", "Juni",
            "Juli", "Agustus", "September", "Oktober", "November", "Desember"
        ]
        formatted_id = f"{date_obj.day} {months_id[date_obj.month-1]} {date_obj.year}"
        
        # Format for English
        formatted_en = date_obj.strftime("%B %d, %Y")
        
        return formatted_id, formatted_en
    except ValueError:
        return date_str, date_str

def get_recap_error_messages(language='id'):
    """Get error messages for recap generation"""
    if language == 'en':
        return {
            'no_messages': (
                "I don't see any conversation messages to recap 🤔. "
                "Share your chat history with me and I'll create a meaningful summary for you! 💙✨"
            ),
            'invalid_date': (
                "The date format seems incorrect 📅. Please use DD.MM.YYYY format (like 11.06.2025). "
                "But don't worry, I can still create a recap of your conversation! ✨💫"
            ),
            'bad_request': (
                "There seems to be an issue with your request format 🔧. "
                "Make sure to include your conversation messages and date. "
                "I'm here to help create a meaningful recap! 🤗💕"
            ),
            'rate_limit': (
                "Many friends are requesting recaps right now! 🚀 "
                "Please wait a moment and try again ⏰. "
                "Your conversation is important and worth summarizing! 💙🌟"
            ),
            'connection_error': (
                "Connection issue while creating your recap 🌐. "
                "Your conversations matter, so please try again in a moment ⏳. "
                "I want to give you a thoughtful summary! 🌙💭"
            ),
            'unexpected_error': (
                "Something unexpected happened while creating your recap 🙈. "
                "But your conversations are valuable - please try again! 💪 "
                "I'm here to help you reflect on your journey 🌟✨."
            )
        }
    else:
        return {
            'no_messages': (
                "Aku tidak melihat pesan percakapan untuk dirangkum 🤔. "
                "Share riwayat chat kamu dan aku akan buatkan rangkuman yang bermakna! 💙✨"
            ),
            'invalid_date': (
                "Format tanggal sepertinya tidak tepat 📅. Gunakan format DD.MM.YYYY (seperti 11.06.2025). "
                "Tapi tidak apa-apa, aku tetap bisa buatkan recap percakapan kamu! ✨💫"
            ),
            'bad_request': (
                "Sepertinya ada masalah dengan format permintaan kamu 🔧. "
                "Pastikan untuk menyertakan pesan percakapan dan tanggal. "
                "Aku di sini untuk membantu membuat recap yang bermakna! 🤗💕"
            ),
            'rate_limit': (
                "Banyak teman yang minta recap sekarang! 🚀 "
                "Tunggu sebentar dan coba lagi ya ⏰. "
                "Percakapan kamu penting dan layak untuk dirangkum! 💙🌟"
            ),
            'connection_error': (
                "Ada masalah koneksi saat membuat recap kamu 🌐. "
                "Percakapan kamu berharga, jadi coba lagi sebentar lagi ya ⏳. "
                "Aku ingin kasih rangkuman yang thoughtful! 🌙💭"
            ),
            'unexpected_error': (
                "Ada hal yang tidak terduga saat membuat recap kamu 🙈. "
                "Tapi percakapan kamu valuable - coba lagi ya! 💪 "
                "Aku di sini untuk membantu kamu merefleksikan perjalanan kamu 🌟✨."
            )
        }

async def handle_recap_request(request: Request):
    """
    Handler for generating conversation recap
    Request body format:
    {
        "date": "11.06.2025",
        "messages": [
            {"role": "user", "content": "..."},
            {"role": "assistant", "content": "..."}
        ]
    }
    """
    try:
        body = await request.json()
        messages = body.get("messages", [])
        date = body.get("date", "")
        
        # Validate messages
        if not messages:
            error_messages = get_recap_error_messages('id') 
            return {"error": error_messages['no_messages']}
        
        # Filter only user and assistant messages and clean content
        conversation_messages = []
        for msg in messages:
            if msg.get("role") in ["user", "assistant"]:
                content = msg.get("content", "").strip()
                if content:
                    # Clean the content
                    cleaned_content = clean_text(content)
                    if cleaned_content:
                        conversation_messages.append({
                            "role": msg["role"],
                            "content": cleaned_content
                        })
        
        if not conversation_messages:
            error_messages = get_recap_error_messages('id')
            return {"error": error_messages['no_messages']}
        
        # Detect language from user messages
        user_messages = [msg["content"] for msg in conversation_messages if msg["role"] == "user"]
        language = 'id'
        if user_messages:
            combined_text = " ".join(user_messages)
            language = detect_language(combined_text)
        
        # Format date
        formatted_date_id, formatted_date_en = format_date_for_recap(date)
        display_date = formatted_date_en if language == 'en' else formatted_date_id
        
        # Create conversation text for recap
        conversation_text = ""
        for msg in conversation_messages:
            role = "User" if msg["role"] == "user" else ("SleepBuddy" if language == 'en' else "TemanTidur")
            conversation_text += f"{role}: {msg['content']}\n\n"
        
        # Get system prompt for recap
        system_prompt = get_recap_system_prompt(language, display_date)
        
        # Create recap prompt
        recap_prompt = {
            "role": "user",
            "content": (
                f"Please create a thoughtful recap of this conversation from {display_date} 📝:\n\n"
                f"{conversation_text}"
                f"\nCreate a caring, empathetic recap in exactly 5-6 complete sentences 💕. "
                f"Use relevant emojis naturally throughout to make it warm and engaging 🌟. "
                f"Each sentence should be meaningful and end properly with punctuation."
            ) if language == 'en' else (
                f"Tolong buatkan recap yang thoughtful dari percakapan tanggal {display_date} 📝:\n\n"
                f"{conversation_text}"
                f"\nBuat recap yang caring dan empathetic dalam tepat 5-6 kalimat lengkap 💕. "
                f"Gunakan emoji yang relevan secara natural untuk membuatnya hangat dan engaging 🌟. "
                f"Setiap kalimat harus bermakna dan berakhir dengan tanda baca yang tepat."
            )
        }
        
        # Generate recap using Azure OpenAI
        response = client.chat.completions.create(
            messages=[system_prompt, recap_prompt],
            max_tokens=200,  # Reduced tokens for shorter recap
            temperature=0.7,  
            top_p=0.9,
            model=DEPLOYMENT_NAME,
        )
        
        recap_content = response.choices[0].message.content
        
        # Ensure the recap is properly formatted and limited
        final_recap = ensure_complete_sentences(recap_content, max_sentences=6)
        
        return {
            "recap": final_recap,
            "date": date,
            "formatted_date": display_date,
            "language": language,
            "message_count": len(conversation_messages)
        }
    
    except json.JSONDecodeError as e:
        logger.error(f"Invalid JSON in recap request: {str(e)}")
        error_messages = get_recap_error_messages('id')
        return {"error": error_messages['bad_request']}
    
    except BadRequestError as e:
        error_details = str(e)
        logger.warning(f"Azure content filtered in recap: {error_details}")
        
        # Detect language for error response
        language = 'id'
        try:
            body = await request.json()
            messages = body.get("messages", [])
            user_messages = [msg["content"] for msg in messages if msg.get("role") == "user"]
            if user_messages:
                combined_text = " ".join(user_messages)
                language = detect_language(combined_text)
        except:
            pass
        
        error_messages = get_recap_error_messages(language)
        return {"error": error_messages['bad_request']}
    
    except RateLimitError as e:
        logger.error(f"Rate limit exceeded in recap: {str(e)}")
        error_messages = get_recap_error_messages('id')
        return {"error": error_messages['rate_limit']}
    
    except APIConnectionError as e:
        logger.error(f"API connection failed in recap: {str(e)}")
        error_messages = get_recap_error_messages('id')
        return {"error": error_messages['connection_error']}
    
    except Exception as e:
        logger.error(f"Unexpected error in recap: {str(e)}")
        error_messages = get_recap_error_messages('id')
        return {"error": error_messages['unexpected_error']}