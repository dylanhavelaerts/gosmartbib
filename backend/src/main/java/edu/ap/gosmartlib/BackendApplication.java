package edu.ap.gosmartlib;

import java.time.Duration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@EnableScheduling
@EnableAsync
public class BackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }

/**
 * Configureer een  timeout voor REST API calls.
 * Dit zorgt ervoor dat als een externe service niet binnen de opgegeven tijd reageert,
 * de call wordt afgebroken en een foutmelding wordt teruggegeven.
 * Dit voorkomt dat je applicatie vastloopt bij het wachten op een reactie van een trage of niet-beschikbare service.
 */
    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(5000));
        factory.setReadTimeout(Duration.ofMillis(15000));
        return new RestTemplate(factory);
    }

}
