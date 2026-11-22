# Angry Birds

[English](README.md) | **فارسی**

<div dir="rtl">

یک بازی فیزیکی به سبک Angry Birds برای اندروید که با Kotlin، libGDX و Box2D ساخته شده است.
پرنده را به عقب بکشید (یک کمان نقطه‌چین کوتاه مسیر پرتاب را نشان می‌دهد)، رها کنید و سازه‌ها را روی سر خوک‌ها خراب کنید.
امروز یک مرحله‌ی دست‌ساز قابل بازی است، داخل یک برج مراحل که برای مراحل بیشتر آماده است.

صورت خوک‌ها یک تصویر سفارشی است: فقط فایل `enemy_head.png` را عوض کنید و صورت تغییر می‌کند، بدون هیچ تغییری در کد.

ساخت خصوصی، منتشر نشده است.

## گیم‌پلی

- **۵ پرنده در هر تلاش**، پشت سر هم و به ترتیبی که مرحله مشخص می‌کند:
  آبی، زرد، آبی، قرمز، قرمز (لیست `birds` در `Levels.kt` که با `tools/generate_level.py` ساخته می‌شود).
- **۵ خوک** (بدنه‌ی دایره‌ای با HP) داخل برج‌های شیشه‌ای، یک سازه با جعبه‌های TNT و یک قفس سنگی روی پایه‌های چوبی بالای
  یک صخره‌ی بلند پنهان شده‌اند. چیدمان مرحله مطابق اسکرین‌شات مرجع است (۱۰۰ پیکسل = ۱ متر) و قطعات زمین بدنه‌ی استاتیک واقعی هستند.
- پرنده را از فلاخن به عقب بکشید (حداکثر ۱.۹ متر)، رها کنید تا شلیک شود. سرعت پرتاب به طول کشش بستگی دارد.
- **پرنده‌ها و توانایی‌ها:** قرمز (ساده)، زرد (هنگام پرواز لمس کنید تا شتاب بگیرد)، آبی (هنگام پرواز لمس کنید تا به سه تکه
  تقسیم شود). توانایی‌ها فقط تا زمانی کار می‌کنند که پرنده به چیزی برخورد نکرده باشد. رد پرواز محل استفاده از توانایی را با
  یک نشان کوچک مشخص می‌کند و بعد از تقسیم، هر سه مسیر کشیده می‌شود.
- **آسیب** از ضربه‌ی برخورد محاسبه می‌شود. بلوک‌ها و خوک‌ها HP از دست می‌دهند و در HP صفر حذف می‌شوند.
  TNT منفجر می‌شود: بلوک‌های نزدیک به انفجار خرد می‌شوند و قطعات دورتر پرتاب می‌شوند.
- **برد:** همه‌ی خوک‌ها نابود شوند. بعد از آن مرحله وقتی تمام می‌شود که امتیاز حدود ۲.۴ ثانیه تغییر نکرده باشد.
  **باخت:** پرنده‌ای نمانده و همه‌چیز ۲ ثانیه ساکن بوده باشد.
- **امتیاز:** برای هر خوک و بلوک نابودشده امتیاز داده می‌شود (یک عدد کوچک «+امتیاز» کنار هرکدام ظاهر می‌شود)
  و برای هر پرنده‌ی استفاده‌نشده ۱۰٬۰۰۰ امتیاز.
- **ستاره‌ها:** پاک کردن مرحله = ۱ ستاره، ۳۵٬۰۰۰ امتیاز = ۲ ستاره، ۴۸٬۰۰۰ امتیاز = ۳ ستاره (نگه داشتن پرنده‌ها ستاره می‌آورد).
  بهترین امتیاز و بهترین ستاره‌ها روی دستگاه ذخیره می‌شوند.
- مسیر آخرین شلیک به شکل یک ردِ نقطه‌چین سفید روی صفحه می‌ماند و وقتی پرنده‌ی بعدی از فلاخن جدا می‌شود پاک می‌شود.

### HUD (بالا سمت راست هنگام بازی)

- **امتیاز** با ارقام درشت طلایی؛ به جای پریدن، شمارش می‌کند و وقتی یک عدد پاداش به آن می‌رسد کمی بزرگ می‌شود.
- **نوار خوک:** با نابود شدن خوک‌ها پر می‌شود (هر خوک یک برش). **شمارنده‌ی داخل نوار** تعداد نابودشده/کل را نشان می‌دهد
  (مثلاً `2/5`) و وقتی آخرین خوک می‌افتد طلایی می‌شود.
