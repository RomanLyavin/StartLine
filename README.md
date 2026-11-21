# StartLine

План тренировок (бег и вело) с ИИ, подводкой к нужным соревнованиям и моделью athlete.

Версия 1.0.1 (versionCode 101).

## Возможности

- Синхронизация тренировок, самочувствия и профиля с intervals.icu
- **Признак «гонка»** у тренировок: старты определяются по типу/названию активности и выделяются в списке и карточке
- **OAuth-подключение часов**: Amazfit / Zepp, Garmin Connect, Huawei Health, Polar Flow.
  Токены шифруются через Android Keystore, callback приходит по `startline://oauth/watch`
- **Тейпер**: план подводки к ближайшему старту — 6 ступеней с рекомендациями по объёму и интенсивности
- Расчёт_ctl/atl, готовности, пульсовых зон и нагрузки
- ИИ-тренер (OpenRouter / DeepSeek) с встроенным резервным анализом
- Календарь стартов и генерация плана на 4 недели

## Сборка

```powershell
$env:JAVA_HOME = "E:\Ai\OpenCode_APK\.tools\jdk\jdk-17.0.21+9"
$env:ANDROID_HOME = "E:\Ai\OpenCode_APK\.tools\android-sdk"
$env:ANDROID_SDK_ROOT = $env:ANDROID_HOME
& "E:\Ai\OpenCode_APK\.tools\gradle\gradle-8.9\bin\gradle.bat" :app:assembleRelease --console=plain
```

Готовый APK: `app/build/outputs/apk/release/app-release.apk`.
Для подписи используется `startline-release.jks` и `keystore.properties` (не committing в git).

## GitHub

Релиз с APK: https://github.com/RomanLyavin/StartLine/releases/tag/v1.0.1

## Важно про OAuth

В `app/src/main/java/com/startline/app/data/WatchIntegrations.kt` содержатся демонстрационные
`clientId`/`clientSecret`. Для реальной авторизации нужно зарегистрировать приложение
у каждого производителя часов и заменить значения на выданные ключи, а также указать
`redirect_uri = startline://oauth/watch` в настройках OAuth-приложения.
