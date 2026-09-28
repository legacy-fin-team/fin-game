# Животные

Виды питомцев, окрасы, этапы роста, спрайты, поглаживание и звуки. В конце — пример добавления
нового животного.

Пути к исходникам даны относительно `app/src/main/java/com/legacy/fingame/`, пути к данным —
относительно `app/src/main/assets/`.

Связанные разделы: [правила роста](pet-growth.md), [одежда на питомце](items.md),
[игровая область и слои](scene.md), [лицензии звуков](licenses.md).

## 1. Модель и каталог

| Класс | Файл | Что делает |
|---|---|---|
| `Animal` | `game/animals/Animal.kt` | Вид: `id`, `name`, `ageCount` (число этапов), `variants` (окрас → папка спрайтов); `coerceAge`, `getIdleSpritePath` |
| `AnimalReader` | `game/animals/AnimalReader.kt` | Читает `assets/data/animals.xml` |
| `AnimalRegistry` | `game/animals/AnimalRegistry.kt` | Каталог видов, создаётся при старте в `FinGameApplication` |
| `AnimalSelection` | `game/animals/AnimalSelection.kt` | Выбор игрока: `animalId` + `variantId` |
| `Growth` | `game/animals/Growth.kt` | Этап из накопленного роста (`ageOf`) |

`assets/data/animals.xml`:

```xml
<animal id="cat" name="Кот" ages="3">
    <variants path="animals/cat/">
        <variant id="orange" />
        <variant id="white" />
    </variants>
</animal>
<animal id="fish" name="Рыба" ages="3">
    <variants path="animals/fish/">
        <variant id="golden" />
    </variants>
</animal>
```

| Атрибут / тег | Смысл |
|---|---|
| `id` | Идентификатор вида; входит в имена спрайтов одежды (`equipped-<id>-<этап>.webp`) и звуков (`<id>1.ogg`) |
| `name` | Название для игрока |
| `ages` | Сколько этапов роста нарисовано (этапы `0 … ages − 1`); если атрибута нет — 1 (`Animal.DEFAULT_AGE_COUNT`) |
| `<variants path>` | Папка окрасов относительно `assets/textures/` |
| `<variant id>` | Окрас; спрайты лежат в `<path>/<id>/` |

Сейчас в игре:

| Животное | `id` | Окрасы | Этапов |
|---|---|---|---|
| Кот | `cat` | `orange`, `white` | 3 |
| Рыба | `fish` | `golden` | 3 |

`AnimalReader` пропускает животное с ошибкой (нет `id` или `name`, повтор `id`, неверный `ages`, нет
`<variants>` или ни одного `<variant>`) с сообщением в logcat. Если не прочиталось ни одного
животного, экран выбора показывает «Выбирать не из кого» с объяснением, а не падает.

## 2. Выбор питомца

Экран `ui/screens/AnimalSelectScreen.kt` показывается, пока питомец не выбран. Два шага: вид и окрас,
затем имя (до 20 символов, `MaxPetNameLength`; пробелы по краям обрезаются, имя можно не вводить).
Выбор сохраняется сразу (`GameViewModel.selectAnimal`): питомец начинает с этапа 0 и полными
шкалами, с этого момента отсчитывается его жизнь (`petBornAtMillis`).

Если сохранённого вида или окраса больше нет в данных (переименовали или удалили), игрок выбирает
заново и видит «Выбери питомца заново» с пояснением, что питомец не потерян, а обновилась игра.

## 3. Спрайты и этапы

Спрайт питомца — `textures/<path>/<окрас>/<этап>/idle.webp`, 32×32 пикселя
(`Animal.IDLE_SPRITE_FILE`, `Animal.getIdleSpritePath`), например
`animals/cat/orange/2/idle.webp`.

- Этап питомца считает `Growth.ageOf(care.growthMillis)` без верхней границы; `Animal.coerceAge`
  ограничивает его диапазоном `0 … ages − 1`, поэтому после последнего нарисованного этапа
  питомец остаётся на нём. Правила роста — [pet-growth.md](pet-growth.md).
- Одежда рисуется тем же ограниченным этапом, что и сам питомец (см. [items.md](items.md)).
- Спрайты загружает `utils/SpriteLoader.kt` (через Coil `ImageRequest`) из `assets/textures/`. Если
  файла нет или он не декодируется, рисуется `textures/error/error.webp`, а в logcat пишется путь.
- Покадровой анимации у питомца в текущей версии нет: на каждый этап — один кадр `idle.webp`.
  Переключателя «Анимации» в настройках тоже нет. Движение на сцене — сердечки при поглаживании
  (раздел 4), переходы между экранами (плавное появление) и перемещение/масштаб сцены жестами (см.
  [scene.md](scene.md)).

