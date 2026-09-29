# Сборка: окружение, варианты сборки, релизный APK, тесты

Связанные разделы: [архитектура](architecture.md), [тестирование](testing.md),
[квесты: ожидание в разных сборках](quests.md), [рост питомца: перемотка «+12 ч»](pet-growth.md).

## 1. Версии инструментов

| Компонент | Версия | Где задано |
|---|---|---|
| Gradle | 9.6.0 | `gradle/wrapper/gradle-wrapper.properties` |
| Android Gradle Plugin | 9.4.0 | `gradle/libs.versions.toml` (`agp`) |
| Kotlin (плагин Compose Compiler) | 2.4.20 | `gradle/libs.versions.toml` (`kotlin`) |
| JDK для демона Gradle | 25, скачивается автоматически (foojay-resolver) | `gradle/gradle-daemon-jvm.properties`, `settings.gradle.kts` |
| JDK для запуска `gradlew` | 17 или новее | требование AGP 9 |
| Уровень байткода | Java 11 | `app/build.gradle.kts` (`compileOptions`) |
| compileSdk / targetSdk | 37 (платформа `android-37.0`) | `app/build.gradle.kts` |
| minSdk | 26 (Android 8.0) | `app/build.gradle.kts` |
| Build Tools | 37.0.0 | ставится вместе с платформой |
| Пакет, версия | `com.legacy.fingame`, `versionName` 1.0, `versionCode` 1 | `app/build.gradle.kts` |
| Название приложения | «Финансовый Питомец» (под значком и в списке приложений) | строка `app_name` в `app/src/main/res/values/strings.xml` |

AGP 9 адресует платформы с минорной версией, поэтому нужна именно `platforms;android-37.0`. Она
опубликована в preview-канале sdkmanager (`--channel=3`); без неё сборка падает с
`Failed to find target with hash string 'android-37.0'`.

Первая сборка требует интернета: Gradle скачивает дистрибутив, JDK 25 для демона и зависимости.
Дальше можно собирать с `--offline`. В `gradle.properties` включён configuration cache.

## 2. Быстрый запуск

```bash
sdkmanager --channel=3 "platform-tools" "platforms;android-37.0" "build-tools;37.0.0"
echo "sdk.dir=$HOME/Library/Android/sdk" > local.properties   # macOS; на Linux обычно ~/Android/Sdk
./gradlew :app:installDebug                                    # собрать демо-сборку и поставить на устройство
./gradlew :app:testDebugUnitTest                               # юнит-тесты
```

Вместо `local.properties` можно задать переменную окружения `ANDROID_HOME`; Android Studio создаёт
файл сама.

## 3. Варианты сборки

Флейворов нет. В `app/build.gradle.kts` три типа сборки: стандартные `debug` и `release` и
дополнительный `releaseDebuggable` (`initWith(release)`, `isDebuggable = true`, подпись отладочным
ключом) — чтобы проверять на устройстве поведение релиза и при этом видеть логи и подключать
отладчик.

| Параметр | debug | release | releaseDebuggable |
|---|---|---|---|
| `isDebuggable` / `BuildConfig.DEBUG` | да | нет | да |
| `BuildConfig.DEMO_MODE` (по умолчанию) | `true` | `false` | `false` |
| Кнопка «+12 ч» (перемотка времени) | есть | нет | нет |
| «Ещё раз» у пройденного квеста игрока | есть | нет | нет |
| Кулдаун квестов и паузы между шагами | действуют | действуют | действуют |
| Минификация (R8) | нет | выключена (`optimization { enable = false }`) | выключена |
| Подпись | отладочный ключ Android SDK | не задана — APK неподписанный | отладочный ключ Android SDK |
| Команда | `./gradlew :app:assembleDebug` | `./gradlew :app:assembleRelease` | `./gradlew :app:assembleReleaseDebuggable` |
| APK | `app/build/outputs/apk/debug/app-debug.apk` | `app/build/outputs/apk/release/app-release-unsigned.apk` | `app/build/outputs/apk/releaseDebuggable/app-releaseDebuggable.apk` |

Два независимых признака сборки:

