# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# osmdroid
-dontwarn org.osmdroid.**
-keep class org.osmdroid.** { *; }

# Google Fonts
-keep class androidx.compose.ui.text.googlefonts.** { *; }
