# 🌈 الألوان — Apprendre les couleurs en darija algérienne

Application Android (APK) pour apprendre les couleurs en darija algérienne à un enfant de 3 ans.

## 📥 Télécharger l'APK

**➡️ [couleurs-darija.apk](https://github.com/redatest/Apprendre_couleur_apk2/releases/latest/download/couleurs-darija.apk)**

(Page des versions : https://github.com/redatest/Apprendre_couleur_apk2/releases)

Installation : ouvrir le lien sur le téléphone Android, télécharger, puis autoriser « Installer des applis inconnues » si Android le demande.

## Fonctionnement

- **Mode apprentissage 📚** : 10 grosses cartes, chacune avec une forme colorée souriante (cercle, carré, triangle, étoile, cœur). Un appui = la forme tourne et rebondit, des confettis éclatent et le nom de la couleur est prononcé.
- **Mode jeu 🎮** : l'application demande « وِين الحمر ؟ » ; l'enfant touche la bonne forme parmi 3. Bonne réponse = confettis + « برافو / مليح ». Mauvaise réponse = la carte tremble et l'application dit quelle couleur c'est.

Couleurs : حمر (hmar), زرق (zerq), خضر (khdhar), صفر (sfar), برتقالي (bortoqali), بنفسجي (banafsaji), وردي (wardi), بيض (bayad), كحل (kahl), قهوي (qahwi).

## Voix et son

Ordre de priorité pour dire une couleur : **voix enregistrée par le parent** > synthèse vocale arabe d'Android > simple bip.

- **Enregistrer votre propre voix (recommandé, vrai darija)** : en mode apprentissage, appuyez longuement sur une carte → *Enregistrer* → dites le mot → *Arrêter*. Le téléphone demande l'autorisation du micro la première fois.
- **Si un bandeau orange « Voix arabe introuvable » apparaît** : touchez-le pour installer la voix arabe (Synthèse vocale Google), puis revenez dans l'app. Sur les téléphones sans services Google (certains Huawei), utilisez plutôt l'enregistrement.
- Pas de son du tout : vérifier le volume **média** (les touches de volume le contrôlent dans l'app) et que le téléphone n'est pas en silencieux.

## Compilation

L'APK est compilé automatiquement par GitHub Actions (`.github/workflows/build-apk.yml`) à chaque push sur `main` et publié dans la release `latest`. Pas besoin d'Android Studio. Pour compiler en local : `gradle assembleDebug`.

Remarque : l'APK est signé avec une clé de debug générée à chaque build ; pour installer une nouvelle version, désinstaller l'ancienne si Android refuse la mise à jour.