- **نوار امتیاز:** با امتیاز پر می‌شود. سه نشانگر ستاره در آستانه‌های ستاره قرار دارند و با یک جهش روشن می‌شوند. روی این نوار
  ترتیب ستاره‌ها **برنزی (اول)، نقره‌ای (دوم) و طلایی (سوم)** است و رنگ نوار از رنگ آخرین ستاره‌ی به‌دست‌آمده پیروی می‌کند.
- **توقف** (دکمه‌ی گرد بالا سمت چپ): بازی را متوقف می‌کند و Resume، Restart، Menu و برای هر دو بخش موسیقی و جلوه‌های صوتی
  یک دکمه‌ی گرد قطع/وصل و یک اسلایدر شدت صدا نشان می‌دهد (همان تنظیمات لابی).

### پایان مرحله

دوربین روی برنده‌ها زوم می‌کند و آن‌ها با کاغذرنگی جشن می‌گیرند. وقتی مرحله برده شود، هر پرنده‌ی استفاده‌نشده همان جا که
ایستاده (فلاخن، نیمکت یا زمین) شادی می‌کند و به نوبت می‌پرد؛ در اوج پرش یک «+10000» بیرون می‌زند و در یک قوس به سمت امتیاز می‌رود.
سپس پنل نتیجه با سه ستاره‌ی بزرگ ظاهر می‌شود (برنزی چپ، طلایی وسط و بزرگ‌تر، نقره‌ای راست؛ یکی‌یکی به ترتیب برنزی، نقره‌ای، طلایی ظاهر می‌شوند)،
امتیاز و بهترین امتیاز شمارش می‌کنند و دکمه‌های Restart / Menu دیده می‌شوند. امتیاز نهایی درشت نمایش داده می‌شود. هر ستاره‌ی به‌دست‌آمده با یک فلش فرود می‌آید
و یک هاله‌ی رنگی نرم، دو لایه پرتو که در جهت مخالف می‌چرخند و چند ستاره‌ی چشمک‌زن اطرافش دارد.

### منوها و روند بازی

`splash -> lobby (آهنگ لابی) -> برج مراحل -> مرحله`

- **Splash:** تصویر عنوان آهسته جلو می‌آید و نوار بارگذاری با یک پرنده‌ی غلتان در انتهایش پر می‌شود. یک لمس، بعد از لحظه‌ای،
  انتظار را رد می‌کند.
- **لابی:** تصویر (`sp_bg.png`) کل صفحه را پر می‌کند و کمی «نفس می‌کشد»، با سایه‌ی ملایم پایین، جرقه‌های شناور، یک دکمه‌ی
  شناور «Continue campaign» که گاهی می‌درخشد، بهترین امتیاز شما در یک قرص پایین سمت چپ (بعد از اولین مرحله‌ی تمام‌شده) و یک
  **دکمه‌ی تنظیمات** گرد بالا سمت راست.
- **تنظیمات** (دکمه‌ی چرخ‌دنده در لابی): دو ردیف، هرکدام با یک دکمه‌ی گرد آیکونی و یک اسلایدر شدت صدا که درصد کنار آن
  نوشته شده است. همه‌چیز روی دستگاه ذخیره می‌شود.
  - **موسیقی:** موسیقی لابی، برج مراحل و داخل بازی. اسلایدر را بکشید تا شدت صدا تنظیم شود (۵۰٪ یعنی موسیقی با نصف شدت
    معمول پخش می‌شود). لمس آیکون موسیقی را کاملاً قطع می‌کند (آیکون تیره می‌شود و خط قرمز روی آن می‌افتد و اسلایدر
    خاکستری می‌شود) و لمس دوباره آن را با همان شدت قبلی برمی‌گرداند. قطع کردن، آهنگ را آهسته محو می‌کند و وصل کردن آهسته
    برمی‌گرداند.
  - **جلوه‌های صوتی (Sound FX):** همه‌ی جلوه‌ها، از جمله صدای دکمه‌ها، صدای پرنده‌ها و بازخورد لمس، با همان اسلایدر و
    آیکون. با رها کردن اسلایدر یک صدای کلیک پخش می‌شود تا شدت جدید شنیده شود.
  - اگر اسلایدر را وقتی ردیف قطع است حرکت بدهید، ردیف دوباره وصل می‌شود؛ اگر آیکون را برای وصل کردن بزنید در حالی که شدت
    صدا ۰٪ است، شدت ۵۰٪ برمی‌گردد.
  همین دو ردیف در **پنل توقف** هنگام بازی هم هستند و از همان تنظیمات ذخیره‌شده استفاده می‌کنند.
  پنل با دکمه‌ی X، با لمس بیرون از آن یا با دکمه‌ی Back اندروید بسته می‌شود.
