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

Функции в открытых ветках (квесты, настройки со звуком и музыкой, взрослый режим, онбординг,
правила роста) и их статус описаны в разделе 5 документации.

## Состав репозитория

```
.
├── app/
│   ├── build.gradle.kts            модуль приложения: SDK, buildTypes, демо-режим, зависимости
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── assets/
│       │   │   ├── data/           игровые данные: animals.xml, items.xml
│       │   │   └── textures/       спрайты: животные, предметы, интерфейс, локации
│       │   ├── java/com/legacy/fingame/
│       │   │   ├── MainActivity.kt, FinGameApplication.kt, DemoMode.kt
│       │   │   ├── game/           игровая логика без UI: economy, animals, items, stats, scene
│       │   │   ├── ui/             Compose: FinGameApp (навигация), screens, components, theme
│       │   │   └── utils/          хранилище (PlayerPreferences), кодеки, SpriteLoader
│       │   └── res/                иконка, тема, пиксельный шрифт Press Start 2P
│       ├── test/                   юнит-тесты JUnit 4 (354 теста)
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

## Сборки

| Сборка | Команда | Демо-режим | Подпись |
|---|---|---|---|
| debug | `./gradlew :app:assembleDebug` | включён | отладочный ключ |
| release | `./gradlew :app:assembleRelease` | выключен | не подписан, подписывается вручную `apksigner` |

Демо-режим переопределяется для любой сборки свойством `-Pfingame.demoMode=true|false`.

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

## Лицензии сторонних компонентов

Библиотеки AndroidX, Jetpack Compose и Coil распространяются по Apache License 2.0, шрифт
Press Start 2P — по SIL Open Font License 1.1. Подробнее — раздел 12 документации.
