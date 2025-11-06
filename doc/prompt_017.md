# Role
You are an expert Android Kotlin developer integrating Firebase Crashlytics 
for real-time crash reporting, non-fatal error logging, and custom user event tracking.

# Context
The goal is to integrate Firebase Crashlytics to automatically collect crash reports 
and allow developers to record custom logs and non-fatal exceptions.

Requirements:
1. Configure Firebase Crashlytics via Gradle (KTS) with all necessary plugins.
2. Initialize Firebase in Application class using Kotlin.
3. Implement a lightweight AppLogger utility to send custom logs and handled exceptions.
4. Demonstrate sample usage in an Activity.
5. Ensure symbol mapping files (mapping.txt) are automatically uploaded on build.
6. The implementation should be production-ready and follow Kotlin best practices.

# Output Format
Generate complete code snippets and configuration files including:

1. **project-level build.gradle.kts**
   - Add Google Services and Crashlytics Gradle plugins

2. **app-level build.gradle.kts**
   - Apply plugins, dependencies, and Firebase initialization
   - Enable automatic mapping upload

3. **MassagerApplication.kt**
   - Initialize Firebase and Crashlytics
   - Enable/disable collection via BuildConfig

4. **AppLogger.kt**
   - Kotlin object class that wraps FirebaseCrashlytics
   - Methods: info(), warn(), error(), recordException()

5. **ExampleActivity.kt**
   - Show how to log custom events and simulate handled/unhandled crashes

6. **google-services.json placeholder**
   - Mention where to download and place it

7. **Instructions**
   - How to test integration (e.g., force crash, view report in Firebase Console)
   - How to link BigQuery for visualization (optional)

# Style
- Use modern Kotlin syntax (no Java interop)
- Proper package naming (e.g. com.example.app)
- Clean, concise, with comments
- Follow Android best practices

# Example Expected Output
- Gradle configs with correct plugins and versions
- Kotlin classes for Application and Logger
- Activity with sample Crashlytics usage
- Step-by-step setup verification guide