- **برج مراحل:** یک برج قابل اسکرول از اتاق‌های تم‌دار با درهای طاقدار (انجام‌شده / بعدی / قفل). ۱۴ در وجود دارد
  (۳ + ۳ + ۴ + ۴)؛ فقط مرحله‌ی ۱ باز است و بقیه با لمس، با یک صدای خفه می‌لرزند.
- **پنجره‌ی واحد:** در manifest حالت split screen و پنجره‌ی شناور ممنوع شده است.

### صدا

صدای پرنده‌ها (پرتاب و جیک‌جیک‌های بیکاری)، صدای توانایی‌ها، صدای برخورد و شکستن برای هر جنس، یک آهنگ لوپ‌شونده‌ی منو و یک
فضای آرام بیرونی هنگام بازی (برای فایل‌های همراه پروژه و روش جایگزینی به [ASSETS.md](ASSETS.md) نگاه کنید).
وقتی برنامه به پس‌زمینه می‌رود صدا قطع می‌شود. موسیقی و جلوه‌های صوتی هرکدام شدت صدای جدا (۰ تا ۱۰۰٪، پیش‌فرض ۱۰۰٪)
و دکمه‌ی قطع/وصل جدا دارند، بخش تنظیمات را ببینید.

### کنترل‌ها

فقط لمسی. اسلایدرهای شدت صدا با انگشت کشیده می‌شوند (لمس روی نوار، مستقیم همان‌جا می‌رود). در لابی دکمه‌ی Back اندروید پنل
تنظیمات را می‌بندد و اگر پنل بسته باشد از بازی خارج می‌شود. داخل مرحله، توقف را روشن/خاموش می‌کند. لمس گوشه‌ی بالا سمت راست
صفحه هنگام بازی، نمایش debug مربوط به Box2D را روشن/خاموش می‌کند (برای تنظیم دقیق مفید است و به‌طور پیش‌فرض خاموش است).

## فناوری‌ها

| مورد | انتخاب |
|---|---|
| زبان | Kotlin |
| موتور | libGDX (بک‌اند Android + ماژول core) |
| فیزیک | Box2D (`com.badlogic.gdx.physics.box2d`) |
| حداقل SDK | 24 |
| جهت صفحه | افقی، تمام‌صفحه‌ی واقعی (immersive، زیر برش دوربین هم کشیده می‌شود، بدون نوار سیاه)، صفحه روشن می‌ماند |

## ساخت و اجرا

پیش‌نیازها: Android Studio با JDK 17 و یک دستگاه یا شبیه‌ساز اندروید (API 24+).

1. ریشه‌ی پروژه را در Android Studio باز کنید و بگذارید Gradle همگام شود.
2. کانفیگ `android` را روی دستگاه اجرا کنید.

خط فرمان (از ریشه‌ی پروژه):

</div>

```
./gradlew assembleDebug        # APK: android/build/outputs/apk/debug/android-debug.apk
./gradlew installDebug         # build and install on the connected device
./gradlew assembleRelease      # APK: android/build/outputs/apk/release/
```

<div dir="rtl">

APK نسخه‌ی release فقط وقتی امضا می‌شود که فایل `keystore.properties` در ریشه‌ی پروژه باشد
(چهار کلید آن در کامنت `android/build.gradle` آمده است). بدون آن، Gradle یک APK امضانشده می‌سازد که اندروید نصبش نمی‌کند؛
برای اشتراک‌گذاری از APK دیباگ استفاده کنید یا یک بار keystore بسازید:

</div>

```
keytool -genkeypair -v -keystore release.jks -alias slingshot -keyalg RSA -keysize 2048 -validity 10000
```

<div dir="rtl">

### فایل‌های Gradle

