import azure.cognitiveservices.speech as speechsdk
import os
from tempfile import NamedTemporaryFile
from config.azure_config import client, DEPLOYMENT_NAME, SPEECH_KEY, SPEECH_REGION
from dotenv import load_dotenv
from fastapi import HTTPException
import logging
import re

load_dotenv()

logger = logging.getLogger(__name__)

SUPPORTED_LANGUAGES = {
    "id": "id-ID",
    "en": "en-US"
}


def validate_audio_format(audio_file):
    """Validate that the uploaded file is in WAV format"""
    allowed_formats = ["audio/wav", "audio/x-wav"]
    allowed_extensions = [".wav"]
    
    content_type = getattr(audio_file, 'content_type', '')
    filename = getattr(audio_file, 'filename', '')
    
    # Check content type
    if content_type not in allowed_formats:
        # Check file extension as fallback
        if not any(filename.lower().endswith(ext) for ext in allowed_extensions):
            raise HTTPException(
                status_code=400, 
                detail="Format audio tidak didukung. Hanya WAV yang diperbolehkan"
            )
    
    logger.info(f"Audio validation passed: {filename} ({content_type})")


def detect_language(text: str) -> str:
    if re.search(r'\b(i|you|the|what|is|are|hello|hey|hi)\b', text.lower()):
        return "en"
    return "id"


def transcribe_audio(file_path: str, language: str = "id") -> str:
    """Transcribe WAV audio file"""
    try:
        # Verify file exists and is not empty
        if not os.path.exists(file_path):
            raise Exception(f"Audio file not found: {file_path}")
        
        file_size = os.path.getsize(file_path)
        if file_size == 0:
            raise Exception("Audio file is empty")
        
        logger.info(f"Transcribing WAV file: {file_path} (size: {file_size} bytes)")
        
        speech_config = speechsdk.SpeechConfig(subscription=SPEECH_KEY, region=SPEECH_REGION)
        speech_config.speech_recognition_language = SUPPORTED_LANGUAGES.get(language, "id-ID")
        
        # Set additional properties for better recognition
        speech_config.set_property(speechsdk.PropertyId.SpeechServiceConnection_InitialSilenceTimeoutMs, "10000")
        speech_config.set_property(speechsdk.PropertyId.SpeechServiceConnection_EndSilenceTimeoutMs, "3000")
        
        audio_input = speechsdk.AudioConfig(filename=file_path)
        recognizer = speechsdk.SpeechRecognizer(speech_config=speech_config, audio_config=audio_input)

        logger.info("Starting speech recognition...")
        result = recognizer.recognize_once()
        
        if result.reason == speechsdk.ResultReason.RecognizedSpeech:
            logger.info(f"Recognition successful: {result.text}")
            return result.text
        elif result.reason == speechsdk.ResultReason.NoMatch:
            logger.warning("No speech could be recognized")
            return "Maaf, aku tidak bisa mendengar dengan jelas. Bisa ulangi lagi?"
        elif result.reason == speechsdk.ResultReason.Canceled:
            cancellation_details = result.cancellation_details
            logger.error(f"Speech recognition canceled: {cancellation_details.reason}")
            if cancellation_details.error_details:
                logger.error(f"Error details: {cancellation_details.error_details}")
            raise Exception(f"Speech recognition canceled: {cancellation_details.reason}")
        else:
            logger.error(f"Speech recognition failed: {result.reason}")
            raise Exception("Gagal mengenali suara: " + str(result.reason))
            
    except Exception as e:
        logger.error(f"Error in transcription: {str(e)}")
        raise Exception(f"Gagal mengenali suara: {str(e)}")


def synthesize_speech(text: str, voice: str = "id-ID-GadisNeural") -> bytes:
    """Convert text to speech using Azure Speech Service - output WAV format"""
    try:
        speech_config = speechsdk.SpeechConfig(subscription=SPEECH_KEY, region=SPEECH_REGION)
        speech_config.speech_synthesis_voice_name = voice
        speech_config.set_speech_synthesis_output_format(speechsdk.SpeechSynthesisOutputFormat.Riff16Khz16BitMonoPcm)
        
        synthesizer = speechsdk.SpeechSynthesizer(speech_config=speech_config, audio_config=None)

        result = synthesizer.speak_text_async(text).get()
        if result.reason == speechsdk.ResultReason.SynthesizingAudioCompleted:
            logger.info("Speech synthesis successful, output format: WAV")
            return result.audio_data
        else:
            logger.error(f"Speech synthesis failed: {result.reason}")
            raise Exception("Gagal mensintesis suara: " + str(result.reason))
    except Exception as e:
        logger.error(f"Error in speech synthesis: {str(e)}")
        raise Exception(f"Gagal mensintesis suara: {str(e)}")


