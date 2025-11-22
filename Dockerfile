# Usar imagen base de Eclipse Temurin (OpenJDK 17) con Maven
FROM maven:3.9-eclipse-temurin-17 as build

# Establecer directorio de trabajo
WORKDIR /app

# Copiar archivos de configuración de Maven
COPY pom.xml .
COPY src ./src

# Construir el proyecto con Maven
RUN mvn clean package -DskipTests

# Segunda etapa para la imagen final
FROM eclipse-temurin:17-jre-jammy

# Establecer directorio de trabajo
WORKDIR /app

# Copiar el archivo JAR desde la etapa de construcción
COPY --from=build /app/target/supabase-auth-java-0.0.1-SNAPSHOT.jar app.jar

# Exponer el puerto (Render asigna el puerto dinámicamente)
EXPOSE 8081

# Variables de entorno para producción
ENV JAVA_OPTS="-Xmx512m -Xms256m"
ENV SPRING_PROFILES_ACTIVE=prod

# Comando para ejecutar la aplicación
CMD ["sh", "-c", "java $JAVA_OPTS -Dserver.port=$PORT -jar app.jar"]