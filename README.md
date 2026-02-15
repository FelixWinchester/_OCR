# OCR Application

> **Инструкция по сборке**
> 1. Требуется **JDK 17** или выше.
> 2. Клонирование репозитория: `git clone https://github.com/FelixWinchester/ваш_репозиторий.git`
> 3. Переход в директорию: `cd ваш_репозиторий`
> 4. Сборка проекта:
>    * **Windows:** `gradlew.bat assembleDebug`
>    * **Linux/macOS:** `./gradlew assembleDebug`
> 5. Путь к готовому APK: `app/build/outputs/apk/debug/`

---

## Patch 0.0.1: Инициализация проекта и базовый UI

В данном обновлении заложена основа архитектуры Android-приложения и реализован первичный графический интерфейс для модуля распознавания текста (OCR).

### Основные изменения
* **Структура проекта**: Сформирована иерархия пакетов. Подключены зависимости в каталоге `libs.versions.toml`.
* **Интерфейс пользователя**: Создан экран выбора изображений на Jetpack Compose. Реализована логика кнопки распознавания.
* **Ресурсы**: Настроена цветовая палитра и текстовые константы в `res/values`.
* **Технический стек**: Использование Kotlin DSL (`.gradle.kts`) для управления сборкой.

---

### Визуальные материалы
*Для просмотра в полном разрешении нажмите на изображение*

<div align="center">
  <table border="0">
    <tr>
      <td align="center" valign="bottom">
        <b>Интерфейс приложения</b>
        <br><br>
        <a href="screenshots/(2)patch0.0.1.png">
          <img src="screenshots/(2)patch0.0.1.png" width="210" alt="UI Screenshot">
        </a>
      </td>
      <td align="center" valign="bottom">
        <b>Структура проекта</b>
        <br><br>
        <a href="screenshots/(1)patch0.0.1.png">
          <img src="screenshots/(1)patch0.0.1.png" width="360" alt="Project Structure">
        </a>
      </td>
    </tr>
  </table>
</div>