def get_ai_response_for_voice(user_text: str, lang: str = "id", is_first_message: bool = False) -> str:
    """Get AI response for voice chat"""
    try:
        if lang == "id":
            system_prompt = {
                "role": "system",
                "content": (
                    "Kamu adalah TemanTidur, AI companion khusus untuk remaja yang merasa kesepian di malam hari. "
                    "Kamu memahami perasaan overthinking, kecemasan tentang masa depan, dan kesepian yang dialami remaja. "
                    "Respons kamu selalu empati, hangat, supportif, tidak menggurui, dan seperti teman sebaya yang peduli. "
                    "Gunakan bahasa yang natural, friendly, dan sesuai dengan komunikasi remaja masa kini. "
                    "Hindari emoji dan karakter khusus karena ini voice chat."
                )
            }
            intro_text = (
                "Hai! Aku TemanTidur, dan aku di sini menemani kamu malam ini. "
                "Ceritakan aja apa yang ada di pikiran kamu, aku siap mendengarkan."
            )
            voice = "id-ID-GadisNeural"
        else:
            system_prompt = {
                "role": "system",
                "content": (
                    "You are a late-night AI friend for teens who feel lonely or anxious. "
                    "Your responses are always empathetic, supportive, and friendly like a peer. "
                    "Avoid being robotic or too formal. This is for voice chat, so avoid emojis or symbols."
                )
            }
            intro_text = (
                "Hey! I'm here to keep you company tonight. "
                "If your thoughts are all over the place, just talk to me. I'm listening."
            )
            voice = "en-US-JennyNeural"

        messages = [system_prompt]

        if is_first_message:
            messages.append({"role": "assistant", "content": intro_text})

        messages.append({"role": "user", "content": user_text})

        response = client.chat.completions.create(
            messages=messages,
            max_tokens=150,
            temperature=0.9,
            top_p=1.0,
            model=DEPLOYMENT_NAME,
        )

        ai_text = response.choices[0].message.content
        return clean_text_for_tts(ai_text)
    except Exception as e:
        logger.error(f"Error getting AI response: {str(e)}")
        return (
            "Maaf, aku sedang mengalami kendala teknis. "
            "Tapi aku tetap di sini untuk menemani kamu. "
            "Coba bicara lagi ya, aku pasti bisa mendengarkan."
        )


def clean_text_for_tts(text: str) -> str:
    emoji_pattern = re.compile("[" 
        u"\U0001F600-\U0001F64F" 
        u"\U0001F300-\U0001F5FF" 
        u"\U0001F680-\U0001F6FF" 
        u"\U0001F1E0-\U0001F1FF"
        u"\U00002702-\U000027B0"
        u"\U000024C2-\U0001F251"
        u"\U0001F900-\U0001F9FF"
        u"\U00002600-\U000026FF"
        u"\U0001F170-\U0001F251"
        "]+", flags=re.UNICODE)
    
    cleaned = emoji_pattern.sub('', text)
    cleaned = re.sub(r'\s+', ' ', cleaned).strip()
    return cleaned


async def handle_voice_chat(audio_file) -> dict:
    """Handle voice chat with WAV format only"""
    tmp_path = None
    try:
        # Validate audio format first
        validate_audio_format(audio_file)
        
        # Get file content
        file_content = await audio_file.read()
        content_type = getattr(audio_file, 'content_type', '')
        filename = getattr(audio_file, 'filename', '')
        
        logger.info(f"Processing WAV audio: filename={filename}, content_type={content_type}, size={len(file_content)} bytes")
        
        # Save uploaded WAV file
        with NamedTemporaryFile(delete=False, suffix='.wav') as tmp:
            tmp.write(file_content)
            tmp_path = tmp.name
        
        logger.info(f"Saved WAV file: {tmp_path}")
        
        # Verify WAV file
        if not os.path.exists(tmp_path) or os.path.getsize(tmp_path) == 0:
            raise Exception("WAV file is empty or not created properly")
        
        # Transcribe audio
        initial_text = transcribe_audio(tmp_path)
        logger.info(f"Transcribed text: {initial_text}")

        # Detect language and check if first message
        detected_lang = detect_language(initial_text)
        is_first_message = len(initial_text.strip()) < 20 and any(
            word in initial_text.lower() for word in ['hai', 'halo', 'hello', 'hi', 'hei']
        )

        # Get AI response
        ai_response = get_ai_response_for_voice(initial_text, detected_lang, is_first_message)
        logger.info(f"AI response: {ai_response}")

        # Synthesize speech
        voice_name = "id-ID-GadisNeural" if detected_lang == "id" else "en-US-JennyNeural"
        audio_data = synthesize_speech(ai_response, voice=voice_name)

        return {
            "user_text": initial_text,
            "ai_text": ai_response,
            "audio_bytes": audio_data,
            "audio_format": "wav"
        }

    except HTTPException:
        # Re-raise HTTPException (validation errors) as-is
        raise
    except Exception as e:
        logger.error(f"Voice chat processing error: {str(e)}")
        
        # Fallback response
        fallback_text = (
            "Maaf, aku mengalami kendala dalam memproses suara kamu. "
            "Tapi aku tetap di sini untuk menemani. Coba bicara lagi ya."
        )
        try:
            fallback_audio = synthesize_speech(fallback_text, voice="id-ID-GadisNeural")
            return {
                "user_text": "Error processing audio",
                "ai_text": fallback_text,
                "audio_bytes": fallback_audio,
                "audio_format": "wav"
            }
        except:
            raise Exception(f"Voice chat error: {str(e)}")
            
    finally:
        # Clean up temporary files
        if tmp_path and os.path.exists(tmp_path):
            try:
                os.unlink(tmp_path)
                logger.info(f"Cleaned up temporary file: {tmp_path}")
            except Exception as cleanup_error:
                logger.warning(f"Failed to cleanup {tmp_path}: {cleanup_error}")