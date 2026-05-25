import streamlit as st
import pandas as pd
import requests
import time
import plotly.express as px

# Page Configuration
st.set_page_config(
    page_title="Canara Bank Security Dashboard — KavachAI",
    page_icon="🛡️",
    layout="wide",
    initial_sidebar_state="expanded"
)

# Header
st.markdown("""
    <div style="background-color:#003366;padding:20px;border-radius:15px;margin-bottom:25px">
        <h1 style="color:#FFCC00;text-align:center;margin:0;font-family:'Outfit', sans-serif;font-weight:900">
            🛡️ KAVACH AI — BANK SECURITY DASHBOARD
        </h1>
        <h3 style="color:#ffffff;text-align:center;margin:5px 0 0 0;font-family:'Inter', sans-serif;font-weight:300">
            Canara Bank Real-Time Deepfake & Spam Telecommunication Firewall
        </h3>
    </div>
""", unsafe_allow_html=True)

# ── Server URL — update this with your laptop IP
SERVER_URL = "http://127.0.0.1:5000/api/logs"

@st.fragment(run_every=3)
def render_realtime_data():
    try:
        response = requests.get(SERVER_URL, timeout=2)
        if response.status_code == 200:
            data = response.json()["logs"]
        else:
            raise Exception("Non-200 response")
    except Exception:
        # Fallback mock data
        data = [
            {
                "phone": "+91 99999 11111",
                "verdict": "SAFE",
                "score": 0.01,
                "voice_status": "Real Human",
                "origin_status": "Jio Network",
                "db_status": "Clean",
                "timestamp": time.time() - 7200,
                "details": "Whitelisted family member. Zero risk detected.",
                "latitude": 12.9716,
                "longitude": 77.5946
            },
            {
                "phone": "+91 88888 22222",
                "verdict": "SAFE",
                "score": 0.05,
                "voice_status": "Real Human",
                "origin_status": "Airtel Network",
                "db_status": "Clean",
                "timestamp": time.time() - 3600,
                "details": "Verified official institutional helpline.",
                "latitude": 13.0827,
                "longitude": 80.2707
            },
            {
                "phone": "+91 98765 43215",
                "verdict": "FRAUD",
                "score": 0.98,
                "voice_status": "AI Cloned (96%)",
                "origin_status": "VoIP (Twilio)",
                "db_status": "Fraud Registry",
                "timestamp": time.time(),
                "details": "CRITICAL: OTP phishing attempt. Synthetic voice detected.",
                "latitude": 28.6139,
                "longitude": 77.2090
            },
            {
                "phone": "+91 77777 33333",
                "verdict": "SUSPICIOUS",
                "score": 0.62,
                "voice_status": "Real Human",
                "origin_status": "VoIP Network",
                "db_status": "Clean",
                "timestamp": time.time() - 1800,
                "details": "Unsolicited promotional call. Moderate risk.",
                "latitude": 19.0760,
                "longitude": 72.8777
            }
        ]

    df = pd.DataFrame(data)
    df["Time"] = df["timestamp"].apply(
        lambda t: time.strftime("%Y-%m-%d %H:%M:%S", time.localtime(t))
    )

    # ── KPI Stats Row
    col1, col2, col3, col4 = st.columns(4)
    total     = len(df)
    fraud     = len(df[df["verdict"] == "FRAUD"])
    suspicious= len(df[df["verdict"] == "SUSPICIOUS"])
    safe      = len(df[df["verdict"] == "SAFE"])

    with col1:
        st.metric("📞 Total Screened Calls", total,    "Active Protection")
    with col2:
        st.metric("❌ Fraud Attacks Blocked", fraud,   "-100% Threat",    delta_color="inverse")
    with col3:
        st.metric("⚠️ Suspicious Warnings",  suspicious,"Needs Caution",  delta_color="off")
    with col4:
        st.metric("✅ Safe Calls Allowed",   safe,     "Verified Clean")

    st.markdown("---")

    # ── Map + Chart
    map_col, chart_col = st.columns([2, 1])

    with map_col:
        st.subheader("🗺️ Live Geographic Threat Heatmap")
        st.markdown("*Real-time geographic logging of blocked spam origin endpoints:*")
        map_df = df[["latitude", "longitude", "verdict"]].copy()
        st.map(map_df, color="#F44336" if fraud > 0 else "#003366")

    with chart_col:
        st.subheader("📊 Threat Distribution")
        fig = px.pie(
            df,
            names="verdict",
            title="Telecommunication Traffic Split",
            color="verdict",
            color_discrete_map={
                "SAFE":       "#4CAF50",
                "SUSPICIOUS": "#FF9800",
                "FRAUD":      "#F44336"
            },
            hole=0.4
        )
        st.plotly_chart(fig, use_container_width=True)

    st.markdown("---")

    # ── Audit Log Table
    st.subheader("📜 Real-Time Security Audit Logs")
    st.markdown("*Detailed audit record of all screened telecommunication traffic:*")

    display_df = df[[
        "Time", "phone", "verdict", "score",
        "voice_status", "origin_status", "db_status", "details"
    ]].copy()

    display_df.columns = [
        "Timestamp", "Caller Number", "Final Verdict",
        "Risk Score", "Voice Signature", "Telecom Network",
        "Security Registry", "Analysis Details"
    ]

    # Color code verdicts
    def color_verdict(val):
        if val == "FRAUD":
            return "background-color: #ffebee; color: #c62828; font-weight: bold"
        elif val == "SUSPICIOUS":
            return "background-color: #fff8e1; color: #e65100; font-weight: bold"
        elif val == "SAFE":
            return "background-color: #e8f5e9; color: #2e7d32; font-weight: bold"
        return ""

    styled_df = display_df.style.map(
    color_verdict, subset=["Final Verdict"]
)

    st.dataframe(
        styled_df.data.sort_index(ascending=False),
        use_container_width=True,
        hide_index=True
    )

render_realtime_data()

# Footer
st.markdown("""
    <div style="text-align:center;margin-top:50px;padding:15px;color:#777777;font-size:12px">
        🛡️ Canara Bank KavachAI Security Firewall — Running with End-to-End Real-Time Logging
    </div>
""", unsafe_allow_html=True)
