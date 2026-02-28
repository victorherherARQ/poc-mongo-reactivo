package com.example.pocmongoreactivo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Clase principal para el arranque de la aplicación Spring Boot.
 * Inicializa el contexto de Spring y configura el soporte para MongoDB Reactivo.
 */
@SpringBootApplication
public class PocMongoReactivoApplication {

    public static void main(String[] args) {
        SpringApplication.run(PocMongoReactivoApplication.class, args);
    }
}
