# Финансовый Питомец

Мобильная игра для Android, которая учит ребёнка обращаться с деньгами. Ребёнок заводит питомца и
заботится о нём, получает монеты раз в день, планирует бюджет (обязательные и необязательные траты,
сбережения, вклад), копит на цели и принимает решения в квестах. Взрослый за простым замком видит
прогресс ребёнка по темам финансовой грамотности, проверяет задания и добавляет свои квесты и
награды.

Приложение полностью автономное: сервера, учётных записей и сетевых запросов нет, все данные
хранятся на устройстве.

- Платформа: Android 8.0+ (minSdk 26), targetSdk 37; пакет `com.legacy.fingame`, версия 1.0 (1). Название приложения — «Финансовый Питомец»; пакет, каталоги и имя Word-файла сохраняют прежнее короткое имя `fingame`.
- Стек: Kotlin, Jetpack Compose (Material 3), пиксельная графика, данные игры в XML.
- Язык интерфейса: русский.

Полная документация — [docs/README.md](docs/README.md); Word-версия для сдачи —
`docs/FinGame-Документация.docx`.

## Что умеет игра

| Возможность | Подробнее |
|---|---|
| Выбор питомца (кот двух окрасов, плавающая золотая рыбка) и имени | [animals.md](docs/animals.md) |
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
| Квесты: шесть историй с выбором, в том числе трёхдневный «Пикник»; случайные события, кулдауны | [quests.md](docs/quests.md) |
| Подсказки по экранам при первом входе и словарик «Помощь» | [education.md](docs/education.md) |
| Взрослый режим: замок, прогресс по темам без оценок, отчёты по дням, покупкам и квестам, проверка заданий, изменение монет с причиной, свои товары, награды и квесты | [quests.md](docs/quests.md), [education.md](docs/education.md) |
| Настройки: громкость звуков и музыки, анимации, тема (светлая / тёмная / авто), сброс прогресса | [ux-accessibility.md](docs/ux-accessibility.md) |
| Демо-сборка с кнопкой «+12 ч» для показа игры за несколько минут | [build.md](docs/build.md) |

Соответствие разделу 2 ТЗ по каждому пункту со статусами — [tz-compliance.md](docs/tz-compliance.md).
Игра проверена на эмуляторе и на телефоне Redmi Note 13 Pro+ 5G с Android 16 ([testing.md](docs/testing.md)).

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
│       ├── test/                   юнит-тесты JUnit 4 (67 классов, 744 теста)
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

| Вариант | Для чего | Перемотка «+12 ч» и «Ещё раз» у квеста | Подпись | Команда |
|---|---|---|---|---|
| debug | Разработка и показ | есть | отладочный ключ | `./gradlew :app:assembleDebug` |
| release | Выпуск | нет | нет, подписывается вручную | `./gradlew :app:assembleRelease` |
| releaseDebuggable | Проверка поведения релиза с отладкой | нет | отладочный ключ | `./gradlew :app:assembleReleaseDebuggable` |

Кулдауны и паузы квестов действуют во всех вариантах; в демо-сборке их пропускает «+12 ч».

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
| [docs/testing.md](docs/testing.md) | Тесты, ручные сценарии, проверка на телефоне |
| [docs/tz-compliance.md](docs/tz-compliance.md) | Соответствие разделу 2 ТЗ: пункты со статусами, модулями и тестами |
| [docs/roadmap.md](docs/roadmap.md) | Ограничения и план развития |

Word-версия собирается командой `python3 docs/tools/make_docx.py` (нужен pandoc 3.x).

## Лицензии

Перечень сторонних библиотек, шрифтов, изображений и звуков с их лицензиями (п. 5.12 ТЗ).

### Правило для ассетов

**Все ассеты игры — спрайты, текстуры интерфейса, фоны, иконки, звуки и музыка — сделаны командой,
если рядом с ними не указано иное.** Работы других авторов лежат в репозитории вместе с лицензией:

- лицензии чужих работ, подключённых как ресурсы Android (шрифт), — в `app/src/main/res/raw/` рядом
  с ресурсом;
- лицензии и авторство остальных чужих ассетов (звуков, музыки, текстур) — в файле
  `attributions.xml` в той же папке, где лежат сами ассеты. Файл попадает в APK вместе с ними.

Всё, что лежит в папке без `attributions.xml` или не упомянуто в нём, — работа команды.

Формат `attributions.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<attributions>
    <attribution>
        <files>
            <file>cat1.ogg</file>
        </files>
        <license>CC-BY-3.0</license>
        <copyright>Modified from 'Meow 4.wav' by freesound user 'TRNGLE'. …</copyright>
        <source>https://freesound.org/people/TRNGLE/sounds/368006/</source>
    </attribution>
</attributions>
```

Одна запись `<attribution>` — группа файлов с одним автором и лицензией: `<file>` — имена файлов в
той же папке, `<license>` — лицензия в виде идентификатора SPDX (`CC0-1.0`, `CC-BY-3.0`,
`CC-BY-4.0`, …), `<copyright>` — автор, исходное название и что изменено, `<source>` — ссылка на
оригинал. Добавляя чужой ассет, нужно дописать запись в `attributions.xml` его папки (или создать
файл) и убедиться, что лицензия разрешает распространять его в составе приложения.

### Чужие ассеты

Файлы `attributions.xml` есть в двух папках; других чужих ассетов в репозитории нет.

**Фоновая музыка** — `app/src/main/assets/audio/music/background/attributions.xml`:

