package com.etapa_productiva.kronos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling // 🔔 Habilita el job diario de alertas de visitas de seguimiento (VisitaAlertaService)
public class KronosApplication {

	public static void main(String[] args) {
		SpringApplication.run(KronosApplication.class, args);
	}

}
