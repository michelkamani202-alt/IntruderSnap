# Règles Proguard/R8 — IntruderSnap

# JavaMail (com.sun.mail) utilise de la réflexion pour charger ses providers.
-keep class com.sun.mail.** { *; }
-keep class javax.mail.** { *; }
-keep class javax.activation.** { *; }
-dontwarn com.sun.mail.**
-dontwarn javax.activation.**

# CameraX
-keep class androidx.camera.** { *; }
