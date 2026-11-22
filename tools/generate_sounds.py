#!/usr/bin/env python3
"""Génère les clips audio (arabe standard) dans app/src/main/res/raw avec une voix neuronale Piper.

Prérequis : pip install piper-tts ; ffmpeg ; la voix « ar_JO-SA_dii-high » (voix féminine) :
  https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-piper-ar_JO-SA_dii-high.tar.bz2
Usage : python3 tools/generate_sounds.py /chemin/vers/vits-piper-ar_JO-SA_dii-high
Pour de vraies voix humaines : remplacez les .ogg de app/src/main/res/raw (mêmes noms).
"""
import glob, os, subprocess, sys, tempfile, wave
from piper import PiperVoice, SynthesisConfig

voice_dir = sys.argv[1]
out_dir = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "app", "src", "main", "res", "raw")
os.makedirs(out_dir, exist_ok=True)
voice = PiperVoice.load(glob.glob(os.path.join(voice_dir, "*.onnx"))[0])
cfg = SynthesisConfig(length_scale=1.15, noise_scale=0.5, noise_w_scale=0.6)  # un peu plus lent et plus stable

COLORS = {  # nom de fichier : mot diacritisé
    "ahmar": "أَحْمَر", "azraq": "أَزْرَق", "akhdar": "أَخْضَر", "asfar": "أَصْفَر",
    "burtuqali": "بُرْتُقَالِي", "banafsaji": "بَنَفْسَجِي", "wardi": "وَرْدِي",
    "abyad": "أَبْيَض", "aswad": "أَسْوَد", "bunni": "بُنِّي",
}
CLIPS = {f"c_{k}": v for k, v in COLORS.items()}
CLIPS.update({f"q_{k}": f"أَيْنَ ال{v}؟" for k, v in COLORS.items()})  # « Où est le … ? »
CLIPS.update({
    "p_la_hadha": "لَا، هَذَا",
    "pr_raai": "رَائِع!",
    "pr_mumtaz": "مُمْتَاز!",
    "pr_ahsanta": "أَحْسَنْتَ!",
})

# coupe les silences aux extrémités, normalise le volume, ajoute une courte marge
AF = ("silenceremove=start_periods=1:start_threshold=-50dB:start_silence=0.03,"
      "areverse,silenceremove=start_periods=1:start_threshold=-50dB:start_silence=0.05,areverse,"
      "loudnorm=I=-14:TP=-1.5:LRA=7,adelay=50|50,apad=pad_dur=0.1")

with tempfile.TemporaryDirectory() as tmp:
    for name, text in CLIPS.items():
        wav = os.path.join(tmp, name + ".wav")
        with wave.open(wav, "wb") as w:
            voice.synthesize_wav(text, w, syn_config=cfg)
        subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", wav, "-af", AF,
                        "-ar", "22050", "-ac", "1", "-c:a", "libvorbis", "-q:a", "5",
                        os.path.join(out_dir, name + ".ogg")], check=True)
        print("ok", name, text)
