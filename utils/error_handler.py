from fastapi import Request
from fastapi.responses import JSONResponse
from fastapi.exceptions import RequestValidationError
from starlette.exceptions import HTTPException as StarletteHTTPException
import logging

logger = logging.getLogger(__name__)

async def http_exception_handler(request: Request, exc: StarletteHTTPException):
    """Handler for HTTP exceptions"""
    logger.error(f"HTTP {exc.status_code} on {request.url}: {exc.detail}")
    
    if exc.status_code == 404:
        return JSONResponse(
            status_code=404,
            content={
                "error": "Endpoint not found",
                "available_endpoints": ["/", "/chat", "/detect-emotion", "/voice-chat"]
            }
        )
    elif exc.status_code == 401:
        return JSONResponse(
            status_code=401,
            content={
                "error": "Invalid authentication token",
                "message": "Include header 'Authorization: Bearer <firebase_token>'"
            }
        )
    elif exc.status_code == 403:
        return JSONResponse(
            status_code=403,
            content={
                "error": "Access denied",
                "message": "Token does not have required permissions"
            }
        )
    
    return JSONResponse(
        status_code=exc.status_code,
        content={"error": f"HTTP {exc.status_code}", "detail": str(exc.detail)}
    )

async def validation_exception_handler(request: Request, exc: RequestValidationError):
    """Handler for validation errors"""
    logger.error(f"Validation error on {request.url}: {exc.errors()}")
    
    return JSONResponse(
        status_code=422,
        content={
            "error": "Invalid data",
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
    """Handler for general exceptions"""
    logger.error(f"Server error on {request.url}: {str(exc)}")
    
    return JSONResponse(
        status_code=500,
        content={
            "error": "Internal server error",
            "message": "Please try again or contact administrator"
        }
    )

def register_exception_handlers(app):
    """Register all exception handlers to FastAPI app"""
    app.add_exception_handler(StarletteHTTPException, http_exception_handler)
    app.add_exception_handler(RequestValidationError, validation_exception_handler)
    app.add_exception_handler(Exception, general_exception_handler)