# Keep annotations (needed by Room, Compose, etc.)
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# ----- Brutus app data classes (Room entities, etc.) -----
-keep class com.pepperonas.brutus.data.** { *; }

# ----- Room -----
# AGP's default-optimize file already covers Room, but we keep entities + DAOs explicitly.
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *
-dontwarn androidx.room.paging.**

# ----- Kotlinx Coroutines -----
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }
-dontwarn kotlinx.coroutines.debug.**

# ----- Jetpack Compose -----
# R8 Compose-mode handles most of this automatically with AGP 8+, but a few rules
# are still needed for runtime reflection in Compose tooling and previews.
-keep class androidx.compose.runtime.** { *; }
-keepclassmembers class androidx.compose.runtime.** { *; }
-dontwarn androidx.compose.**

# ----- CameraX -----
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**

# ----- ML Kit Barcode (works for both bundled and unbundled variants) -----
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.internal.mlkit_** { *; }
-dontwarn com.google.mlkit.**
-dontwarn com.google.android.gms.internal.mlkit_**

# ----- ZXing -----
-keep class com.google.zxing.** { *; }
-dontwarn com.google.zxing.**

# ----- DataStore -----
-keep class androidx.datastore.*.** { *; }
-dontwarn androidx.datastore.**

# ----- Navigation Compose -----
-dontwarn androidx.navigation.**

# ----- AlarmManager / PendingIntent (uses class names via Intent) -----
-keep class com.pepperonas.brutus.receiver.AlarmReceiver { *; }
-keep class com.pepperonas.brutus.receiver.BootReceiver { *; }
-keep class com.pepperonas.brutus.service.AlarmService { *; }
-keep class com.pepperonas.brutus.AlarmActivity { *; }
-keep class com.pepperonas.brutus.MainActivity { *; }
-keep class com.pepperonas.brutus.TestAlarmActivity { *; }

# Strip Log.* in release for size + tiny perf gain
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}
