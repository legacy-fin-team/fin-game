# Fin Game

Мобильная игра для Android, которая учит ребёнка обращаться с деньгами. Ребёнок заводит
питомца, ухаживает за ним, получает ежедневный доход, планирует бюджет (обязательные траты,
необязательные траты, сбережения, вклад), копит на цели и принимает решения в квестах.
Для родителя предусмотрен взрослый режим с замком и отчётами о том, как ребёнок распоряжался
деньгами.

Приложение полностью автономное: сервера нет, сетевых запросов нет, все данные хранятся
локально на устройстве.

- Платформа: Android 8.0+ (minSdk 26), targetSdk 37.
- Язык и UI: Kotlin, Jetpack Compose (Material 3), пиксельная графика.
- Язык интерфейса: русский.

Полная сопроводительная документация: [`docs/DOCUMENTATION.md`](docs/DOCUMENTATION.md)
(она же в Word: [`docs/FinGame-Документация.docx`](docs/FinGame-Документация.docx)).

## Что умеет игра

| Возможность | Где в коде |
|---|---|
| Выбор питомца (кот двух окрасов, золотая рыбка) и имени | `ui/screens/AnimalSelectScreen.kt`, `assets/data/animals.xml` |
| Уход: шкалы здоровья, сытости и настроения, которые убывают со временем | `game/stats/PetStats.kt`, `game/stats/StatKind.kt` |
| Рост питомца по этапам (один этап в сутки) | `game/animals/Growth.kt` |
| Доход: бонус дня +50 монет раз в календарные сутки | `game/economy/Economy.kt` |
| Магазин с категориями, корзиной и вариантами товаров | `ui/screens/ShopScreen.kt`, `assets/data/items.xml` |
| Инвентарь: еда расходуется, игрушки многоразовые, одежда и декор надеваются | `ui/screens/InventoryScreen.kt`, `game/items/*` |
| Бюджет периода: обязательные / необязательные траты / сбережения, отчёт «план — факт» | `game/economy/Budget.kt`, `ui/screens/BudgetScreen.kt` |
| Вклад на 2–7 дней под 4–15 % | `game/economy/Deposit.kt` |
| Цели: звёздочка на товаре в магазине и прогресс накопления | `game/items/Goals.kt`, `ui/screens/GoalsCarousel.kt` |
| Журнал всех движений денег | `game/economy/MoneyLog.kt`, `ui/screens/LogScreen.kt` |
| Демо-сборка с кнопкой «Вперёд на 12 часов» | `DemoMode.kt`, `app/build.gradle.kts` |
| Настройки: звуки, музыка, тема (светлая/тёмная/авто), сброс прогресса; кнопка «Режим взрослого» — пока заглушка | `ui/screens/SettingsScreen.kt`, `game/settings/*` |
| Поглаживание питомца касанием (сердечки, звук) | `game/scene/PetTouch.kt` |

Функции в ветках, ожидающих слияния:

| Возможность | Ветка | PR |
|---|---|---|
| Квесты: ситуации выбора с последствиями для денег и питомца, случайные события, кулдаун и одноразовые квесты | `stef-quests` | #20, готова |
| Взрослый режим: замок, прогресс по темам без оценок, отчёты и история, проверка квестов взрослым, изменение монет с причиной, награды из жизни, свои товары и квесты (включает `stef-quests`; настройки из бывшей ветки `options` уже в `main`) | `stef-adult` | #21, готова |
| Рост в зависимости от ухода; штрафы за дни без заботы (меньше бонус дня, дороже одежда и декор) | `stef-growth` | готовится |
| Окно знакомства при первом запуске и раздел «Помощь» со словариком терминов | `stef-onboarding` | готовится |

Подробный статус — в разделе 5 документации.

## Состав репозитория

```
.
├── app/
│   ├── build.gradle.kts            модуль приложения: SDK, buildTypes, демо-режим, зависимости
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── assets/
│       │   │   ├── data/           игровые данные: animals.xml, items.xml, audio.xml
│       │   │   ├── textures/       спрайты: животные, предметы, интерфейс, локации
│       │   │   └── audio/          фоновая музыка и звуки питомца (OGG), attributions.xml
│       │   ├── java/com/legacy/fingame/
│       │   │   ├── MainActivity.kt, FinGameApplication.kt, DemoMode.kt
│       │   │   ├── game/           игровая логика без UI: economy, animals, items, stats, scene, settings
│       │   │   ├── ui/             Compose: FinGameApp (навигация), screens, components, theme
│       │   │   └── utils/          хранилище (PlayerPreferences), кодеки, SpriteLoader
│       │   └── res/                иконка, тема, пиксельный шрифт Press Start 2P и его лицензия
│       ├── test/                   юнит-тесты JUnit 4 (385 тестов, из них 2 падают — см. документацию)
│       └── androidTest/            инструментальные Compose-тесты
├── gradle/                         libs.versions.toml, wrapper, JVM-тулчейн демона Gradle
├── docs/                           сопроводительная документация
├── build.gradle.kts, settings.gradle.kts, gradle.properties
└── gradlew, gradlew.bat
```

