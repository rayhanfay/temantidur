import os
from openai import AzureOpenAI
from dotenv import load_dotenv

load_dotenv()

# Azure OpenAI Configuration
client = AzureOpenAI(
    api_version=os.getenv("AZURE_API_VERSION"),
    azure_endpoint=os.getenv("AZURE_OPENAI_ENDPOINT"),
    api_key=os.getenv("AZURE_OPENAI_KEY"),
    # Remove any proxy or timeout configurations that might be causing issues
)

DEPLOYMENT_NAME = os.getenv("AZURE_DEPLOYMENT_NAME")

# Azure Custom Vision Configuration
AZURE_CUSTOM_VISION_ENDPOINT = os.getenv("AZURE_CUSTOM_VISION_ENDPOINT")
AZURE_CUSTOM_VISION_KEY = os.getenv("AZURE_CUSTOM_VISION_KEY")

# Azure Speech Configuration
SPEECH_KEY = os.getenv("AZURE_SPEECH_KEY")
SPEECH_REGION = os.getenv("AZURE_SPEECH_REGION")