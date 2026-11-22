# 🌈 الألوان — Apprendre les couleurs en arabe

Application Android (APK) pour apprendre les couleurs en **arabe standard** à un enfant de 3 ans, avec des sons **intégrés à l'application** (aucune connexion ni voix à installer).

## 📥 Télécharger l'APK

**➡️ [couleurs-darija.apk](https://github.com/redatest/Apprendre_couleur_apk2/releases/latest/download/couleurs-darija.apk)**

(Page des versions : https://github.com/redatest/Apprendre_couleur_apk2/releases)

Installation : ouvrir le lien sur le téléphone Android, télécharger, puis autoriser « Installer des applis inconnues » si Android le demande. Si la mise à jour est refusée, désinstaller l'ancienne version d'abord.

## Fonctionnement

- **Mode apprentissage 📚** : 10 cartes avec une forme colorée souriante (cercle, carré, triangle, étoile, cœur). Un appui = la forme tourne et rebondit, des confettis éclatent et le nom de la couleur est dit deux fois.
- **Mode jeu 🎮** : « أين الأحمر ؟ » (avec la couleur demandée) ; l'enfant touche la bonne forme parmi 3. Bonne réponse = confettis + « رائع / ممتاز / أحسنت ». Mauvaise réponse = la carte tremble et l'app dit « لا، هذا … » suivi de la couleur touchée.

Couleurs : أحمر (ahmar), أزرق (azraq), أخضر (akhdar), أصفر (asfar), برتقالي (burtuqali), بنفسجي (banafsaji), وردي (wardi), أبيض (abyad), أسود (aswad), بني (bunni).

## Sons

Les 24 clips sont dans `app/src/main/res/raw/` (format `.ogg`) : 10 couleurs (`c_*`), 10 questions « أين الـ… ؟ » (`q_*`), « لا، هذا » (`p_la_hadha`) et 3 félicitations (`pr_*`).

Ils sont générés avec une voix neuronale arabe féminine (Piper, `ar_JO-SA_dii-high`) par `tools/generate_sounds.py`. Pour une voix humaine (la vôtre ou celle d'un proche), enregistrez chaque mot et **remplacez les fichiers `.ogg` en gardant exactement les mêmes noms** ; il n'y a rien d'autre à modifier. Chaque push relance la compilation.

## Compilation

L'APK est compilé automatiquement par GitHub Actions (`.github/workflows/build-apk.yml`) à chaque push sur `main` et publié dans la release `latest`. Pour compiler en local : `gradle assembleDebug`.
