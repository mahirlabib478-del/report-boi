# রিপোর্ট বই

বাংলা মাসিক পরিকল্পনা, দৈনিক কাজের হিসাব এবং মাসিক রিপোর্ট তৈরির প্রজেক্ট।

## Android offline app

Android অ্যাপের সোর্স কোড আছে [android-app ফোল্ডারে](android-app/README.md)।

- কোনো server বা cloud database নেই।
- তথ্য ফোনের private storage-এ একটি JSON ফাইলে থাকে।
- একই ফোনে একাধিক সদস্যের হিসাব রাখা যায়।
- মাসিক রিপোর্ট দেখা, PDF তৈরি, প্রিন্ট এবং JSON backup/restore করা যায়।
- একাধিক ফোনের মধ্যে automatic sync নেই।

## Development

Android অ্যাপের জন্য android-app ফোল্ডারের README অনুসরণ করুন। এটি Expo + React Native দিয়ে তৈরি।