فایل‌های Gradle (`build.gradle`، `settings.gradle`، `gradle.properties`، `core/build.gradle`، `android/build.gradle`) و
فایل‌های ماژول اندروید (`AndroidManifest.xml`، `AndroidLauncher.kt`، `res/`) یک اسکلت مرجع هستند که از چیدمان معمول
gdx-liftoff پیروی می‌کند. این‌ها در محیطی که این تحویل را ساخته (بدون شبکه و بدون Android SDK) build نشده‌اند.
اگر از قبل یک پروژه‌ی سالم ساخته‌شده با gdx-liftoff دارید، فایل‌های Gradle و نسخه‌های خودتان را نگه دارید و فقط
`core/` و `android/assets/` را در آن کپی کنید.

نسخه‌های `gradle.properties` (libGDX 1.13.1، Kotlin 2.0.21، Android Gradle Plugin 8.7.3، Gradle 8.9) مربوط به همین اسکلت
هستند و لزوماً با نسخه‌های شما یکی نیستند. Gradle wrapper (`gradlew`، `gradlew.bat`،
`gradle/wrapper/gradle-wrapper.jar`) باینری است و همراه پروژه نیست. آن را از یک پروژه‌ی liftoff کپی کنید یا یک بار با Gradle
نصب‌شده‌ی محلی `gradle wrapper --gradle-version 8.9` را اجرا کنید.

## دارایی‌ها (Assets)

همه‌ی دارایی‌ها اختیاری‌اند. فایل گم‌شده با یک placeholder تولیدشده جایگزین می‌شود و یک هشدار در Logcat (تگ‌های `Assets` و
`SoundEffects`) نوشته می‌شود، پس بازی همیشه اجرا می‌شود. تصاویر، صداها و فونت همراه پروژه اورجینال و به‌صورت برنامه‌ای
تولید شده‌اند (`tools/`، [ASSETS.md](ASSETS.md) را ببینید)، به‌جز `sp_bg.png` (تصویر عنوان)، چهره‌ها (`enemy_head.png` و سه پرنده که با `tools/make_faces.py` از عکس‌های شما بریده می‌شوند)، آیکون برنامه، فونت (ROGENZ نسخه‌ی دمو، فقط استفاده‌ی شخصی) و ضبط‌هایی که شما داده‌اید.
فایل‌های خودتان را با همان نام در `android/assets/` بگذارید تا جایگزین شوند. نام‌ها، اندازه‌ها و نکته‌ها در
[ASSETS.md](ASSETS.md) است.

- **پس‌زمینه‌ی لابی و splash:** `android/assets/sp_bg.png` را عوض کنید (کل صفحه را می‌پوشاند).
- **صورت خوک:** `android/assets/enemy_head.png` را عوض کنید (تصویر مربعی؛ هنگام بارگذاری به دایره بریده می‌شود).
- **صورت پرنده‌ها:** عکس‌های `tools/source_art/` را عوض کنید (`img2` قرمز، `img3` زرد، `img4` آبی) و `python3 tools/make_faces.py` را اجرا کنید؛ سه فایل `bird.png`، `bird_yellow.png` و `bird_blue.png` دوباره ساخته می‌شوند.
- **فونت:** `tools/fonts/ROGENZ-Demo.ttf` ← `python3 tools/generate_font.py` فایل‌های `font.fnt` و `font.png` را می‌نویسد.
- **آیکون برنامه:** `tools/source_art/icon.jpg` ← `python3 tools/make_launcher_icon.py`.

## معماری

دو ماژول Gradle: `core` (تمام کد بازی، بدون API اندروید) و `android` (launcher و منابع).

</div>

