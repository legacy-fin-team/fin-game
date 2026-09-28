# Лицензии и сторонние компоненты

Связанные разделы: [животные и звуки](animals.md), [предметы и текстуры](items.md),
[сборка](build.md).

## 1. Правило для ассетов

**Все ассеты игры — спрайты, текстуры интерфейса, фоны, иконки, звуки и музыка — сделаны командой,
если рядом с ними не указано иное.**

Работы других авторов, которые используются в игре, сопровождаются лицензией в репозитории:

- **лицензии чужих работ, подключённых как ресурсы Android** (шрифт), лежат в
  `app/src/main/res/raw/` рядом с ресурсом;
- **лицензии и авторство остальных чужих ассетов** (звуков, музыки, текстур) указываются в файле
  `attributions.xml` в той же папке, где лежат сами ассеты. Файл попадает в APK вместе с ними.

Всё, что лежит в папке без `attributions.xml` или не упомянуто в нём, — работа команды.

## 2. Формат `attributions.xml`

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

| Тег | Смысл |
|---|---|
| `<attribution>` | Одна запись: группа файлов с одинаковым автором и лицензией |
| `<files>/<file>` | Имена файлов в той же папке |
| `<license>` | Лицензия в виде идентификатора SPDX (`CC0-1.0`, `CC-BY-3.0`, `CC-BY-4.0`, …) |
| `<copyright>` | Автор, исходное название и что изменено (обрезка, моно, конвертация) |
| `<source>` | Ссылка на оригинал |

Добавляя чужой ассет, нужно дописать запись в `attributions.xml` его папки (или создать файл,
если его там ещё нет) и убедиться, что лицензия разрешает распространение в составе приложения.

## 3. Чужие ассеты в игре

Файлы `attributions.xml` сейчас есть в двух папках; других чужих ассетов в репозитории нет.

**Фоновая музыка** — `app/src/main/assets/audio/music/background/attributions.xml`:

| Файл | Источник | Лицензия |
|---|---|---|
| `piano-208-octave-long.ogg` | freesound, пользователь josefpres, https://freesound.org/people/josefpres/sounds/853163/; сконвертирован из WAV в OGG | CC0-1.0 |
| `piano-214-octave-down-long.ogg` | freesound, josefpres, https://freesound.org/people/josefpres/sounds/870334/; сконвертирован в OGG | CC0-1.0 |
| `piano-214-octave-long.ogg` | freesound, josefpres, https://freesound.org/people/josefpres/sounds/866781/; сконвертирован в OGG | CC0-1.0 |

**Звуки питомца** — `app/src/main/assets/audio/sounds/animal/attributions.xml`:

| Файл | Источник | Лицензия |
|---|---|---|
| `cat1.ogg` | «Meow 4.wav», freesound, TRNGLE, https://freesound.org/people/TRNGLE/sounds/368006/; обрезан, моно, OGG | CC-BY-3.0 |
| `cat2.ogg` | freesound, TRNGLE, https://freesound.org/people/TRNGLE/sounds/362652/; моно, OGG | CC-BY-3.0 |
| `pat1.ogg` | «pop.ogg», mirrorcult (GitHub, space-station-14), https://github.com/space-wizards/space-station-14/blob/9168fc629c555b8c395d695d291faea1eeda1db6/Resources/Audio/Effects/pop.ogg | CC0-1.0 |
| `pat2.ogg` | «Pop, High, A (H1).wav», InspectorJ (jshaw.co.uk), https://freesound.org/people/InspectorJ/sounds/411642/; сведён в моно | CC-BY-4.0 |

Для CC-BY-3.0 и CC-BY-4.0 (`cat1.ogg`, `cat2.ogg`, `pat2.ogg`) авторство указано в
`attributions.xml` в составе APK и в этом документе; отдельного экрана «Об авторах» в игре нет.

**Шрифт** — `app/src/main/res/font/press_start_2p_regular.ttf`:

| Шрифт | Автор | Лицензия |
|---|---|---|
| Press Start 2P | © 2012 The Press Start 2P Project Authors (cody@zone38.net), зарезервированное имя «Press Start 2P» | SIL Open Font License 1.1; полный текст — `app/src/main/res/raw/press_start_2p_regular.txt` |

OFL разрешает встраивать шрифт в приложение, в том числе коммерческое; нельзя продавать сам шрифт
отдельно и выпускать изменённую версию под именем «Press Start 2P».

## 4. Ассеты команды

Без `attributions.xml`, то есть сделаны командой:

| Ассеты | Путь |
|---|---|
| Спрайты животных (кот, рыба; три этапа) | `app/src/main/assets/textures/animals/` |
| Иконки и спрайты предметов (еда, игрушки, одежда на питомце, декор) | `app/src/main/assets/textures/items/` |
| Фон комнаты | `app/src/main/assets/textures/locations/` |
| Иконки интерфейса, шкал, разделов магазина | `app/src/main/assets/textures/ui/`, `textures/shop/` |
| Сердечко, заглушка отсутствующего спрайта | `app/src/main/assets/textures/fx/`, `textures/error/` |
| Иконка приложения | `app/src/main/res/mipmap-*/`, `res/drawable/ic_launcher_*` |
| Тексты игры, подсказок, словарика, квестов | `app/src/main/assets/data/`, исходный код |

## 5. Библиотеки

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

В APK попадают только библиотеки из строк «приложение» и «загрузка спрайтов» и их транзитивные
зависимости AndroidX и Kotlin (Apache-2.0). Полный список:
`./gradlew :app:dependencies --configuration releaseRuntimeClasspath`.
