# StartLine

План тренировок (бег и вело) с ИИ, подводкой к нужным соревнованиям и моделью athlete.

Версия 1.0.3 (versionCode 103).

## Возможности

- Синхронизация тренировок, самочувствия и профиля с intervals.icu
- **Признак «гонка»** у тренировок: старты определяются по типу/названию активности и выделяются в списке и карточке
- **OAuth-подключение часов**: Amazfit / Zepp, Garmin Connect, Huawei Health, Polar Flow.
  Токены шифруются через Android Keystore, callback приходит по `startline://oauth/watch`
- **Тейпер**: план подводки к ближайшему старту — 6 ступеней с рекомендациями по объёму и интенсивности
- **Расписание тренировок**: дней в неделю, дни для длительной, интервальной и силовой,
  частота силовых (каждую неделю, раз в 2 недели и т. д.)
- Расчёт ctl/atl, готовности, пульсовых зон и нагрузки
- ИИ-тренер (OpenRouter / DeepSeek) с встроенным резервным анализом
- Календарь стартов и генерация плана на 4 недели
- Экран «О программе»: разработчик, поддержка проекта, проверка обновлений на GitHub
- Резервная копия настроек (JSON, экспорт/импорт)

## Изменения 1.0.3

- Настройка частоты силовых тренировок (пункт 1.3 плана доработок)
- Частота силовых попадает в резервную копию настроек
- `themes.xml`: `android:windowLightNavigationBar` вынесен в `values-v27` (lint `NewApi` при minSdk 26)
- Миграция базы данных до версии 4 (`profiles.strength_every_weeks`)

## Сборка

```powershell
$env:JAVA_HOME = "E:\Ai\OpenCode_APK\.tools\jdk\jdk-17.0.21+9"
$env:ANDROID_HOME = "E:\Ai\OpenCode_APK\.tools\android-sdk"
$env:ANDROID_SDK_ROOT = $env:ANDROID_HOME
& "E:\Ai\OpenCode_APK\.tools\gradle\gradle-8.9\bin\gradle.bat" :app:testDebugUnitTest --console=plain
& "E:\Ai\OpenCode_APK\.tools\gradle\gradle-8.9\bin\gradle.bat" :app:lintRelease --console=plain
& "E:\Ai\OpenCode_APK\.tools\gradle\gradle-8.9\bin\gradle.bat" :app:assembleRelease --console=plain
```

Готовый APK: `app/build/outputs/apk/release/app-release.apk`, копируется в
`StartLine-1.0.3.apk`. Для подписи используется `startline-release.jks` и
`keystore.properties` (не попадает в git).

## Проверка APK 1.0.3

- Файл: `StartLine-1.0.3.apk` (10 625 255 байт)
- SHA-256: `217AD2621FE4EA3F91C3AE981E6BD6AF3D7D1CD9705BCE4C914500CC992D8B07`
- `aapt2 dump badging`: package `com.startline.app`, versionCode 103, versionName 1.0.3, targetSdk 34
- `zipalign -c -v 4`: Verification successful
- Подпись: v2-схема, сертификат SHA-256 `dc511246a66e424fd47ae2a0e3a723a32f1bd3a7c5cf8eacd86bb1aff953960e`
  (тот же, что у 1.0.0, 1.0.1, 1.0.2 — обновление установится поверх установленной версии)
- Тесты: 30 unit-тестов, все пройдены (`app/src/test/java/...`)

## GitHub

Релизы: https://github.com/RomanLyavin/StartLine/releases
Релиз `v1.0.3` с APK `StartLine-1.0.3.apk` публикуется вручную (в этой папке нет
локального git-репозитория).

## Важно про OAuth

В `app/src/main/java/com/startline/app/data/WatchIntegrations.kt` содержатся демонстрационные
`clientId`/`clientSecret`. Для реальной авторизации нужно зарегистрировать приложение
у каждого производителя часов и заменить значения на выданные ключи, а также указать
`redirect_uri = startline://oauth/watch` в настройках OAuth-приложения.
