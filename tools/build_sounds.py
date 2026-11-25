#!/usr/bin/env python3
"""Prépare les sons de l'application dans app/src/main/res/raw (format .ogg).

1) Prononciations des couleurs : fichiers du dossier colors/ (red.mp3, blue.mp3, ... wow.mp3)
   -> silences coupés, volume normalisé -> c_*.ogg et pr_wow.ogg.
   + « where is the colour.mp3 » -> p_ayna (question du jeu), « not this.mp3 » -> p_la_hadha (mauvaise réponse).
2) Félicitations رائع / ممتاز / أحسنت : voix neuronale Piper (optionnel).
   pip install piper-tts ; voix : vits-piper-ar_JO-SA_dii-high (sherpa-onnx, release « tts-models »)

Usage : python3 tools/build_sounds.py [dossier_voix_piper]
Pour remplacer un son : changez le fichier de colors/ (ou le .ogg de res/raw, même nom).
"""
import glob, os, subprocess, sys, tempfile, wave

ROOT = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
RAW = os.path.join(ROOT, "app", "src", "main", "res", "raw")
COLORS = os.path.join(ROOT, "colors")
os.makedirs(RAW, exist_ok=True)

MAP = {  # fichier colors/  ->  ressource Android
    "red": "c_ahmar", "blue": "c_azraq", "green": "c_akhdar", "yellow": "c_asfar",
    "orange": "c_burtuqali", "purple": "c_banafsaji", "pink": "c_wardi",
    "white": "c_abyad", "black": "c_aswad", "brown": "c_bunni", "wow": "pr_wow",
    "where is the colour": "p_ayna", "not this": "p_la_hadha",
}
AF = ("silenceremove=start_periods=1:start_threshold=-45dB:start_silence=0.03,"
      "areverse,silenceremove=start_periods=1:start_threshold=-45dB:start_silence=0.06,areverse,"
      "loudnorm=I=-14:TP=-1.5:LRA=7,adelay=40|40,apad=pad_dur=0.1")

def encode(src, name, sr):
    subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", src, "-af", AF, "-ar", str(sr),
                    "-ac", "1", "-c:a", "libvorbis", "-q:a", "5",
                    os.path.join(RAW, name + ".ogg")], check=True)
    print("ok", name)

for f in glob.glob(os.path.join(RAW, "c_*.ogg")) + glob.glob(os.path.join(RAW, "q_*.ogg")) + \
        glob.glob(os.path.join(RAW, "pr_wow.ogg")) + glob.glob(os.path.join(RAW, "p_*.ogg")):
    os.remove(f)
for src, name in MAP.items():
    encode(os.path.join(COLORS, src + ".mp3"), name, 44100)

if len(sys.argv) > 1:
    from piper import PiperVoice, SynthesisConfig
    voice = PiperVoice.load(glob.glob(os.path.join(sys.argv[1], "*.onnx"))[0])
    cfg = SynthesisConfig(length_scale=1.15, noise_scale=0.5, noise_w_scale=0.6)
    PHRASES = {
        "pr_raai": "رَائِع!",
        "pr_mumtaz": "مُمْتَاز!",
        "pr_ahsanta": "أَحْسَنْتَ!",
    }
    with tempfile.TemporaryDirectory() as tmp:
        for name, text in PHRASES.items():
            wav = os.path.join(tmp, name + ".wav")
            with wave.open(wav, "wb") as w:
                voice.synthesize_wav(text, w, syn_config=cfg)
            encode(wav, name, 22050)
