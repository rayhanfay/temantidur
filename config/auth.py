import firebase_admin
from firebase_admin import credentials, auth as firebase_auth
from fastapi import HTTPException, status, Depends
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
import os
import json

# Initialize Firebase
firebase_json = os.getenv("FIREBASE_JSON")
if firebase_json:
    cred = credentials.Certificate(json.loads(firebase_json))
else:
    raise ValueError("FIREBASE_JSON environment variable not set")

# Initialize Firebase app if not already initialized
if not firebase_admin._apps:
    firebase_admin.initialize_app(cred)

# Create HTTPBearer instance
security = HTTPBearer()

def verify_firebase_token(credentials: HTTPAuthorizationCredentials = Depends(security)):
    """
    Dependency function to verify Firebase token
    """
    token = credentials.credentials
    try:
        decoded_token = firebase_auth.verify_id_token(token)
        return decoded_token
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED, 
            detail=f"Invalid Firebase token: {str(e)}"
        )
    
    