- **Демо-режим** — `BuildConfig.DEMO_MODE` → `DemoMode.ENABLED`. Даёт кнопку «+12 ч» на главном
  экране (см. [pet-growth.md](pet-growth.md), раздел 7) и кнопку «Ещё раз» у пройденного квеста.
  Переопределяется для любого типа gradle-свойством `fingame.demoMode`:
  `./gradlew :app:assembleRelease -Pfingame.demoMode=true` — релиз с перемоткой для показа жюри;
  `./gradlew :app:assembleDebug -Pfingame.demoMode=false` — отладочная сборка без неё.
- **Отладка** — `BuildConfig.DEBUG` (debug и releaseDebuggable): логи и отладчик. На правила игры
  он не влияет. Кулдауны и паузы квестов действуют во всех сборках: `MainActivity` передаёт в
  `GameViewModel` `ignoreQuestDelays = false`. В демо-сборке их, как и остальное ожидание, пропускает
  «+12 ч» (см. [quests.md](quests.md), раздел 6).

Остальное поведение одинаково во всех сборках.

`app-debug.apk` и `app-releaseDebuggable.apk` подписаны одним отладочным ключом и ставятся друг
поверх друга. APK, подписанный релизным ключом, поверх них не ставится (и наоборот) — сначала
`adb uninstall com.legacy.fingame`, при этом прогресс теряется.

## 4. Сборка релизного APK по шагам

Проверено на macOS (Apple Silicon), JDK 25, build-tools 37.0.0: сборка релиза занимает меньше
минуты, неподписанный APK — около 13 МБ.

1. Клонировать репозиторий и перейти в его корень.
2. Установить компоненты SDK:

   ```bash
   sdkmanager --channel=3 "platform-tools" "platforms;android-37.0" "build-tools;37.0.0"
   ```

3. Указать SDK: `echo "sdk.dir=$HOME/Library/Android/sdk" > local.properties` или `ANDROID_HOME`.
4. Собрать релиз:

   ```bash
   ./gradlew :app:assembleRelease
   # демо-вариант релиза: ./gradlew :app:assembleRelease -Pfingame.demoMode=true
   ```

   Результат: `app/build/outputs/apk/release/app-release-unsigned.apk`.

5. Создать ключ подписи (один раз; хранить вне репозитория — в репозитории ключей и паролей нет):

   ```bash
   keytool -genkeypair -v -keystore fingame-release.jks -alias fingame \
     -keyalg RSA -keysize 2048 -validity 10000
   ```

6. Выровнять и подписать APK утилитами из build-tools:

   ```bash
   BT=$ANDROID_HOME/build-tools/37.0.0      # или $HOME/Library/Android/sdk/build-tools/37.0.0
   $BT/zipalign -p -f 4 app/build/outputs/apk/release/app-release-unsigned.apk fingame-aligned.apk
   $BT/apksigner sign --ks fingame-release.jks --ks-key-alias fingame \
     --out fingame-1.0-release.apk fingame-aligned.apk
   $BT/apksigner verify --print-certs fingame-1.0-release.apk
   ```

7. Установить: `adb install -r fingame-1.0-release.apk`.

## 5. Тесты из командной строки

| Команда | Что делает |
|---|---|
| `./gradlew :app:testDebugUnitTest` | Юнит-тесты на JVM; отчёт `app/build/reports/tests/testDebugUnitTest/index.html` |
| `./gradlew :app:testDebugUnitTest --tests '*QuestEngineTest'` | Один класс |
| `./gradlew :app:connectedDebugAndroidTest` | Инструментальные тесты на подключённом устройстве или эмуляторе |

Если `connectedDebugAndroidTest` не может скачать зависимости тестового раннера, те же тесты
запускаются вручную:

```bash
./gradlew :app:assembleDebug :app:assembleDebugAndroidTest
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r -t app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w com.legacy.fingame.test/androidx.test.runner.AndroidJUnitRunner
```

Состав тестов и ручные сценарии — [testing.md](testing.md).

## 6. Word-версия документации

Отдельный документ для сдачи собирается из файлов `docs/*.md` скриптом (нужен pandoc 3.x):

```bash
python3 docs/tools/make_docx.py
```

Результат — `docs/FinGame-Документация.docx`; порядок разделов задан в самом скрипте и совпадает с
оглавлением [docs/README.md](README.md).
