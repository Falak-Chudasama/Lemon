# Coconut Snap Production ProGuard / R8 Rules

# Room Database
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-dontwarn androidx.room.paging.**

# Android Components & Receivers
-keep class com.coconut.CoconutApplication { *; }
-keep class com.coconut.MainActivity { *; }
-keep class com.coconut.policy.CoconutDeviceAdminReceiver { *; }
-keep class com.coconut.policy.PackageReconcileReceiver { *; }
-keep class com.coconut.worker.ReconciliationWorker { *; }

# Data Models
-keep class com.coconut.data.** { *; }
-keep class com.coconut.policy.PolicyResult { *; }
-keep class com.coconut.policy.PolicyCapabilities { *; }
-keep class com.coconut.dns.DnsTestResult { *; }

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