Питомец стоит в середине комнаты, опущенный на пол на 16 пикселей рисунка
(`SceneViewport.PET_FLOOR_SHIFT_PX`), и занимает четверть стороны сцены (`GameLayer.ANIMAL`,
32 из 128 пикселей). Подробнее — [scene.md](scene.md).

## 4. Поглаживание и звуки

Касание питомца на сцене — это «погладить» (`game/scene/PetTouch.kt`, `ui/screens/MainScreen.kt`):

- над головой поднимаются три сердечка (`HeartBurst.HEARTS = 3`, спрайт `textures/fx/heart.webp`
  8×8); поглаживание засчитывается не чаще раза в 400 мс (`PetTouchController.MIN_TAP_INTERVAL_MILLIS`);
- высота головы, от которой поднимаются сердечки, задана по этапам: строки 11, 3, 0 спрайта
  (`HeartBurst.HEAD_TOP_ROWS`); у этапа старше последнего известного берётся последнее значение;
- играет случайный звук этого животного (`AudioManager.playAnimalSound`);
- шкалы и деньги поглаживание не меняет.

Звуки лежат в `assets/audio/sounds/animal/` (OGG). `AudioManager` при старте загружает все `.ogg` из
этой папки; ключ звука — имя файла без расширения. Звуки животного — ключи вида
`<id животного><цифры>` (`AudioManager.animalSoundKeys`): у кота `cat1.ogg`, `cat2.ogg`. У животного
без своих звуков играет запасной звук поглаживания `pat1.ogg` или `pat2.ogg` (ключ `SOUND_PAT`,
то же правило «`pat` + цифры»). Сейчас своих звуков у рыбы нет — у неё играют `pat1`/`pat2`.

Громкость звуков задаётся ползунком «Звуки» в настройках (`GameSettings.soundVolume`, 0 —
выключено), см. [ux-accessibility.md](ux-accessibility.md).

## 5. Пример: как добавить новое животное

Добавим собаку `dog` с двумя окрасами и тремя этапами.

1. **Данные.** В `app/src/main/assets/data/animals.xml`:

   ```xml
   <animal id="dog" name="Пёс" ages="3">
       <variants path="animals/dog/">
           <variant id="brown" />
           <variant id="black" />
       </variants>
   </animal>
   ```

2. **Спрайты питомца** (WebP 32×32, прозрачный фон, лапы на той же строке, что у кота, — питомца
   опускают на пол на фиксированные 16 пикселей рисунка):

   ```
   app/src/main/assets/textures/animals/dog/brown/0/idle.webp
   app/src/main/assets/textures/animals/dog/brown/1/idle.webp
   app/src/main/assets/textures/animals/dog/brown/2/idle.webp
   app/src/main/assets/textures/animals/dog/black/0/idle.webp   … и так же для black
   ```

3. **Одежда на новом животном.** Для каждого варианта каждой одежды из `items.xml` (`bow`, `tie`,
   `bowtie`) нужны `equipped-dog-0.webp`, `equipped-dog-1.webp`, `equipped-dog-2.webp` в папке
   варианта, например `items/bow/red/equipped-dog-1.webp`. Без них на собаке вместо одежды будет
   заглушка. Декору (`placed.webp`) ничего добавлять не нужно. Подробно — [items.md](items.md).

4. **Звуки (необязательно).** `app/src/main/assets/audio/sounds/animal/dog1.ogg`, `dog2.ogg` —
   будут играть при поглаживании; без них играют `pat1`/`pat2`. Для чужого звука добавить запись в
   `attributions.xml` в той же папке (формат — [licenses.md](licenses.md)).

5. **Высота головы для сердечек.** `HeartBurst.HEAD_TOP_ROWS` общий для всех животных (подобран под
   кота). Если голова нового животного на своих этапах заметно выше или ниже, сердечки будут
   начинаться не над головой — тогда это значение нужно сделать зависящим от животного
   (изменение кода в `game/scene/PetTouch.kt`).

6. **Тесты.** `AnimalReaderTest` и `AnimalRegistryTest` проверяют правила чтения на встроенных
   примерах; отдельной проверки поставляемого `animals.xml` нет. Звуки проверяет
   `PetTouchAssetsTest` (`an animal's sounds are its id followed by digits only`). Запуск —
   `./gradlew :app:testDebugUnitTest`, затем в игре: «Настройки» → «Сбросить прогресс» →
   выбрать собаку.

Код для нового вида менять не нужно: экран выбора, сцена, рост, одежда и звуки берут всё из данных.
