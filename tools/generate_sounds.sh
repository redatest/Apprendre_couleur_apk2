#!/usr/bin/env bash
# Génère les clips audio (arabe standard) avec espeak-ng + ffmpeg dans app/src/main/res/raw.
# Prérequis : espeak-ng, ffmpeg.   Usage : bash tools/generate_sounds.sh
# Pour utiliser de vraies voix humaines : remplacez simplement les .ogg de app/src/main/res/raw
# (mêmes noms de fichiers).
set -euo pipefail
OUT="$(dirname "$0")/../app/src/main/res/raw"
TMP="$(mktemp -d)"
mkdir -p "$OUT"

gen() {  # nom  texte
  espeak-ng -v ar+f3 -s 105 -p 68 -g 6 -w "$TMP/$1.wav" "$2"
  ffmpeg -y -loglevel error -i "$TMP/$1.wav" \
    -af "silenceremove=start_periods=1:start_threshold=-45dB:start_silence=0.02,areverse,silenceremove=start_periods=1:start_threshold=-45dB:start_silence=0.02,areverse,adelay=60|60,dynaudnorm=f=150:g=7,volume=1.6,alimiter=limit=0.95,apad=pad_dur=0.08" \
    -ar 22050 -ac 1 -c:a libvorbis -q:a 4 "$OUT/$1.ogg"
}

# Couleurs
gen c_ahmar      "أَحْمَر"
gen c_azraq      "أَزْرَق"
gen c_akhdar     "أَخْضَر"
gen c_asfar      "[[?'as.far]]"
gen c_burtuqali  "بُرْتُقَالِيّ"
gen c_banafsaji  "بَنَفْسَجِيّ"
gen c_wardi      "وَرْدِيّ"
gen c_abyad      "أَبْيَض"
gen c_aswad      "أَسْوَد"
gen c_bunni      "بُنِّيّ"
# Phrases
gen p_ayna       "[[?'ajna ?al.l'awnu]]"
gen p_la_hadha   "لَا، هَذَا"
# Félicitations
gen pr_raai      "رَائِع"
gen pr_mumtaz    "مُمْتَاز"
gen pr_ahsanta   "أَحْسَنْتَ"
rm -rf "$TMP"
