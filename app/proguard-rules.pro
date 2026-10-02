# Keep data models used by Firestore / Gson / Retrofit / Kotlinx Serialization / Room
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod

# Keep all data model classes and their members (constructors, fields, getters/setters)
-keep class com.focuspath.shared.model.** { *; }
-keepclassmembers class com.focuspath.shared.model.** { *; }

-keep class com.focuspath.app.data.model.** { *; }
-keepclassmembers class com.focuspath.app.data.model.** { *; }

-keep class com.focuspath.app.data.remote.** { *; }
-keepclassmembers class com.focuspath.app.data.remote.** { *; }

-keep class com.focuspath.app.core.data.local.** { *; }
-keepclassmembers class com.focuspath.app.core.data.local.** { *; }

-keep class com.focuspath.app.core.domain.model.** { *; }
-keepclassmembers class com.focuspath.app.core.domain.model.** { *; }

# Keep Firestore PropertyName annotations and default constructors
-keepclassmembers class * {
    @com.google.firebase.firestore.PropertyName <fields>;
    @com.google.firebase.firestore.PropertyName <methods>;
    public <init>();
}

# Keep Kotlin @Keep annotated classes
-keep @androidx.annotation.Keep class * { *; }
-keepclassmembers class * {
    @androidx.annotation.Keep *;
}
