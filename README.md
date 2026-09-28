# Fin Game

Мобильная игра для Android, которая учит ребёнка обращаться с деньгами. Ребёнок заводит питомца и
заботится о нём, получает монеты раз в день, планирует бюджет (обязательные и необязательные траты,
сбережения, вклад), копит на цели и принимает решения в квестах. Взрослый за простым замком видит
прогресс ребёнка по темам финансовой грамотности, проверяет задания и добавляет свои квесты и
награды.

Приложение полностью автономное: сервера, учётных записей и сетевых запросов нет, все данные
хранятся на устройстве.

- Платформа: Android 8.0+ (minSdk 26), targetSdk 37; пакет `com.legacy.fingame`, версия 1.0 (1).
- Стек: Kotlin, Jetpack Compose (Material 3), пиксельная графика, данные игры в XML.
- Язык интерфейса: русский.

Полная документация — [docs/README.md](docs/README.md); Word-версия для сдачи —
`docs/FinGame-Документация.docx`.

## Что умеет игра

| Возможность | Подробнее |
|---|---|
| Выбор питомца (кот двух окрасов, золотая рыбка) и имени | [animals.md](docs/animals.md) |
| Уход: шкалы здоровья, сытости и настроения убывают и при закрытом приложении; еда и игрушки их восполняют | [pet-growth.md](docs/pet-growth.md) |
| Рост питомца в три этапа: скорость зависит от ухода; дни без заботы уменьшают бонус дня и повышают цены на необязательное | [pet-growth.md](docs/pet-growth.md) |
| Доход: бонус дня раз в календарные сутки; награды квестов | [economy.md](docs/economy.md) |
| Магазин: еда, игрушки, одежда, декор, награды «Другое»; варианты, корзина, подтверждение покупки | [items.md](docs/items.md) |
| Инвентарь: съесть, поиграть, надеть одежду, поставить декор в комнату | [items.md](docs/items.md) |
| Сцена с питомцем: пять слоёв, масштаб и перемещение жестами, поглаживание (сердечки, звук) | [scene.md](docs/scene.md) |
| Бюджет периода: обязательные, необязательные, вклад, «Останется»; итог «план — факт — разница» | [economy.md](docs/economy.md) |
| Вклад на 2–7 дней под 4–15 %, досрочное закрытие без процентов | [economy.md](docs/economy.md) |
| Цели: звёздочка в магазине и прогресс накопления на главном экране | [items.md](docs/items.md) |
| Журнал всех движений денег по дням | [economy.md](docs/economy.md) |
| Квесты: истории с выбором, случайные события, кулдауны, одноразовые квесты | [quests.md](docs/quests.md) |
| Подсказки по экранам при первом входе и словарик «Помощь» | [education.md](docs/education.md) |
| Взрослый режим: замок, прогресс по темам без оценок, отчёты по дням, покупкам и квестам, проверка заданий, изменение монет с причиной, свои товары, награды и квесты | [quests.md](docs/quests.md), [education.md](docs/education.md) |
| Настройки: громкость звуков и музыки, тема (светлая / тёмная / авто), сброс прогресса | [ux-accessibility.md](docs/ux-accessibility.md) |
| Демо-сборка с кнопкой «+12 ч» для показа игры за несколько минут | [build.md](docs/build.md) |

Соответствие требованиям ТЗ по пунктам — [requirements.md](docs/requirements.md).

## Состав репозитория

```
.
├── app/
│   ├── build.gradle.kts            модуль приложения: SDK, типы сборки, демо-режим, зависимости
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml     одна Activity, разрешений нет
│       │   ├── assets/
│       │   │   ├── data/           игровые данные: animals, items, quests, care, hints, help, audio (.xml)
│       │   │   ├── textures/       спрайты: животные, предметы, интерфейс, магазин, комната, эффекты
│       │   │   └── audio/          фоновая музыка и звуки питомца (OGG) + attributions.xml
│       │   ├── java/com/legacy/fingame/
│       │   │   ├── MainActivity.kt, FinGameApplication.kt, DemoMode.kt
│       │   │   ├── game/           логика без UI: economy, rules, stats, animals, items, quests,
│       │   │   │                   scene, adult, hints, help, settings; GameViewModel, PlayerState
│       │   │   ├── ui/             Compose: FinGameApp (навигация), screens, components, theme
│       │   │   └── utils/          хранилище PlayerPreferences, кодеки, SpriteLoader
│       │   └── res/                иконка, тема, шрифт Press Start 2P и его лицензия (res/raw)
│       ├── test/                   юнит-тесты JUnit 4 (65 классов, 718 тестов)
│       └── androidTest/            инструментальные Compose-тесты
├── docs/                           документация (docs/README.md — оглавление), Word-версия, tools/
├── gradle/                         libs.versions.toml, wrapper, JVM-тулчейн демона Gradle
├── build.gradle.kts, settings.gradle.kts, gradle.properties
└── gradlew, gradlew.bat
```

