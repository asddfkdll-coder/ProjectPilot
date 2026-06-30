# ProjectPilot v1.1 🚀

تطبيق Android احترافي لمتابعة مشاريع البرمجة وتشغيل سيرفراتها مباشرة عبر **Termux**.

## ✨ الميزات
- **اكتشاف ذكي** لـ 10 أنواع مشاريع (Node, Python, PHP, Java/Spring, Go, Rust, Docker, .NET, Ruby, HTML).
- **جدول تشغيل** لكل مشروع: الأمر، البورت، المتطلبات.
- **تكامل Termux** عبر `com.termux.RUN_COMMAND` لتشغيل الأوامر بضغطة.
- **Git tracking**: branch / آخر commit / المؤلف / عدد التغييرات / remote — قراءة مباشرة من `.git` بدون JGit.
- **Recipes**: مكتبة أوامر جاهزة لكل نوع مشروع (Install Node, Django migrate, Compose up...).
- **Auto-backup** كل 24 ساعة عبر WorkManager، مع زر "Backup now" ومشاركة الملفات.
- **`.env` مشفّر** بـ AES-256-GCM (Android Keystore).
- Foreground Service لمراقبة السيرفرات + إشعارات.
- بنية Clean Architecture + MVVM، Kotlin 2.0، Jetpack Compose، Material 3.

## 📱 المتطلبات على الهاتف
1. Android 8.0+ (API 26+).
2. **Termux من F-Droid** (ليس Google Play — النسخة هناك قديمة): https://f-droid.org/packages/com.termux/
3. في Termux:
   ```bash
   echo 'allow-external-apps = true' >> ~/.termux/termux.properties
   termux-reload-settings
   pkg update && pkg upgrade
   ```

## 🛠️ طرق البناء (اختر واحدة)

### الطريقة 1 — Android Studio (موصى بها)
1. افتح المجلد بـ Android Studio Hedgehog/Iguana 2024+.
2. Sync Gradle.
3. Run.

### الطريقة 2 — البناء على الهاتف نفسه عبر Termux
الـ sandbox السحابي محدود الذاكرة، لكن هاتفك الحديث قادر:
```bash
# داخل Termux، بعد فك الزيب:
cd ProjectPilot
./build_on_phone.sh
# يثبّت JDK + SDK + يبني، ثم يطبع المسار:
# app/build/outputs/apk/debug/app-debug.apk
```

### الطريقة 3 — سطر الأوامر على PC
```bash
./gradlew :app:assembleDebug
# APK في: app/build/outputs/apk/debug/app-debug.apk
```

## 🧱 بنية المشروع
```
ProjectPilot/
├── app/src/main/java/com/projectpilot/app/
│   ├── ProjectPilotApp.kt        (Application + WorkManager Hilt config)
│   ├── di/AppModule.kt
│   ├── data/
│   │   ├── local/                (Room: DAO + Database)
│   │   ├── repository/           (ProjectRepository)
│   │   ├── scanner/              (10 Detectors + ProjectScanner)
│   │   ├── git/GitInfoReader.kt  (← جديد)
│   │   ├── backup/BackupWorker.kt(← جديد)
│   │   └── recipes/CommandRecipes(← جديد)
│   ├── domain/model/             (Project, DetectionResult, ProjectType)
│   ├── ui/
│   │   ├── MainActivity.kt + Theme
│   │   └── screens/
│   │       ├── home/             (HomeScreen + ViewModel)
│   │       ├── add/              (AddProjectScreen)
│   │       ├── detail/           (ProjectDetailScreen)
│   │       ├── git/              (GitScreen ← جديد)
│   │       ├── recipes/          (RecipesScreen ← جديد)
│   │       └── settings/         (SettingsScreen — مع toggle Auto-backup)
│   ├── service/                  (ServerMonitorService + BootReceiver)
│   ├── termux/                   (TermuxCommandRunner)
│   └── security/                 (EncryptionManager AES-256)
```

## 🔐 الأمان
- لا توجد أسرار مضمنة. كل مفاتيح التشفير من Android Keystore.
- التحقق الصارم من المسارات (Path-traversal prevention).
- تمرير الأوامر كـ array وليس string لمنع Shell Injection.
- `.env` مشفّر داخل EncryptedSharedPreferences.
- `networkSecurityConfig` يمنع cleartext (ما عدا localhost).
- ProGuard/R8 يحذف الـ Logs في Release.
- استثناء الملف الحساس من النسخ الاحتياطي السحابي.
- FileProvider لمشاركة النسخ الاحتياطية بدون كشف المسارات.

## 🔭 خارطة الطريق
- [ ] دعم iOS عبر Kotlin Multiplatform.
- [ ] نسخة Desktop (Compose for Desktop).
- [ ] مزامنة سحابية مشفّرة طرف-إلى-طرف.
- [ ] اقتراحات أوامر بالذكاء الاصطناعي.

## 📜 الرخصة
MIT.
