from fastapi import Request
from fastapi.responses import JSONResponse
from fastapi.exceptions import RequestValidationError
from starlette.exceptions import HTTPException as StarletteHTTPException
import logging

logger = logging.getLogger(__name__)

async def http_exception_handler(request: Request, exc: StarletteHTTPException):
    """Handler untuk HTTP exceptions"""
    logger.error(f"HTTP {exc.status_code} pada {request.url}: {exc.detail}")
    
    if exc.status_code == 404:
        return JSONResponse(
            status_code=404,
            content={
                "error": "Endpoint tidak ditemukan",
                "available_endpoints": ["/", "/chat", "/detect-emotion", "/voice-chat"]
            }
        )
    elif exc.status_code == 401:
        return JSONResponse(
            status_code=401,
            content={
                "error": "Token autentikasi tidak valid",
                "message": "Sertakan header 'Authorization: Bearer <firebase_token>'"
            }
        )
    elif exc.status_code == 403:
        return JSONResponse(
            status_code=403,
            content={
                "error": "Akses ditolak",
                "message": "Token tidak memiliki izin yang diperlukan"
            }
        )
    
    return JSONResponse(
        status_code=exc.status_code,
        content={"error": f"HTTP {exc.status_code}", "detail": str(exc.detail)}
    )

async def validation_exception_handler(request: Request, exc: RequestValidationError):
    """Handler untuk validation errors"""
    logger.error(f"Validation error pada {request.url}: {exc.errors()}")
    
    return JSONResponse(
        status_code=422,
        content={
            "error": "Data tidak valid",
            "details": [
                {
                    "field": " -> ".join(str(loc) for loc in error["loc"]),
                    "message": error["msg"]
                }
                for error in exc.errors()
            ]
        }
    )

async def general_exception_handler(request: Request, exc: Exception):
    """Handler untuk general exceptions"""
    logger.error(f"Server error pada {request.url}: {str(exc)}")
    
    return JSONResponse(
        status_code=500,
        content={
            "error": "Internal server error",
            "message": "Silakan coba lagi atau hubungi administrator"
        }
    )

def register_exception_handlers(app):
    """Register semua exception handlers ke FastAPI app"""
    app.add_exception_handler(StarletteHTTPException, http_exception_handler)
    app.add_exception_handler(RequestValidationError, validation_exception_handler)
    app.add_exception_handler(Exception, general_exception_handler)