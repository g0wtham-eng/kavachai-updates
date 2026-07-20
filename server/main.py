from fastapi import FastAPI, Query
from pydantic import BaseModel
from typing import List, Optional
import time
import random

app = FastAPI(
    title="Canara Bank Security Server",
    description="Real-time call screening and fraud detection for Canara Bank customers.",
    version="2.0.0"
)

# Centralized call logger database in-memory
monitored_calls = [
    {
        "phone": "+91 98765 43211",
        "verdict": "SAFE",
        "score": 0.08,
        "voice_status": "Real Human",
        "origin_status": "Jio Network",
        "db_status": "Clean",
        "timestamp": time.time() - 3600 * 4,
        "details": "Verified local community representative. Low probability of risk.",
        "latitude": 12.9716,
        "longitude": 77.5946
    },
    {
        "phone": "+91 88776 65522",
        "verdict": "SUSPICIOUS",
        "score": 0.62,
        "voice_status": "Real Human",
        "origin_status": "Airtel Network",
        "db_status": "Clean",
        "timestamp": time.time() - 3600 * 2,
        "details": "Unsolicited loan offer. Not an official Canara Bank channel.",
        "latitude": 19.0760,
        "longitude": 72.8777
    },
    {
        "phone": "+91 99999 00005",
        "verdict": "FRAUD",
        "score": 0.98,
        "voice_status": "AI Cloned (96%)",
        "origin_status": "VoIP (Twilio)",
        "db_status": "Fraud Registry",
        "timestamp": time.time() - 3600,
        "details": "CRITICAL WARNING: Canara Bank account phishing. Detected synthetic robo-voice.",
        "latitude": 28.6139,
        "longitude": 77.2090
    }
]

SAFE_WHITELIST = {
    "9999911111": {
        "name": "Verified Personal Contact (Family)",
        "verdict": "SAFE",
        "score": 0.01,
        "details": "Whitelisted family member. Zero risk detected.",
        "voice_status": "Real Human",
        "origin_status": "Jio Network",
        "db_status": "Clean",
        "transcript": [
            "KavachAI: Hello, this is Canara Bank's security verification system. Please state your name and reason for calling.",
            "Caller: Hey! Just calling about dinner.",
            "KavachAI: Cross-referencing caller ID with Jio/Airtel network logs...",
            "KavachAI: Voice signature matched. Safe personal contact."
        ]
    },
    "8888822222": {
        "name": "Canara Bank Official Customer Care",
        "verdict": "SAFE",
        "score": 0.05,
        "details": "Verified official institutional helpline. Legitimate bank channel.",
        "voice_status": "Real Human",
        "origin_status": "Airtel Network",
        "db_status": "Clean",
        "transcript": [
            "KavachAI: Hello, this is Canara Bank's security verification system. Please state your name and reason for calling.",
            "Caller: Hello, this is the official Canara Bank Service department.",
            "KavachAI: Verifying number routing through Jio/Airtel telecom networks...",
            "KavachAI: Network path verified. Official institutional caller."
        ]
    }
}

class FraudResponse(BaseModel):
    verdict: str
    score: float
    details: Optional[str] = None
    transcript: List[str]
    voiceStatus: str
    originStatus: str
    dbStatus: str

@app.get("/")
def read_root():
    return {
        "status": "online",
        "service": "Canara Bank Security Service",
        "version": "2.0.0",
        "logs_count": len(monitored_calls)
    }

@app.get("/api/logs")
def get_logs():
    return {"logs": monitored_calls}

