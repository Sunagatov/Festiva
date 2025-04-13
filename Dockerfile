# 1. Сборочный этап: используем Maven + JDK 21
FROM maven:3.9.6-eclipse-temurin-21 AS build

# 2. Устанавливаем рабочую директорию
WORKDIR /app

# 3. Копируем проект и собираем jar-файл
COPY . /app
RUN mvn clean package -DskipTests

# 4. Финальный образ: только JRE + приложение
FROM eclipse-temurin:21-jre

# 5. Рабочая директория в контейнере
WORKDIR /app

# 6. Копируем собранный jar-файл (без суффикса "shaded")
COPY --from=build /app/target/Festiva-1.0-SNAPSHOT.jar /app/festiva.jar

# 7. Переменные среды, если нужно
ENV JAVA_OPTS=""

# 8. Точка входа
CMD java $JAVA_OPTS -jar festiva.jar
