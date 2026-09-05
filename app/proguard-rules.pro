# Credential Manager and the Google ID helper read credential payloads
# reflectively, so their model types must survive shrinking.
-keep class com.google.android.libraries.identity.googleid.** { *; }
-keep class androidx.credentials.** { *; }

# Firebase Auth model classes are deserialised reflectively.
-keep class com.google.firebase.auth.** { *; }

# Keep the annotations R8 needs to reason about Kotlin metadata.
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations
-keepattributes Signature,InnerClasses,EnclosingMethod
