# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Preserve line numbers for debugging crash stack traces
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# ============================================================================
# NAVIGATION - Keep Screen sealed class and data objects
# ============================================================================
-keep class com.stockapp.nav.Screen { *; }
-keep class com.stockapp.nav.Screen$* { *; }
-keep class com.stockapp.nav.NavArgs { *; }
-keep class com.stockapp.nav.DeepLinkScheme { *; }

# ============================================================================
# KOTLIN SERIALIZATION
# ============================================================================
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# Keep all @Serializable classes and their serializers
-keep @kotlinx.serialization.Serializable class * { *; }
-keep class **$$serializer { *; }

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep domain models for serialization
-keep,includedescriptorclasses class com.stockapp.**$$serializer { *; }
-keepclassmembers class com.stockapp.** {
    *** Companion;
}
-keepclasseswithmembers class com.stockapp.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep all API/DTO/Model classes for serialization
-keep class com.stockapp.core.api.** { *; }
-keep class com.stockapp.core.backup.** { *; }
-keep class com.stockapp.core.py.** { *; }
-keep class com.stockapp.feature.search.domain.model.** { *; }
-keep class com.stockapp.feature.analysis.domain.model.** { *; }
-keep class com.stockapp.feature.indicator.domain.model.** { *; }
-keep class com.stockapp.feature.financial.domain.model.** { *; }
-keep class com.stockapp.feature.financial.data.dto.** { *; }
-keep class com.stockapp.feature.ranking.domain.model.** { *; }
-keep class com.stockapp.feature.ranking.data.dto.** { *; }
-keep class com.stockapp.feature.etf.domain.model.** { *; }
-keep class com.stockapp.feature.etf.data.dto.** { *; }
-keep class com.stockapp.feature.scheduling.domain.model.** { *; }
-keep class com.stockapp.feature.settings.domain.model.** { *; }

# ============================================================================
# ROOM DATABASE
# ============================================================================
-keep class com.stockapp.core.db.** { *; }
-keep class com.stockapp.core.db.entity.** { *; }
-keep class com.stockapp.core.db.dao.** { *; }

# ============================================================================
# HILT DEPENDENCY INJECTION
# ============================================================================
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ComponentSupplier { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# Keep Hilt generated classes
-keep class **_HiltModules* { *; }
-keep class **_Factory { *; }
-keep class **_MembersInjector { *; }

# Keep ViewModels annotated with @HiltViewModel
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }

# Keep all DI modules
-keep class com.stockapp.core.di.** { *; }
-keep class com.stockapp.feature.*.di.** { *; }

# ============================================================================
# CHAQUOPY (Python integration)
# ============================================================================
-keep class com.chaquo.python.** { *; }

# ============================================================================
# OKHTTP
# ============================================================================
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-keep class okio.** { *; }

# ============================================================================
# COMPOSE & LIFECYCLE
# ============================================================================
-keep class androidx.compose.** { *; }
-keep class androidx.lifecycle.** { *; }

# Keep Compose runtime classes
-dontwarn androidx.compose.**

# ============================================================================
# WORKMANAGER
# ============================================================================
-keep class * extends androidx.work.Worker { *; }
-keep class * extends androidx.work.ListenableWorker { *; }
-keep class androidx.work.** { *; }

# Keep HiltWorker annotated classes
-keep @androidx.hilt.work.HiltWorker class * { *; }
-keep class com.stockapp.feature.scheduling.worker.** { *; }
-keep class com.stockapp.feature.etf.worker.** { *; }

# ============================================================================
# CORE STATE & MANAGERS
# ============================================================================
-keep class com.stockapp.core.state.** { *; }
-keep class com.stockapp.core.cache.** { *; }
-keep class com.stockapp.core.theme.** { *; }
-keep class com.stockapp.core.network.** { *; }

# ============================================================================
# GOOGLE ERROR PRONE ANNOTATIONS
# ============================================================================
-dontwarn com.google.errorprone.annotations.CanIgnoreReturnValue
-dontwarn com.google.errorprone.annotations.CheckReturnValue
-dontwarn com.google.errorprone.annotations.Immutable
-dontwarn com.google.errorprone.annotations.RestrictedApi
-dontwarn com.google.errorprone.annotations.**

# ============================================================================
# MISCELLANEOUS
# ============================================================================
# Keep R class
-keepclassmembers class **.R$* {
    public static <fields>;
}

# Keep enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep Parcelable
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}