## Быстрый запуск

Нужны JDK 17 или новее (Gradle сам скачает JDK 25 для демона) и Android SDK с платформой
`android-37.0`; Android Studio — по желанию.

1. Установить платформу SDK (она опубликована в preview-канале sdkmanager):

   ```bash
   sdkmanager --channel=3 "platform-tools" "platforms;android-37.0" "build-tools;37.0.0"
   ```

2. Указать путь к SDK (или открыть проект в Android Studio — файл создастся сам):

   ```bash
   echo "sdk.dir=$HOME/Library/Android/sdk" > local.properties   # macOS; на Linux обычно ~/Android/Sdk
   ```

3. Собрать и установить демо-сборку на устройство или эмулятор:

   ```bash
   ./gradlew :app:installDebug
   ```

4. Запустить юнит-тесты:

   ```bash
   ./gradlew :app:testDebugUnitTest
   ```

Сборка и подпись релизного APK по шагам — [build.md](docs/build.md).

## Варианты сборки

| Вариант | Для чего | Перемотка «+12 ч» | Ожидание в квестах | Подпись | Команда |
|---|---|---|---|---|---|
| debug | Разработка и показ | есть | нет (кулдауны и паузы сняты) | отладочный ключ | `./gradlew :app:assembleDebug` |
| release | Выпуск | нет | есть | нет, подписывается вручную | `./gradlew :app:assembleRelease` |
| releaseDebuggable | Проверка поведения релиза с отладкой | нет | нет | отладочный ключ | `./gradlew :app:assembleReleaseDebuggable` |

Демо-режим включается для любого варианта свойством `-Pfingame.demoMode=true` (например, релиз для
показа жюри: `./gradlew :app:assembleRelease -Pfingame.demoMode=true`). Подробности, пути к APK и
остальные различия — [build.md](docs/build.md).

## Документация

| Файл | О чём |
|---|---|
| [docs/README.md](docs/README.md) | Оглавление и соответствие требованиям ТЗ к документации |
| [docs/build.md](docs/build.md) | Окружение, варианты сборки, релизный APK, тесты |
| [docs/architecture.md](docs/architecture.md) | Архитектура, экраны, жизненный цикл данных, структура данных |
| [docs/pet-growth.md](docs/pet-growth.md) | Рост питомца: все правила и формулы |
| [docs/animals.md](docs/animals.md) | Животные; как добавить новое |
| [docs/items.md](docs/items.md) | Предметы, магазин, инвентарь; как добавить новый предмет |
| [docs/quests.md](docs/quests.md) | Квесты; как добавить новый квест |
| [docs/scene.md](docs/scene.md) | Игровая область: размеры, слои, координаты, жесты |
| [docs/economy.md](docs/economy.md) | Деньги, бюджет, вклад, журнал |
| [docs/education.md](docs/education.md) | Образовательный контент, подсказки, словарик |
| [docs/ux-accessibility.md](docs/ux-accessibility.md) | UX/UI, доступность, разрешения, данные, удаление профиля |
| [docs/testing.md](docs/testing.md) | Тесты и ручные сценарии |
| [docs/requirements.md](docs/requirements.md) | Матрица соответствия требованиям |
| [docs/licenses.md](docs/licenses.md) | Лицензии |
| [docs/roadmap.md](docs/roadmap.md) | Ограничения и план развития |

Word-версия собирается командой `python3 docs/tools/make_docx.py` (нужен pandoc 3.x).

## Лицензии

Все ассеты игры (спрайты, текстуры интерфейса, фоны, иконки, звуки) сделаны командой, если не
указано иное. Лицензии чужих работ, подключённых как ресурсы Android, лежат в
`app/src/main/res/raw/` (шрифт Press Start 2P — SIL Open Font License 1.1,
`press_start_2p_regular.txt`). Лицензии и авторство остальных чужих ассетов указаны в файлах
`attributions.xml` рядом с самими ассетами — сейчас это фоновая музыка
(`assets/audio/music/background/`, CC0-1.0) и звуки питомца (`assets/audio/sounds/animal/`,
CC0-1.0, CC-BY-3.0, CC-BY-4.0). Библиотеки AndroidX, Jetpack Compose и Coil — Apache License 2.0.
Подробно — [docs/licenses.md](docs/licenses.md).
