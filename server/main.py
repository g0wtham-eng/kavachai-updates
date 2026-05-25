from fastapi import FastAPI, Query
from pydantic import BaseModel
from typing import List, Optional
import time
import random

app = FastAPI(
    title="KavachAI Fraud Detection Server",
    description="Real-time call screening, AI-vs-AI analysis, and database logger backend.",
    version="1.2.0"
)

# Centralized call logger database in-memory
monitored_calls = [
    # Populate some historical mock logs so the dashboard shows charts and maps instantly!
    {
        "phone": "+91 98765 43211",
        "verdict": "SAFE",
        "score": 0.08,
        "voice_status": "Real Human",
        "origin_status": "Jio Network",
        "db_status": "Clean",
        "timestamp": time.time() - 3600 * 4,
        "details": "Verified local community representative. Low probability of risk.",
        "latitude": 12.9716,  # Bengaluru
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
        "details": "Unsolicited promotional request offering rewards. Moderate spam history.",
        "latitude": 19.0760,  # Mumbai
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
        "details": "CRITICAL WARNING: Unsafe banking scam. Detected synthetic robo-voice and OTP phishing attempt.",
        "latitude": 28.6139,  # New Delhi
        "longitude": 77.2090
    }
]

# Central database of Whitelisted SAFE numbers on our server
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
            "KavachAI: Hello, screening call for verification...",
            "Caller: Hey! It's me, just calling to ask if we are meeting for dinner tonight.",
            "KavachAI: Voice signature matched. Contact is whitelisted.",
            "KavachAI: VERDICT: Safe personal contact."
        ]
    },
    "8888822222": {
        "name": "Federal Bank Official Customer Care",
        "verdict": "SAFE",
        "score": 0.05,
        "details": "Verified official institutional helpline. Legitimate bank channel.",
        "voice_status": "Real Human",
        "origin_status": "Airtel Network",
        "db_status": "Clean",
        "transcript": [
            "KavachAI: Hello, this is KavachAI. Please state your verified ID.",
            "Caller: Hello, this is the official Federal Bank Service department. We are calling to confirm your address change.",
            "KavachAI: Checking digital certificate... Certificate MATCHED.",
            "KavachAI: Verified institutional caller. Voice match: Natural."
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
        "service": "KavachAI Fraud Detection Service",
        "version": "1.2.0",
        "logs_count": len(monitored_calls)
    }

@app.get("/api/logs")
def get_logs():
    return {"logs": monitored_calls}

@app.get("/check-caller", response_model=FraudResponse)
def check_caller(phoneNumber: str = Query(..., description="The phone number to screen")):
    # Clean the phone number for consistent matching
    clean_number = "".join(c for c in phoneNumber if c.isdigit())
    
    # Random GPS location in India for the heatmap logs
    mock_lat = random.uniform(12.0, 28.0)
    mock_lon = random.uniform(72.0, 85.0)

    # 1. Check if the number is in our server's whitelisted database
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
            
    # 2. If it is NOT whitelisted, run dynamic AI screening scenario rules
    if clean_number.endswith("1"):
        # Safe community representative scenario
        voice_s = "Real Human"
        origin_s = "Jio Network"
        db_s = "Clean"
        score = 0.08
        verdict = "SAFE"
        details = "Verified local community representative. Low probability of risk."
        transcript = [
            "KavachAI: Hello, identifying caller identity...",
            "Caller: Hi, this is Rajesh from your local community center.",
            "KavachAI: Checking caller database...",
            "KavachAI: Verified local community number. Voice match: Natural."
        ]
    elif clean_number.endswith("2"):
        # Suspicious promotional lucky draw
        voice_s = "Real Human"
        origin_s = "VoIP Network"
        db_s = "Clean"
        score = 0.62
        verdict = "SUSPICIOUS"
        details = "Unsolicited promotional request offering rewards. Moderate spam history."
        transcript = [
            "KavachAI: Hello, this is KavachAI Screening. State your purpose.",
            "Caller: I'm calling about a lucky draw promotion you won!",
            "KavachAI: Analyzing intent and checking databases...",
            "KavachAI: Suspicious request. Potential marketing call or low-risk scam."
        ]
    else:
        # PREMIUM AI-VS-AI FRAUD SCENARIO (Unsafe Banking / OTP Scam)
        voice_s = "AI Cloned (96%)"
        origin_s = "VoIP (Twilio)"
        db_s = "Fraud Registry"
        score = 0.98
        verdict = "FRAUD"
        details = "CRITICAL WARNING: Unsafe banking scam. Detected synthetic robo-voice and OTP phishing attempt."
        transcript = [
            "KavachAI: Hello, this is KavachAI screening assistant. State your name and purpose.",
            "Caller (Robo-AI): This is an automated alert from Federal Bank Security. We detected a suspicious transfer of Rs. 45,000.",
            "KavachAI: Checking server database... No active bank request found. Analyzing voice clone signature...",
            "Caller (Robo-AI): System alert. Repeat the OTP immediately to block this transfer and prevent account suspension.",
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
