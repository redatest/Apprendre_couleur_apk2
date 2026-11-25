# 🌈 الألوان — Apprendre les couleurs en arabe

Application Android (APK) pour apprendre les couleurs en **arabe standard** à un enfant de 3 ans, avec des sons **intégrés à l'application** (aucune connexion ni voix à installer).

## 📥 Télécharger l'APK

**➡️ [couleurs-darija.apk](https://github.com/redatest/Apprendre_couleur_apk2/releases/latest/download/couleurs-darija.apk)**

(Page des versions : https://github.com/redatest/Apprendre_couleur_apk2/releases)

Installation : ouvrir le lien sur le téléphone Android, télécharger, puis autoriser « Installer des applis inconnues » si Android le demande. Si la mise à jour est refusée, désinstaller l'ancienne version d'abord.

## Fonctionnement

- **Interface 3D** : formes solides éclairées (sphère, cube, pyramide, étoile et cœur en volume) qui oscillent en continu, avec ombre au sol et un petit visage qui suit la rotation ; cartes et boutons « en relief » qui s'inclinent vers le doigt quand on appuie ; bulles brillantes qui flottent en fond ; confettis « papier » qui tournent en 3D.
- **Mode apprentissage 📚** : 10 cartes. Un appui = la forme fait un tour sur elle-même, des confettis éclatent, la couleur est dite une fois, puis un pouce levé 👍 apparaît.
- **Mode jeu 🎮** : « Where is the colour? » (ton fichier) puis le nom de la couleur ; l'enfant touche la bonne forme parmi 3. Bonne réponse = confettis + « wow » (ou رائع / ممتاز / أحسنت) + pouce levé 👍. Mauvaise réponse = la carte tremble et l'app dit « not this » (ton fichier) suivi de la couleur touchée.

Couleurs : أحمر (ahmar), أزرق (azraq), أخضر (akhdar), أصفر (asfar), برتقالي (burtuqali), بنفسجي (banafsaji), وردي (wardi), أبيض (abyad), أسود (aswad), بني (bunni).

## Sons

- **Prononciations des couleurs** : fichiers du dossier `colors/` (`red.mp3`, `blue.mp3`, … `wow.mp3`). `tools/build_sounds.py` les convertit dans `app/src/main/res/raw/` (silences coupés, volume normalisé).
- **Question et mauvaise réponse** : `where is the colour.mp3` et `not this.mp3` (dossier `colors/`).
- **Félicitations** رائع / ممتاز / أحسنت : voix neuronale arabe (Piper, `ar_JO-SA_dii-high`), générées par le même script (optionnel).

Pour changer un son : remplacez le fichier correspondant dans `colors/` (même nom), relancez `python3 tools/build_sounds.py` et poussez. 

## Compilation

L'APK est compilé automatiquement par GitHub Actions (`.github/workflows/build-apk.yml`) à chaque push sur `main` et publié dans la release `latest`. Pour compiler en local : `gradle assembleDebug`.
