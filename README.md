# IntruderSnap

Application Android (Kotlin) de protection par code PIN : verrouille les applications de votre choix et, après un nombre d'échecs configurable, prend une photo avec la caméra frontale et l'envoie par email.

**Usage prévu : protéger VOTRE PROPRE téléphone.** Le fonctionnement est expliqué en clair à l'utilisateur dès le premier lancement (écran de consentement obligatoire) ; l'icône de l'app reste toujours visible et une notification signale en permanence que la protection est active. Utiliser ce type d'application pour surveiller quelqu'un d'autre à son insu peut être illégal selon votre juridiction — ce n'est pas l'usage pour lequel elle a été conçue.

## Fonctionnalités

- Verrouillage par code PIN (haché SHA-256 + sel, jamais stocké en clair)
- Protection sélective d'applications tierces via un service d'accessibilité
- Capture photo (caméra frontale, CameraX) après N échecs consécutifs — seuil réglable
- Envoi automatique de la photo par email (SMTP configuré par l'utilisateur, identifiants chiffrés via Android Keystore / EncryptedSharedPreferences)
- Notification permanente tant que la protection est active (transparence obligatoire)
- Journal local des tentatives (réussites, échecs, captures envoyées), consultable et effaçable à tout moment
- Écran de consentement bloquant au premier lancement, consultable à nouveau depuis le tableau de bord

## Ouvrir le projet

Le projet inclut maintenant son propre **wrapper Gradle** (`gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`, verrouillé sur **Gradle 8.7**, la version minimale exigée par AGP 8.5.2). Ni Android Studio ni vous n'avez besoin de le générer.

1. Décompressez le zip, puis ouvrez le dossier `IntruderSnap/` dans **Android Studio Koala (2024.1) ou plus récent**.
2. À l'ouverture, Android Studio doit détecter `gradlew` et proposer de synchroniser avec le wrapper du projet — acceptez (ne choisissez pas "utiliser le Gradle local de l'IDE").
3. Vérifiez que le **JDK utilisé par Gradle est en version 17** : File → Settings → Build, Execution, Deployment → Build Tools → Gradle → "Gradle JDK" doit être 17 (ou "jbr-17"). C'est la cause la plus fréquente d'échec de synchronisation sur un projet comme celui-ci si l'IDE utilise un JDK plus ancien.
4. Laissez Gradle télécharger les dépendances listées dans `app/build.gradle.kts` (nécessite une connexion internet normale, sans restriction, vers `dl.google.com` et `repo.maven.apache.org`).
5. Connectez un appareil ou lancez un émulateur avec **API 26+** (Android 8.0 minimum, requis pour `setShowWhenLocked`).
6. Lancez `app` (Run ▶).

> En ligne de commande : `./gradlew assembleDebug` (Linux/macOS) ou `gradlew.bat assembleDebug` (Windows) à la racine du projet.

### Si la synchronisation échoue encore

Ouvrez l'onglet **Build** (ou **Sync**) en bas d'Android Studio et copiez le texte exact de l'erreur (souvent précédé de `FAILURE:` ou `e: ...` pour les erreurs Kotlin) — c'est la seule façon de diagnostiquer précisément ce qui bloque chez vous, un sync Gradle pouvant échouer pour des dizaines de raisons différentes (JDK, proxy réseau, SDK manquant, cache corrompu...).

## Configuration après installation

1. **Consentement** : lisez et acceptez l'écran expliquant la capture photo.
2. **Code PIN** : choisissez un code à 4–8 chiffres.
3. **Tableau de bord** :
   - *Service de verrouillage* : appuyez sur la ligne d'état pour ouvrir les réglages d'accessibilité Android et activer IntruderSnap (requis pour protéger d'autres apps).
   - *Échecs avant capture photo* : nombre d'essais autorisés avant déclenchement.
   - *Email* : renseignez votre serveur SMTP (ex. `smtp.gmail.com`, port `587`), l'adresse expéditrice, un **mot de passe d'application** (ne jamais utiliser votre mot de passe principal), et l'adresse destinataire des alertes. Un bouton « Envoyer un email de test » permet de vérifier la configuration.
   - *Applications protégées* : cochez les applications à verrouiller par PIN.
   - *Journal* : historique des tentatives.

## Permissions demandées

| Permission | Pourquoi |
|---|---|
| Caméra | Prendre la photo après échec de PIN |
| Internet | Envoyer l'email via SMTP |
| Service d'accessibilité | Détecter quand une app protégée passe au premier plan |
| Service de premier plan (caméra) | Exécuter la capture + l'envoi hors de l'UI, avec notification obligatoire |
| Démarrage au boot | Recevoir l'événement système (aucune action réseau/caméra déclenchée par ce récepteur) |

## Structure du projet

```
app/src/main/java/com/michel/intrudersnap/
├── IntruderSnapApp.kt           # Application, canal de notification
├── data/
│   ├── Prefs.kt                 # Configuration chiffrée (PIN, SMTP, apps protégées)
│   ├── SecurityUtils.kt         # Hachage du PIN (SHA-256 + sel)
│   └── AttemptLogStore.kt       # Journal local des tentatives
├── ui/
│   ├── SplashActivity.kt        # Routage initial
│   ├── ConsentActivity.kt       # Écran de consentement (bloquant)
│   ├── SetupPinActivity.kt      # Création/modification du PIN
│   ├── LockActivity.kt          # Écran de verrouillage + logique de tentatives
│   ├── SettingsActivity.kt      # Tableau de bord
│   ├── ProtectedApps*.kt        # Sélection des apps protégées
│   └── Log*.kt                  # Journal des tentatives
├── lock/AppLockAccessibilityService.kt  # Détection d'app au premier plan
├── capture/
│   ├── CameraCapture.kt         # Capture silencieuse (CameraX)
│   └── IntruderCaptureService.kt# Service de premier plan : capture + envoi
├── mail/EmailSender.kt          # Envoi SMTP (JavaMail)
└── boot/BootReceiver.kt
```

## Remarque sur la vérification

Ce projet a été écrit et relu dans un environnement sans SDK Android ni accès réseau à Maven/Google (donc sans compilation Gradle possible ici). Les références entre fichiers (ids de vues, chaînes, classes déclarées dans le manifeste) ont été vérifiées une à une par recoupement automatique. Une vérification par compilation réelle dans Android Studio reste recommandée avant toute mise en production.