@app.get("/check-caller", response_model=FraudResponse)
def check_caller(phoneNumber: str = Query(..., description="The phone number to screen")):
    clean_number = "".join(c for c in phoneNumber if c.isdigit())
    
    mock_lat = random.uniform(12.0, 28.0)
    mock_lon = random.uniform(72.0, 85.0)

    for whitelisted_num, data in SAFE_WHITELIST.items():
        if clean_number.endswith(whitelisted_num):
            log_entry = {
                "phone": phoneNumber,
                "verdict": data["verdict"],
                "score": data["score"],
                "voice_status": data["voice_status"],
                "origin_status": data["origin_status"],
                "db_status": data["db_status"],
                "timestamp": time.time(),
                "details": data["details"],
                "latitude": mock_lat,
                "longitude": mock_lon
            }
            monitored_calls.append(log_entry)
            
            return FraudResponse(
                verdict=data["verdict"],
                score=data["score"],
                details=data["details"],
                transcript=data["transcript"],
                voiceStatus=data["voice_status"],
                originStatus=data["origin_status"],
                dbStatus=data["db_status"]
            )
            
    if clean_number.endswith("1"):
        voice_s = "Real Human"
        origin_s = "Jio Network"
        db_s = "Clean"
        score = 0.08
        verdict = "SAFE"
        details = "Verified local community representative."
        transcript = [
            "KavachAI: Hello, this is Canara Bank's security verification system. Please state your name and reason for calling.",
            "Caller: Hi, this is Rajesh from your local community center.",
            "KavachAI: Cross-referencing caller ID with official databases...",
            "KavachAI: Verified local community number. Safe to answer."
        ]
    elif clean_number.endswith("2"):
        voice_s = "Real Human"
        origin_s = "VoIP Network"
        db_s = "Clean"
        score = 0.62
        verdict = "SUSPICIOUS"
        details = "Unsolicited loan request offering rewards."
        transcript = [
            "KavachAI: Hello, this is Canara Bank's security verification system. Please state your name and reason for calling.",
            "Caller: I'm calling about a pre-approved Canara Bank loan!",
            "KavachAI: Verifying number routing through Jio/Airtel telecom networks...",
            "KavachAI: Alert: Number is not routed through official Canara Bank servers.",
            "KavachAI: Suspicious request. Potential marketing call."
        ]
    else:
        voice_s = "AI Cloned (96%)"
        origin_s = "VoIP (Twilio)"
        db_s = "Fraud Registry"
        score = 0.98
        verdict = "FRAUD"
        details = "CRITICAL WARNING: Canara Bank account phishing. Detected synthetic robo-voice."
        transcript = [
            "KavachAI: Hello, this is Canara Bank's security verification system. Please state your name and reason for calling.",
            "Caller (Robo-AI): This is an automated alert from Canara Bank. We detected a suspicious transfer of Rs. 45,000.",
            "KavachAI: Interrogating server connection... Routing traced to unknown VoIP network, not Airtel/Jio.",
            "KavachAI: Checking Canara Server database... No active bank request found.",
            "Caller (Robo-AI): System alert. Repeat the OTP immediately to block this transfer.",
            "KavachAI: WARNING: Deepfake voice synthesis probability: 96%. Phishing signature verified (OTP demand).",
            "KavachAI: TERMINATING CALL. Blocked banking threat."
        ]

    # RAG Trigger Check: Search transcript for security threat keywords (OTP, pin, password, card)
    # If any fraud indicator is present in the dialogue, force FRAUD verdict!
    full_text = " ".join(transcript).lower()
    if any(keyword in full_text for keyword in ["otp", "pin", "password", "card", "cvv"]):
        verdict = "FRAUD"
        score = 0.98
        voice_s = "AI Cloned (96%)"
        db_s = "Fraud Registry"
        details = "CRITICAL WARNING: Unsafe banking scam. Detected synthetic robo-voice and OTP phishing attempt."

    # Save to the in-memory log database
    log_entry = {
        "phone": phoneNumber,
        "verdict": verdict,
        "score": score,
        "voice_status": voice_s,
        "origin_status": origin_s,
        "db_status": db_s,
        "timestamp": time.time(),
        "details": details,
        "latitude": mock_lat,
        "longitude": mock_lon
    }
    monitored_calls.append(log_entry)

    return FraudResponse(
        verdict=verdict,
        score=score,
        details=details,
        transcript=transcript,
        voiceStatus=voice_s,
        originStatus=origin_s,
        dbStatus=db_s
    )
