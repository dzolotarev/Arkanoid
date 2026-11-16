# ⚡ Arkanoid v1.0.0

Классическая игра **Arkanoid** на Java с неоновой графикой, частицами и системой уровней.

## Возможности

- 🎨 Неоновая визуальная стилизация — свечение, градиенты, анимированный фон со звёздами
- ✨ Система частиц — взрывы при разрушении кирпичей, искры при ударах
- 🔥 След мяча (trail)
- 📈 Система уровней — после уничтожения всех кирпичей скорость увеличивается
- 💪 Кирпичи с 2 HP на верхних рядах (начиная со 2-го уровня)
- 🏆 Рекорд (high score) в рамках сессии
- 🎯 Угол отскока зависит от места удара по платформе

## Системные требования

- Linux x86_64
- Или: `java 17+` для запуска JAR-версии

## Сборки

| Файл | Описание | Размер |
|------|----------|--------|
| `arkanoid-linux.tar.gz` | Нативный ELF-бинарник со встроенной JVM (v1.0.0) | 31 МБ |
| `src/` + Gradle | Исходники для сборки | — |

## Установка

```bash
tar xzf arkanoid-linux.tar.gz
```

## Запуск

### Нативная сборка (рекомендуется)

```bash
./arkanoid-linux/bin/Arkanoid
```

### JAR-сборка

```bash
java -jar arkanoid.jar
```

## Управление

| Клавиша | Действие |
|---------|----------|
| ← / → | Движение платформы |
| P / Esc | Пауза |
| Space | Рестарт |

## Сборка из исходников

```bash
javac -d build/classes $(find src/main/java -name "*.java")
jar cfe arkanoid.jar com.arcanoid.Main -C build/classes .
java -jar arkanoid.jar
```

Или через jpackage для нативного бинарника:

```bash
mkdir -p deploy-input && cp arkanoid.jar deploy-input/
jpackage --type app-image \
  --name Arkanoid \
  --input deploy-input \
  --main-jar arkanoid.jar \
  --main-class com.arcanoid.Main \
  --app-version 1.0 \
  --module-path $(dirname $(readlink -f $(which java)))/jmods \
  --add-modules java.desktop,java.base,java.logging,java.management,java.sql,java.xml,jdk.unsupported
```

## Структура проекта

```
arkanoid/
├── build/dist/             # готовые сборки (вне git)
├── src/main/java/com/arkanoid/
│   ├── Main.java           # точка входа
│   ├── GameFrame.java      # окно JFrame
│   └── GamePanel.java      # игровая логика и рендеринг
├── LICENSE                 # MIT
├── build.gradle.kts        # конфигурация Gradle
└── settings.gradle.kts     # имя проекта
```

## License

MIT — подробности в [LICENSE](LICENSE).
