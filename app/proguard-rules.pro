-keep class **$** extends androidx.work.ListenableWorker { *; }
-keep class ** extends androidx.work.ListenableWorker { *; }

-keep class com.google.firebase.** { *; }
-keep class com.google.firebase.**$** { *; }

-keep class androidx.work.** { *; }
-keep class androidx.work.**$** { *; }
-dontwarn androidx.work.**