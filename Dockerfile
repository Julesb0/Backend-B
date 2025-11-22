# Usar imagen base de Eclipse Temurin (OpenJDK 17)
FROM eclipse-temurin:17-jre-jammy

# Establecer directorio de trabajo
WORKDIR /app

# Copiar el archivo JAR
COPY target/supabase-auth-java-0.0.1-SNAPSHOT.jar app.jar

# Exponer el puerto (Render asigna el puerto dinámicamente)
EXPOSE 8081

# Variables de entorno para producción
ENV JAVA_OPTS="-Xmx512m -Xms256m"
ENV SPRING_PROFILES_ACTIVE=prod

# Comando para ejecutar la aplicación
CMD ["sh", "-c", "java $JAVA_OPTS -Dserver.port=$PORT -jar app.jar"]