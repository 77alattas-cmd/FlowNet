# FlowNet 🌐

تطبيق أندرويد متكامل لأدوات الشبكة، خادم ملفات محلي، وبروكسي HTTP، مبني بـ **Kotlin** و **Jetpack Compose**.

![Kotlin](https://img.shields.io/badge/Kotlin-1.9+-blueviolet)
![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material3-green)
![minSdk](https://img.shields.io/badge/minSdk-26-orange)
![License](https://img.shields.io/badge/license-MIT-lightgrey)

---

## ✨ المميزات

- 🖥️ **خادم HTTP محلي** لمشاركة الملفات عبر Wi-Fi
- 🔒 **مصادقة Basic Auth** مع تخزين مشفّر (`EncryptedSharedPreferences`)
- 🌐 **بروكسي محلي** لتحليل حركة المرور وتوجيه الحزم
- 📊 **سجلّات الشبكة** (Captive Portal / Network Logs) مع حفظ محلي باستخدام Room
- 🧰 **أدوات شبكة**: فحص DNS، ping، معلومات الاتصال، فحص المنافذ
- 📷 **مسح وتوليد QR** لمشاركة رابط الخادم وإعدادات الاتصال بسرعة
- 🎨 **Material 3** مع دعم الألوان والتصميم العصري (Glassmorphism & Adaptive Layouts)

---

## 🏗️ البنية المعمارية (Architecture)

يعتمد التطبيق على معمارية **Clean Architecture + MVVM** وحقن التبعيات بـ **Dagger Hilt**:

```text
app/src/main/java/com/fn/has/code/
├── core/                       # النواة والمرافق المشتركة
│   ├── constants/              # الثوابت العامة للتطبيق
│   ├── security/               # إدارة التشفير وتوليد كلمات المرور (SecureCredentialsStore)
│   └── utils/                  # أدوات الشبكة، فحص الاتصال، وإدارة الطاقة
├── data/                       # طبقة البيانات (Data Layer)
│   └── local/db/
│       ├── dao/                # واجهات الوصول لقاعدة البيانات (FlowNetDao, BlockedDomainDao)
│       ├── entity/             # كائنات الجداول (CaptiveLog, NetworkLog, BlockedDomain)
│       └── FlowNetDatabase.kt  # قاعدة بيانات Room المحلية
├── di/                         # وحدات حقن التبعيات (Dependency Injection)
│   └── DatabaseModule.kt       # تزويد قاعدة البيانات و DAOs عبر Hilt
├── engine/                     # محركات الشبكة والخدمات المنخفضة المستوى
│   ├── dns/                    # محرك تحليل وحظر نطاقات الـ DNS
│   └── proxy/                  # خوارزميات معالجة حزم البروكسي
├── server/                     # خوادم الاتصال المحلية (Servers)
│   ├── HttpFileServer.kt       # خادم ملفات HTTP لمشاركة الوسائط والمستندات
│   └── LocalProxyServer.kt     # خادم البروكسي المحلي مع دعم المصادقة الآمنة
├── service/                    # خدمات الخلفية (Android Services)
│   ├── FlowNetCoreService.kt   # الخدمة الأساسية لإدارة تشغيل الخوادم بالخلفية
│   └── FlowNetVpnService.kt    # خدمة الـ VPN لتوجيه الحزم
└── ui/                         # واجهة المستخدم (Jetpack Compose UI)
    ├── components/             # المكونات الرسومية القابلة لإعادة الاستخدام (Cards, Scanners)
    ├── main/                   # شاشات العرض وإدارة الحالة (MainActivity, MainViewModel, Dashboard)
    └── theme/                  # ثيمات الألوان، الخطوط ونظام Material 3
```

---

## 🚀 التقنيات المستخدمة (Tech Stack)

| التقنية | الاستخدام |
|---|---|
| **Kotlin & Coroutines** | لغة البرمجة الرئيسية ومعالجة المهام غير المتزامنة وتدفقات البيانات (Flows) |
| **Jetpack Compose** | بناء واجهات المستخدم التفاعلية وفق معايير Material 3 |
| **Dagger Hilt** | إدارة وحقن التبعيات (Dependency Injection) عبر كامل طبقات التطبيق |
| **Room Database** | تخزين السجلات وحفظ النطاقات المحظورة محلياً |
| **AndroidX Security Crypto** | تشفير وحفظ بيانات الاعتماد والمفاتيح السرية بأمان |
| **CameraX & ZXing** | قراءة وتوليد رموز الاستجابة السريعة (QR Codes) |

---

## 🛠️ التشغيل والبناء (Build & Run)

1. استنساخ المشروع:
   ```bash
   git clone <repo_url>
   ```
2. فتح المشروع باستخدام **Android Studio Ladybug (أو أحدث)**.
3. التأكد من تثبيت **JDK 17**.
4. تشغيل الاختبارات:
   ```bash
   ./gradlew testDebugUnitTest
   ```
5. بناء وتشغيل التطبيق على الجهاز أو المحاكي عبر زر **Run** (أو `./gradlew assembleDebug`).
