FROM maven:3.9.6-eclipse-temurin-17 as build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jdk
WORKDIR /app

# Créer le dossier config avant la copie
RUN mkdir -p config

# Copier le .jar et le fichier de config
COPY --from=build /app/target/*.jar app.jar
COPY --from=build /app/src/main/resources/application.properties ./config/application.properties

# Démarrage avec le bon chemin de configuration
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.config.location=file:./config/application.properties"]
