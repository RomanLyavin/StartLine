# StartLine

План тренировок (бег и вело) с ИИ, подводкой к нужным соревнованиям и моделью athlete.

Версия 1.0.4 (versionCode 104).

## Возможности

- Синхронизация тренировок, самочувствия и профиля с intervals.icu
- **Признак «гонка»** у тренировок: старты определяются по типу/названию активности и выделяются в списке и карточке
- **OAuth-подключение часов**: Amazfit / Zepp, Garmin Connect, Huawei Health, Polar Flow.
  Токены шифруются через Android Keystore, callback приходит по `startline://oauth/watch`
- **Тейпер**: план подводки к ближайшему старту — 6 ступеней с рекомендациями по объёму и интенсивности
- **Расписание тренировок**: дней в неделю, дни для длительной, интервальной и силовой,
  частота силовых (каждую неделю, раз в 2 недели и т. д.)
- **Письмо с паролем при регистрации** — реальная отправка по SMTP (EHLO, STARTTLS,
  AUTH LOGIN) без сторонних библиотек
- Расчёт ctl/atl, готовности, пульсовых зон и нагрузки
- ИИ-тренер (OpenRouter / DeepSeek) с встроенным резервным анализом
- Календарь стартов и генерация плана на 4 недели
- Экран «О программе»: разработчик, поддержка проекта, проверка обновлений на GitHub
- Резервная копия настроек (JSON, экспорт/импорт)

## Изменения 1.0.4

- Отправка письма с данными для входа при регистрации (`data/Mailer.kt`,
  `data/WelcomeMail.kt`), тесты `MailerTest` и `MailSenderTest`
- Настройки SMTP читаются из `mail.properties` (файл не коммитится) и попадают
  в `BuildConfig`: `MAIL_HOST`, `MAIL_PORT`, `MAIL_USER`, `MAIL_PASSWORD`,
  `MAIL_FROM`, `MAIL_STARTTLS`
- Регистрация не отменяется, если SMTP недоступен: аккаунт создаётся, а
  приложение сообщает, что письмо не отправлено
- Кнопки «О программе» и «Поддержать» на главном экране
- Загрузка данных при старте перенесена с главного потока — из-за неё экран
  зависал на телефоне с уже созданным аккаунтом

## Настройка почты

`mail.properties` в корне проекта (не попадает в git):

```properties
host=smtp.example.com
port=587
user=bot@example.com
password=<пароль приложения>
from=bot@example.com
starttls=true
```

Если файл пустой, регистрация работает, но письмо не отправляется.

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
`StartLine-1.0.4.apk`. Для подписи используется `startline-release.jks` и
`keystore.properties` (не попадает в git).

## Проверка APK 1.0.4

- Файл: `StartLine-1.0.4.apk` (10 625 261 байт)
- SHA-256: `CFD39536AB127C51138BEE9663FDC27769B620FE36BE61ABFA0865BDD470E8C6`
- `aapt2 dump badging`: package `com.startline.app`, versionCode 104, versionName 1.0.4, targetSdk 34
- `zipalign -c -v 4`: Verification successful
- Подпись: v2-схема, сертификат SHA-256 `dc511246a66e424fd47ae2a0e3a723a32f1bd3a7c5cf8eacd86bb1aff953960e`
  (тот же, что у 1.0.0–1.0.3 — обновление установится поверх установленной версии)
- Тесты: 38 unit-тестов, все пройдены (`app/src/test/java/...`)
- Отправка письма проверена на эмуляторе: приложение подключилось к SMTP-серверу,
  выполнило EHLO, AUTH LOGIN, MAIL FROM, RCPT TO, DATA и передало тело письма
  с паролем (лог сервера: `emulator-check/smtp-server.log`)

## GitHub

Релиз с APK: https://github.com/RomanLyavin/StartLine/releases/tag/v1.0.4

## Важно про OAuth

В `app/src/main/java/com/startline/app/data/WatchIntegrations.kt` содержатся демонстрационные
`clientId`/`clientSecret`. Для реальной авторизации нужно зарегистрировать приложение
у каждого производителя часов и заменить значения на выданные ключи, а также указать
`redirect_uri = startline://oauth/watch` в настройках OAuth-приложения.
