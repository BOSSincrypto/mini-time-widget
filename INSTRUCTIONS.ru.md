# Mini Time Widget — инструкция по запуску

## Что это

Минималистичное Android-приложение: виджет 1x2 для главного экрана с двумя временами из разных часовых поясов. Настройка: часовые пояса, цвет текста, цвет и прозрачность фона, 5 шрифтов, 12/24-часовой формат. Тап по виджету открывает настройки.

Оптимизация:
- Ноль сторонних зависимостей (чистый Kotlin + Android SDK, без AndroidX/Compose)
- Ноль фоновой работы: время тикает через `TextClock` внутри `RemoteViews` — нет сервисов, будильников, `updatePeriodMillis=0` → нулевой расход батареи
- R8 minify + shrinkResources: release APK ~33 КБ

## Требования

- JDK 17
- Android SDK: platform android-34, build-tools 34.0.0 (Android Studio поставит всё сама)
- minSdk 26 (Android 8.0+)

## Сборка

### Вариант 1: Android Studio
1. Открыть папку проекта (File → Open)
2. Дождаться Gradle sync
3. Run ▶ или Build → Build APK(s)

### Вариант 2: командная строка
```bash
# укажите путь к Android SDK (или пропустите, если задан ANDROID_HOME)
echo "sdk.dir=/путь/к/android-sdk" > local.properties

./gradlew assembleDebug      # app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleRelease    # app/build/outputs/apk/release/ (unsigned, нужна подпись для установки)
```

Готовый собранный APK лежит в `prebuilt/app-debug.apk` — его можно сразу установить:
```bash
adb install prebuilt/app-debug.apk
```

## Использование

1. Долгий тап по главному экрану → «Виджеты»
2. Найти «Mini Time Widget» и перетащить на экран (занимает слот 2×1)
3. Откроется экран настроек: выбрать 2 часовых пояса, шрифт, цвета, прозрачность, формат времени → Save
4. Для изменения настроек — просто тапнуть по виджету

## Структура проекта

```
app/src/main/java/dev/minitime/widget/
  TimeWidgetProvider.kt    — AppWidgetProvider, строит RemoteViews из настроек
  WidgetConfigActivity.kt  — экран настроек с живым предпросмотром
  WidgetPrefs.kt           — хранение настроек (SharedPreferences, per-widget)
app/src/main/res/
  layout/widget_font_*.xml — 5 вариантов виджета (по одному на шрифт)
  layout/activity_config.xml
  xml/widget_info.xml      — метаданные виджета (1x2, resizable)
```

Проверено end-to-end на эмуляторе Android 14 (API 34): добавление виджета, настройка цветов/шрифта/формата, повторная настройка тапом, сохранение настроек после переустановки.
