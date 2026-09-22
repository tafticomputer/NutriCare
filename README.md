# NutriCare — اپلیکیشن اندروید مشاوره پزشک و متخصص تغذیه

نقش‌ها: بیمار، پزشک/متخصص تغذیه، مدیر
فناوری: Kotlin + Jetpack Compose + Firebase (Auth + Firestore)

## راه‌اندازی

1. پروژه را در Android Studio (نسخه Koala یا جدیدتر) باز کنید و اجازه دهید Gradle Sync انجام شود.
2. در https://console.firebase.google.com یک پروژه بسازید و یک اپ Android با package name
   `com.example.nutricare` اضافه کنید. فایل `google-services.json` را دانلود و در پوشه‌ی `app/` بگذارید.
3. در Firebase: Authentication ← Sign-in method ← «Email/Password» را فعال کنید.
4. در Firebase: Firestore Database را بسازید (Production mode) و محتوای فایل `firestore.rules`
   را در تب Rules قرار دهید و Publish کنید.
5. اپ را اجرا کنید (Run) یا از Build ← Build APK فایل نصبی بسازید.

## ساخت اولین مدیر

1. در اپ با یک ایمیل ثبت‌نام کنید (حساب به‌صورت «بیمار» ساخته می‌شود).
2. در Firebase Console ← Firestore ← مجموعه‌ی `users` ← سند همان کاربر، فیلد `role` را به `ADMIN` تغییر دهید.
3. اپ را ببندید و دوباره وارد شوید؛ پنل مدیریت باز می‌شود.

## ساخت پزشک

1. پزشک با ایمیل خودش در اپ ثبت‌نام می‌کند.
2. مدیر در پنل مدیریت ← تب «کاربران» ← «تبدیل به پزشک» را می‌زند و تخصص را وارد می‌کند.
3. پزشک از این پس پس از ورود، کارتابل خود را می‌بیند؛ و بیماران او را در لیست پزشکان می‌بینند.

## جریان کار

بیمار پزشک را انتخاب می‌کند ← درخواست در کارتابل پزشک ظاهر می‌شود ← پزشک می‌پذیرد یا رد می‌کند
← در صورت پذیرش، چت بین بیمار و پزشک فعال می‌شود ← پزشک مشاوره را تکمیل می‌کند.

## اعلان‌ها (Push Notification)

اعلان‌ها با FCM و Cloud Functions ارسال می‌شوند:
- ثبت درخواست جدید توسط بیمار ← اعلان به پزشک
- پذیرش / رد / تکمیل درخواست ← اعلان به بیمار
- پیام جدید در چت ← اعلان به طرف مقابل (بدون نمایش متن پیام)

### استقرار Cloud Functions

1. Node.js نسخه ۲۰ و Firebase CLI را نصب کنید: `npm install -g firebase-tools`
2. `firebase login` و سپس در ریشه‌ی پروژه `firebase use --add` (پروژه‌ی خود را انتخاب کنید).
3. پروژه‌ی Firebase باید روی طرح **Blaze** (پرداخت به‌ازای مصرف) باشد؛ Cloud Functions روی طرح رایگان کار نمی‌کند.
4. در `functions/index.js` مقدار `REGION` را با محل دیتابیس Firestore هماهنگ کنید.
5. `cd functions && npm install && cd ..`
6. `firebase deploy --only functions,firestore:rules`

### نکات
- روی اندروید ۱۳ به بالا، اپ هنگام اولین اجرا اجازه‌ی نمایش اعلان را می‌پرسد.
- FCM به سرویس‌های گوگل (Google Play Services) روی گوشی کاربر نیاز دارد.

## GitHub Actions

دو workflow در پوشه‌ی `.github/workflows/` وجود دارد:

- `android.yml`: با هر push به شاخه‌ی main و هر Pull Request، اپ را Build می‌کند و فایل APK را به‌عنوان Artifact ذخیره می‌کند (Actions ← اجرای مربوطه ← Artifacts).
- `firebase-deploy.yml`: با تغییر `functions/` یا `firestore.rules` در main، قوانین Firestore و Cloud Functions را روی Firebase منتشر می‌کند.

### Secrets لازم (Settings ← Secrets and variables ← Actions)

| نام | مقدار |
|---|---|
| `GOOGLE_SERVICES_JSON` | محتوای `google-services.json` به‌صورت Base64 |
| `FIREBASE_SERVICE_ACCOUNT` | محتوای JSON کلید Service Account (فقط برای deploy) |
| `FIREBASE_PROJECT_ID` | شناسه‌ی پروژه‌ی Firebase (فقط برای deploy) |

ساخت Base64 برای `google-services.json`:
- Linux: `base64 -w0 app/google-services.json`
- macOS: `base64 -i app/google-services.json | tr -d '\n'`
- Windows PowerShell: `[Convert]::ToBase64String([IO.File]::ReadAllBytes("app\google-services.json"))`

فایل `google-services.json` در `.gitignore` قرار دارد و نباید در گیت‌هاب commit شود.

## انتشار نسخه‌ی امضاشده (Release)

فایل `.github/workflows/release.yml` با ساخت تگ جدید (مثلاً `v1.0.0`) یک APK امضاشده و یک AAB می‌سازد
و آن‌ها را به یک GitHub Release پیوست می‌کند.

### ساخت Keystore (فقط یک‌بار)

```
keytool -genkeypair -v -keystore nutricare-release.jks -alias nutricare -keyalg RSA -keysize 2048 -validity 10000
```

⚠️ فایل `.jks` و رمزها را جای امن (خارج از گیت) نگه‌داری و از آن‌ها نسخه‌ی پشتیبان بگیرید.
با گم شدن آن، امکان به‌روزرسانی اپ منتشرشده با همان امضا وجود ندارد.

### Secrets اضافه برای release

| نام | مقدار |
|---|---|
| `KEYSTORE_BASE64` | فایل `.jks` به‌صورت Base64 |
| `KEYSTORE_PASSWORD` | رمز Keystore |
| `KEY_ALIAS` | نام alias (مثلاً `nutricare`) |
| `KEY_PASSWORD` | رمز کلید |

(به‌علاوه‌ی `GOOGLE_SERVICES_JSON` که قبلاً تعریف شد.)

### انتشار

```
git tag v1.0.0
git push origin v1.0.0
```

پیش از انتشار عمومی، `applicationId` را در `app/build.gradle.kts` از `com.example.nutricare`
به شناسه‌ی اختصاصی خودتان تغییر دهید (و در Firebase هم همان package name را ثبت کنید).
