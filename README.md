# YazedTeacherPro

تطبيق Android لإدارة المدرس والسنتر، مبني بـ Kotlin وJetpack Compose ويخزن البيانات محليًا في SQLite.

## الوظائف الحالية

- تسجيل الدخول الافتراضي: `1` / `000000`.
- تجربة 15 يوم ونظام تفعيل يعتمد على Android Hardware ID.
- لوحة تحكم عربية RTL.
- إدارة الطلبة والسناتر والمجموعات.
- الحضور والغياب.
- المدفوعات والمتأخرات والإيصالات.
- المصروفات.
- الامتحانات والدرجات والترتيب.
- طابور رسائل WhatsApp.
- التقارير وتصدير CSV/TXT.
- النسخ الاحتياطي والاسترجاع.
- الإعدادات وسجل العمليات.

## بناء APK من GitHub فقط

أي Push إلى فرع `main` يشغّل GitHub Actions تلقائيًا، ويبني ملف:

`YazedTeacherPro.apk`

ثم ينشره في Release ثابت باسم `latest`، لذلك يمكن تحميل أحدث نسخة مباشرة من صفحة Releases بدون Android Studio أو بناء على الكمبيوتر.

## التقنيات

- Kotlin 2.3.21
- Jetpack Compose / Material 3
- Android Gradle Plugin 8.13.2
- Gradle 8.13
- JDK 17
- compileSdk / targetSdk 36
- SQLiteOpenHelper

## Package

`com.sarhansoftware.yazedteacherpro`
