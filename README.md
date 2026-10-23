# রিপোর্ট বই — Native Android (Kotlin)

একজন ব্যবহারকারীর জন্য সম্পূর্ণ অফলাইন Android অ্যাপ। এটি Expo/React Native নয়; Native Android, Kotlin ও Jetpack Compose (Material 3) ব্যবহার করে তৈরি।

## ফিচার
- বাংলা UI: হোম, মাসিক পরিকল্পনা, দৈনিক হিসাব, মাসিক রিপোর্ট
- তথ্য Android-এর private SharedPreferences-এ JSON আকারে থাকে
- কোনো server, cloud database, login, internet permission বা multi-member feature নেই
- মাসিক পরিকল্পনা ও দৈনিক রেকর্ড
- মাসিক totals, PDF সংরক্ষণ, share ও print
- Android cloud auto-backup বন্ধ

## Android Studio দিয়ে চালানো
1. Android Studio-তে repository root খুলুন (যেখানে settings.gradle.kts আছে)।
2. Android SDK Platform 35 এবং JDK 17 ইনস্টল আছে নিশ্চিত করুন।
3. Gradle Sync শেষ হলে Run ▶ চাপুন এবং emulator বা USB debugging চালু করা ফোন বেছে নিন।
4. প্রথম build-এর সময় dependencies ডাউনলোড করতে ইন্টারনেট লাগবে। ইনস্টল হওয়ার পরে অ্যাপের ফর্ম ও হিসাব অফলাইনে চলে।

## APK তৈরি
Android Studio: Build → Build Bundle(s) / APK(s) → Build APK(s)। APK সাধারণত app/build/outputs/apk/debug/app-debug.apk-এ পাওয়া যায়।

## ডেটা
ডেটা শুধু অ্যাপের private storage-এ রাখা হয়। অ্যাপ uninstall বা app data clear করলে তথ্য মুছে যাবে। প্রয়োজনীয় রিপোর্ট PDF হিসেবে সংরক্ষণ করে রাখুন।

## গুরুত্বপূর্ণ
কাগজের ফর্মের কয়েকটি সংক্ষিপ্ত নাম/একক প্রকাশের আগে মূল ফর্মের সঙ্গে যাচাই করুন। সোর্স কোড যোগ করা হয়েছে; বাস্তব ডিভাইসে build ও QA এখনও করতে হবে।