```
core/src/main/kotlin/com/example/slingshot/
├── AngryBirds.kt            Game subclass; owns batch, shapes, font, assets, sounds, music, progress, settings
├── screens/
│   ├── SplashScreen.kt         artwork with a loading bar, then the lobby
│   ├── MenuScreen.kt           lobby: Continue button, best score, settings button and settings panel (mute buttons + volume sliders)
│   ├── LevelSelectScreen.kt    the scrollable level tower
│   └── GameScreen.kt           wiring only: creates and connects everything below
├── world/
│   ├── Units.kt                world size (23.4 x 10.8 m), gravity, fixed step constants
│   ├── PhysicsWorld.kt         Box2D World + fixed-timestep accumulator
│   ├── BodyFactory.kt          creates ground, box, circle and polygon bodies
│   ├── ImpactQueue.kt          impacts recorded during the step (no allocation in steady state)
│   └── GameContactListener.kt  postSolve -> ImpactQueue, nothing else
├── entities/
│   ├── Entity.kt               body + hp + optional damage profile
│   ├── Bird.kt, BirdType.kt    READY / DRAGGING / FLYING; red, yellow, blue
│   ├── Block.kt                Material enum (WOOD, STONE, GLASS, TNT) + Block
│   ├── Enemy.kt                the pig
│   └── DamageConfig.kt         all damage and blast tuning
├── systems/
│   ├── SlingshotConfig.kt      anchor, MAX_PULL, POWER and band geometry
│   ├── SlingshotController.kt  touch -> drag -> launch
│   ├── DamageSystem.kt         impacts -> HP, TNT blasts, removes destroyed entities after the step
│   ├── GameStateManager.kt     bird queue, abilities, PLAYING / WON / LOST, settle detection
│   ├── ScoreManager.kt         points, bird bonus, star thresholds
│   ├── Progress.kt             best score and best stars (Preferences)
│   ├── Settings.kt             music on/off, sound on/off, music volume and sound volume (Preferences)
│   └── GameEvents.kt           stretch / launch / hop / impact / destroyed / win / lose / star / click ...
├── level/
│   ├── LevelData.kt            pure data description of a level
│   ├── Levels.kt               LEVEL_1 (generated by tools/generate_level.py)
│   └── LevelLoader.kt          LevelData -> bodies and entities
├── render/
│   ├── GameRenderer.kt         scenery, tiled blocks, pigs, band, pouch, waiting and cheering birds, debug overlay
│   ├── Trajectory.kt           AimPreview (dotted prediction while aiming) and FlightTrail (dots left by a shot)
│   ├── Effects.kt              debris, smoke, floating score numbers, easing functions (Ease)
│   ├── Finale.kt               state of the level-end sequence (unused birds, where they stand)
│   ├── CheerPose.kt            crouch / jump / landing of a counted bird, as functions of time
│   ├── BonusFlyers.kt          the "+10000" numbers that burst out of the birds and fly into the score
│   ├── CountUp.kt              numbers that count up smoothly
│   ├── VolumeSlider.kt         the volume slider (drawing and touch -> value), shared by the lobby and the pause panel
│   └── Hud.kt                  score, pig bar with counter, star score bar, pause panel (with volume sliders), result panel
├── assets/
│   ├── Assets.kt               texture loading, placeholder fallback, circular face crop, star colors
│   └── PlaceholderArt.kt       generated placeholder images (used only when a file is missing)
└── audio/
    ├── SoundEffects.kt         sound effects, implements GameEvents, master switch (enabled) and volume (volumeScale)
    └── MusicPlayer.kt          looping tracks with cross-fades, ducking, master switch (enabled) and volume (volumeScale)
tools/                          generate_art.py (+ art_ui.py, art_scenery.py), generate_sounds.py, generate_font.py,
                                generate_level.py, recolor_stars.py, make_faces.py, make_launcher_icon.py
android/
├── AndroidManifest.xml, build.gradle, res/
├── assets/                     images, sounds, music and the font
└── src/main/kotlin/.../AndroidLauncher.kt
```

<div dir="rtl">

### روند هر فریم (داخل مرحله)

`GameScreen.render(delta)`:

1. `PhysicsWorld.update` فیزیک Box2D را با گام ثابت ۱/۶۰ ثانیه جلو می‌برد (زمان فریم حداکثر ۰.۲۵ ثانیه محدود می‌شود). پیش از
   هر گام، هر موجودیت وضعیت قبلی‌اش را ذخیره می‌کند. contact listener فقط ضربه‌ها را ثبت می‌کند.
2. `DamageSystem.update` ضربه‌های ثبت‌شده را به آسیب تبدیل می‌کند و حالا که دنیا در حال گام برداشتن نیست بدنه‌ها را نابود می‌کند.
3. `GameStateManager.update` پرنده‌ها را بازنشسته و ایجاد می‌کند و برد یا باخت را تعیین می‌کند.
4. هر موجودیت موقعیت رسم خود را بین وضعیت قبلی و فعلی فیزیک (`PhysicsWorld.alpha`) حساب می‌کند، پس حرکت روی صفحه‌های
   ۹۰/۱۲۰ هرتز روان است.
