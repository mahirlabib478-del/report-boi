# রিপোর্ট বই — Android Offline App

বাংলা মাসিক পরিকল্পনা, দৈনিক কাজের হিসাব এবং মাসিক রিপোর্ট তৈরির Android অ্যাপ।

## অফলাইন নীতি
- কোনো server বা cloud database নেই।
- কোনো account/login নেই।
- তথ্য অ্যাপের private storage-এ একটি JSON ফাইলে রাখা হয়।
- PDF তৈরি, প্রিন্ট এবং JSON backup/restore করা যায়।
- একই ফোনে একাধিক সদস্যের আলাদা প্রোফাইল রাখা যায়।
- অন্য ফোনে automatic sync নেই; JSON backup export করে import করতে হবে।
- ভূমিকা কেবল স্থানীয় লেবেল, login বা access-control নয়। ফোনটি ব্যবহার করতে পারলে অন্য কেউ স্থানীয় তথ্য দেখতে পারে।

## চালানোর নিয়ম
প্রয়োজন: Node.js LTS, Android ফোন, এবং development/testing-এর জন্য Expo Go।

1. এই android-app ফোল্ডারটি কম্পিউটারে clone বা download করুন।
2. টার্মিনালে android-app ফোল্ডারে প্রবেশ করুন।
3. npm install চালান।
4. npx expo start চালান।
5. Android ফোন ও কম্পিউটার একই Wi-Fi-তে রেখে Expo Go দিয়ে QR code scan করুন।

## APK তৈরি
Expo Go শুধু development/testing-এর জন্য। APK বানাতে Expo account ও EAS Build প্রয়োজন:
1. npm install -g eas-cli
2. eas login
3. eas build:configure
4. eas build -p android --profile preview

APK তৈরি হওয়ার পরে দৈনিক হিসাব offline-এ চলবে।

## Backup
সদস্য ট্যাব থেকে JSON backup share করুন। অন্য ফোনে অ্যাপ খুলে JSON ব্যাকআপ আমদানি নির্বাচন করুন। Import করলে ওই ফোনের আগের তথ্য প্রতিস্থাপিত হতে পারে; আগে backup রাখুন।

## প্রকাশের আগে
মূল কাগজের ফরমে কিছু সংক্ষিপ্ত শব্দ অস্পষ্ট। দায়িত্বশীলের সঙ্গে field names, units, monthly totals এবং PDF layout মিলিয়ে চূড়ান্ত করুন।