| Файл | Источник | Лицензия |
|---|---|---|
| `piano-208-octave-long.ogg` | freesound, josefpres, https://freesound.org/people/josefpres/sounds/853163/; сконвертирован из WAV в OGG | CC0-1.0 |
| `piano-214-octave-down-long.ogg` | freesound, josefpres, https://freesound.org/people/josefpres/sounds/870334/; сконвертирован в OGG | CC0-1.0 |
| `piano-214-octave-long.ogg` | freesound, josefpres, https://freesound.org/people/josefpres/sounds/866781/; сконвертирован в OGG | CC0-1.0 |

**Звуки питомца** — `app/src/main/assets/audio/sounds/animal/attributions.xml`:

| Файл | Источник | Лицензия |
|---|---|---|
| `cat1.ogg` | «Meow 4.wav», freesound, TRNGLE, https://freesound.org/people/TRNGLE/sounds/368006/; обрезан, моно, OGG | CC-BY-3.0 |
| `cat2.ogg` | freesound, TRNGLE, https://freesound.org/people/TRNGLE/sounds/362652/; моно, OGG | CC-BY-3.0 |
| `pat1.ogg` | «pop.ogg», mirrorcult (GitHub, space-station-14), https://github.com/space-wizards/space-station-14/blob/9168fc629c555b8c395d695d291faea1eeda1db6/Resources/Audio/Effects/pop.ogg | CC0-1.0 |
| `pat2.ogg` | «Pop, High, A (H1).wav», InspectorJ (jshaw.co.uk), https://freesound.org/people/InspectorJ/sounds/411642/; сведён в моно | CC-BY-4.0 |

Для CC-BY-3.0 и CC-BY-4.0 авторство указано в `attributions.xml` в составе APK и здесь; отдельного
экрана «Об авторах» в игре нет.

**Шрифт** — `app/src/main/res/font/press_start_2p_regular.ttf`: Press Start 2P, © 2012 The Press
Start 2P Project Authors (cody@zone38.net), зарезервированное имя «Press Start 2P». Лицензия — SIL
Open Font License 1.1, полный текст — `app/src/main/res/raw/press_start_2p_regular.txt`. Она
разрешает встраивать шрифт в приложение, в том числе коммерческое; нельзя продавать сам шрифт
отдельно и выпускать изменённую версию под тем же именем.

### Ассеты команды

| Ассеты | Путь |
|---|---|
| Спрайты животных (кот, рыбка; три этапа) | `app/src/main/assets/textures/animals/` |
| Иконки и спрайты предметов (еда, игрушки, одежда на питомце, декор) | `app/src/main/assets/textures/items/` |
| Фон комнаты | `app/src/main/assets/textures/locations/` |
| Иконки интерфейса, шкал, разделов магазина | `app/src/main/assets/textures/ui/`, `textures/shop/` |
| Сердечко, заглушка отсутствующего спрайта | `app/src/main/assets/textures/fx/`, `textures/error/` |
| Иконка приложения | `app/src/main/res/mipmap-*/`, `res/drawable/ic_launcher_*` |
| Название приложения (под значком, в списке приложений) | строка `app_name` в `app/src/main/res/values/strings.xml` |
| Тексты игры, подсказок, словарика, квестов | `app/src/main/assets/data/`, исходный код |

### Библиотеки

Версии — из `gradle/libs.versions.toml`.

| Библиотека | Артефакт | Версия | Лицензия | Где используется |
|---|---|---|---|---|
| AndroidX Core KTX | `androidx.core:core-ktx` | 1.19.0 | Apache-2.0 | приложение |
| AndroidX Activity Compose | `androidx.activity:activity-compose` | 1.13.0 | Apache-2.0 | приложение |
| AndroidX Lifecycle (runtime-ktx, runtime-compose, viewmodel-compose) | `androidx.lifecycle:*` | 2.11.0 | Apache-2.0 | приложение |
| Jetpack Compose BOM (ui, ui-graphics, ui-tooling-preview, material3) | `androidx.compose:compose-bom` | 2026.02.01 | Apache-2.0 | приложение |
| Coil 3 (compose, gif) | `io.coil-kt.coil3:coil-compose`, `coil-gif` | 3.6.2 | Apache-2.0 | загрузка спрайтов |
| Compose UI Tooling, UI Test Manifest | `androidx.compose.ui:ui-tooling`, `ui-test-manifest` | по BOM | Apache-2.0 | только debug |
| JUnit 4 | `junit:junit` | 4.13.2 | EPL-1.0 | юнит-тесты |
| AndroidX Test JUnit, Espresso, Compose UI Test | `androidx.test.ext:junit` 1.3.0, `espresso-core` 3.7.0, `ui-test-junit4` | см. каталог | Apache-2.0 | инструментальные тесты |
| Android Gradle Plugin | `com.android.application` | 9.4.0 | Apache-2.0 | сборка |
| Kotlin и Compose Compiler | `org.jetbrains.kotlin.plugin.compose` | 2.4.20 | Apache-2.0 | сборка |
| Foojay Toolchains Resolver | `org.gradle.toolchains.foojay-resolver-convention` | 1.0.0 | Apache-2.0 | сборка |

В APK попадают только библиотеки из строк «приложение» и «загрузка спрайтов» и их зависимости
AndroidX и Kotlin (Apache-2.0). Полный список —
`./gradlew :app:dependencies --configuration releaseRuntimeClasspath`.