5. `GameRenderer` دنیا را بر حسب متر رسم می‌کند و بعد `Hud` لایه‌ی رویی را در یک صفحه‌ی مجازی جدا (حداقل ۱۲۸۰×۷۲۰) می‌کشد.

## تصمیم‌های طراحی

- **همه‌جا متر.** دنیای فیزیک ۲۳.۴ × ۱۰.۸ متر است. پیکسل هرگز وارد کد فیزیک نمی‌شود.
- **تمام‌صفحه بدون نوار.** هم دنیا و هم UI از `ExtendViewport` استفاده می‌کنند: تصویر کل نمایشگر را پر می‌کند و صفحه‌ی عریض‌تر
  فقط صحنه‌ی بیشتری در دو طرف نشان می‌دهد. تم اندروید و launcher نوارهای سیستم را پنهان می‌کنند و زیر برش دوربین می‌کشند؛
  UI نزدیک لبه‌ها safe inset را رعایت می‌کند.
- **رندر درون‌یابی‌شده.** فیزیک با ۶۰ هرتز ثابت اجرا می‌شود؛ اسپرایت‌ها در موقعیتی بین دو گام آخر رسم می‌شوند
  (`Entity.renderX/Y/Angle`).
- **بلوک‌های کاشی‌شده، نه کشیده.** بافت بلوک‌ها در اندازه‌ی ثابت به‌ازای هر متر در راستای ضلع بلند تکرار می‌شود؛ آسیب با
  لایه‌های ترک نشان داده می‌شود.
- **پیش‌نمایش نشانه‌گیری = همان فیزیک واقعی.** `AimPreview` گرانش و damping را با همان گام ثابتی که Box2D استفاده می‌کند و از
  همان سرعت پرتاب انتگرال می‌گیرد.
- **هیچ تغییری روی بدنه‌ها در حین گام.** callbackهای برخورد هرگز به سلامت یا بدنه‌ها دست نمی‌زنند. آن‌ها در `ImpactQueue` می‌نویسند؛
  بدنه‌ها بعد از گام در `DamageSystem` و `GameStateManager` نابود می‌شوند.
- **آسیب بر پایه‌ی ضربه.** `damage = max(0, impulse - threshold) * factor` برای هر جنس. همه‌ی عددها در `DamageConfig` و
  `Material` هستند. یک ثانیه‌ی اول بعد از بارگذاری آسیب نادیده گرفته می‌شود (آرام گرفتن برج).
- **استاتیک هنگام نگه داشتن.** پرنده تا رها شدن یک بدنه‌ی استاتیک است، پس به joint نیاز ندارد.
- **Restart = یک `GameScreen` جدید.** `hide()` دنیا و renderer قبلی را dispose می‌کند، پس هیچ بدنه یا listenerی از یک تلاش به
  تلاش بعد نمی‌ماند.
- **رویداد، نه وابستگی.** منطق بازی `GameEvents` گزارش می‌دهد؛ `SoundEffects` آن را پیاده‌سازی می‌کند.
- **یک جا برای تنظیمات صدا.** `AngryBirds.setMusicOn` / `setSoundOn` (قطع/وصل) و `setMusicVolume` / `setSoundVolume`
  (اسلایدر) انتخاب را در `Settings` ذخیره می‌کنند و روی `MusicPlayer` / `SoundEffects` اعمال می‌کنند (`enabled`،
  `volumeScale`)؛ هیچ صفحه‌ای مستقیم به کلاس‌های صدا دست نمی‌زند. هر چهار مقدار هنگام شروع برنامه برگردانده می‌شوند.
  قطع کردن، شدت صدا را نگه می‌دارد؛ پس وصل کردن به همان سطح برمی‌گردد.
- **ستاره از روی امتیاز.** `ScoreManager.starsFor` امتیاز نهایی را به ۰ تا ۳ ستاره تبدیل می‌کند. `Assets.starOnFor` رنگ‌ها را
  بر اساس جایگاه می‌دهد (برنزی، طلایی، نقره‌ای): پنل نتیجه و درهای برج از آن استفاده می‌کنند، پس ستاره‌ی طلایی همان وسطی است.
  ستاره‌های پنل نتیجه به ترتیب برنزی، نقره‌ای، طلایی ظاهر می‌شوند (`RESULT_STAR_RANK` در `Hud.kt`). فقط نوار امتیاز داخل
  بازی از چپ به راست برنزی، نقره‌ای، طلایی را نشان می‌دهد (`SCORE_BAR_STAR_STYLE`).
