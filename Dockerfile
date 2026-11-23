# Multi-stage Dockerfile for the CargaImagenes Spring Boot application
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /build

# Copia sólo los archivos necesarios para aprovechar la cache de capas
COPY pom.xml ./
COPY src ./src

# Construye el jar (sin tests para acelerar)
RUN mvn -B -DskipTests package

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Copia el artefacto generado
COPY --from=build /build/target/*.jar app.jar

# Puerto por defecto de Spring Boot
EXPOSE 8080

# Punto de montaje del volumen de imágenes del NAS
VOLUME ["/volume1/homes"]

# Variables de entorno que la app puede leer
ENV UPLOAD_DIR=/volume1/homes
ENV LOGGING_FILE_NAME=/volume1/homes/admin/uploadImages/carga-imagenes.log

ENTRYPOINT ["java","-jar","/app/app.jar"]