## Быстрый запуск

Нужны: JDK 17 или новее (Gradle сам подтягивает JDK 25 для демона), Android SDK с платформой
`android-37.0`, Android Studio последней версии (по желанию).

1. Установите платформу SDK. Платформа 37.0 публикуется в preview-канале sdkmanager:

   ```bash
   sdkmanager --channel=3 "platform-tools" "platforms;android-37.0" "build-tools;37.0.0"
   ```

2. Укажите путь к SDK (или откройте проект в Android Studio — файл создастся сам):

   ```bash
   echo "sdk.dir=$HOME/Library/Android/sdk" > local.properties   # macOS; на Linux обычно ~/Android/Sdk
   ```

3. Соберите и установите отладочную (демо) сборку на подключённое устройство или эмулятор:

   ```bash
   ./gradlew :app:installDebug
   ```

   Или вручную: `./gradlew :app:assembleDebug` → `app/build/outputs/apk/debug/app-debug.apk`.

4. Запустите юнит-тесты:

   ```bash
   ./gradlew :app:testDebugUnitTest
   ```

Сборка релизного APK и его подпись описаны пошагово в
[разделе 2 документации](docs/DOCUMENTATION.md#2-требования-к-окружению-и-сборка-релизного-apk).

## Варианты сборки

| Вариант | Debuggable | Подпись | Минификация | `DEMO_MODE` | Чем отличается в игре | Команда | APK |
|---|---|---|---|---|---|---|---|
| debug | да | отладочный ключ Android SDK | нет | `true` | Есть кнопка «+12 ч» (перемотка времени, см. `DemoMode.kt`) | `./gradlew :app:assembleDebug` | `app/build/outputs/apk/debug/app-debug.apk` |
| release | нет | не задана — APK неподписанный, подписывается вручную (`apksigner`) | выключена (`optimization { enable = false }`) | `false` | Кнопки перемотки нет, время идёт как у игрока | `./gradlew :app:assembleRelease` | `app/build/outputs/apk/release/app-release-unsigned.apk` |
| releaseDebuggable | да (копия `release` + `isDebuggable = true`) | отладочный ключ Android SDK (как у debug) | выключена (копия `release`) | `false` | Как у release, но APK уже подписан отладочным ключом и ставится поверх debug-сборки, без удаления | `./gradlew :app:assembleReleaseDebuggable` | `app/build/outputs/apk/releaseDebuggable/app-releaseDebuggable.apk` |

`DEMO_MODE` переопределяется для любого варианта свойством `-Pfingame.demoMode=true|false`
(например, `./gradlew :app:assembleRelease -Pfingame.demoMode=true` — релизная сборка с кнопкой
перемотки времени для показа жюри). Подробнее, включая правило для отладочных сборок в системе
квестов, — в [разделе 2.2 документации](docs/DOCUMENTATION.md#22-типы-сборки-и-демо-режим).

## Документация

[`docs/DOCUMENTATION.md`](docs/DOCUMENTATION.md) содержит:

1. Назначение, состав, быстрый запуск.
2. Требования к окружению и сборка релизного APK.
3. Функциональная и компонентная архитектура.
4. Структуры данных: профиль, экономика, задания, прогресс.
5. Матрица соответствия функциональным требованиям.
6. Формулы и правила расчёта.
7. Карта образовательного контента.
8. UX/UI-решения и доступность.
9. Разрешения Android, собираемые данные, удаление профиля.
10. Тест-кейсы и отчёт о проверке на устройстве.
11. Известные ограничения и план развития.
12. Сторонние библиотеки, шрифты и ресурсы.

Word-версия собирается из Markdown командой `python3 docs/tools/make_docx.py` (нужен pandoc 3.x).

## Лицензии сторонних компонентов

Библиотеки AndroidX, Jetpack Compose и Coil распространяются по Apache License 2.0, шрифт
Press Start 2P — по SIL Open Font License 1.1 (текст лицензии — в
`app/src/main/res/raw/press_start_2p_regular.txt`), фоновая музыка и звуки питомца — по CC0-1.0 /
CC-BY-3.0 / CC-BY-4.0 (список источников — в `attributions.xml` рядом с аудиофайлами). Подробнее,
включая ресурсы без указанного источника, — раздел 12 документации.