- **فونت bitmap و layoutها.** یک `GlyphLayout` فقط در همان مقیاس فونتی که با آن ساخته شده معتبر است، پس هر متن با همان ضریب
  اندازه‌ای رسم می‌شود که با آن layout شده است.
- **Placeholderها بافت واقعی‌اند.** پشتوانه‌ی آن‌ها pixmapهای managed است، پس مثل بافت‌های فایلی از دست رفتن GL context
  اندروید را تحمل می‌کنند.

## نکات تنظیم (Tuning)

| چه چیزی | کجا |
|---|---|
| قدرت پرتاب، حداکثر و حداقل کشش، شعاع گرفتن، نقطه‌ی لنگر | `systems/SlingshotConfig.kt` |
| آستانه و ضریب آسیب، شعاع انفجار TNT، زمان فرصت آرام‌گیری | `entities/DamageConfig.kt` |
| چگالی، اصطکاک و HP بلوک | `entities/Block.kt` (`Material`) |
| HP و اندازه‌ی خوک | `entities/Enemy.kt` |
| استراحت/تایم‌اوت پرنده، توانایی‌ها، زمان‌های آرام‌گیری برد و باخت | `systems/GameStateManager.kt` |
| امتیازها، پاداش پرنده، آستانه‌ی ستاره‌ها، حداکثر نوار امتیاز | `systems/ScoreManager.kt` |
| چیدمان مرحله و ترتیب پرنده‌ها | `level/Levels.kt` (با `tools/generate_level.py` ساخته می‌شود) |
| گرانش، گام ثابت، تعداد iterationها | `world/Units.kt` |
| آستانه و شدت صدای برخورد | `audio/SoundEffects.kt` |
| شدت آهنگ‌ها و سرعت محو شدن | `audio/MusicPlayer.kt` |
| شدت صدای پیش‌فرض، شدت صدای برگشتی هنگام وصل کردن در ۰٪ | `systems/Settings.kt` |
| چیدمان پنل توقف و ردیف‌های شدت صدا | `render/Hud.kt` |
| ظاهر و اندازه‌ی اسلایدر | `render/VolumeSlider.kt` |
| چیدمان لابی، انیمیشن و پنل تنظیمات | `screens/MenuScreen.kt` |
| نوار خوک، نوار امتیاز، شمارنده، ترتیب ستاره‌ها (نوار امتیاز: `SCORE_BAR_STAR_STYLE`) | `render/Hud.kt`، `assets/Assets.kt` |

قاعده‌ی سرانگشتی: `POWER` بیشتر یعنی شلیک سریع‌تر و صاف‌تر. `DamageProfile.threshold` کمتر یا `factor` بیشتر سازه‌ها را
شکننده‌تر می‌کند. برای بررسی برخوردها، هنگام بازی گوشه‌ی بالا سمت راست را لمس کنید.

## محدودیت‌های شناخته‌شده

- یک مرحله‌ی قابل بازی؛ ۱۳ در دیگر برج قفل است. `LevelData` کد Kotlin ثابت (تولیدشده) است و از JSON بارگذاری نمی‌شود.
- تست خودکار وجود ندارد؛ راستی‌آزمایی همان چک‌لیست دستی [docs/TESTING.md](docs/TESTING.md) است.
- پیاده‌سازی نشده: دوربین دنبال‌کننده، ویبره، backend جدول امتیازات.
- فایل‌های بسیار بزرگ `enemy_head.png` (بالای حدود ۲۰۴۸ پیکسل) پیش از بریده شدن کامل decode می‌شوند و ممکن است حافظه‌ی زیادی بگیرند.

## مستندات بیشتر

- [README.md](README.md): همین فایل به انگلیسی
- [ASSETS.md](ASSETS.md): نام و فرمت فایل‌های دارایی
- [docs/TESTING.md](docs/TESTING.md): چک‌لیست تست دستی
- [docs/DEMO_SCRIPT.md](docs/DEMO_SCRIPT.md): متن کوتاه برای ارائه‌ی پروژه
- [docs/CHANGELOG.md](docs/CHANGELOG.md): تغییرات هر بخش

</div>
