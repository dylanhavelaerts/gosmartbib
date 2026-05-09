package edu.ap.gosmartlib;

import java.time.Duration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
<<<<<<< HEAD
import org.springframework.http.client.SimpleClientHttpRequestFactory;
=======
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
>>>>>>> 8f458e69877bb36fc3c9706148ae8c6a3dea5210
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
<<<<<<< HEAD
 * Configureer een  timeout voor REST API calls.
 * Dit zorgt ervoor dat als een externe service niet binnen de opgegeven tijd reageert,
 * de call wordt afgebroken en een foutmelding wordt teruggegeven.
=======
 * Configureer een  timeout voor REST API calls. 
 * Dit zorgt ervoor dat als een externe service niet binnen de opgegeven tijd reageert, 
 * de call wordt afgebroken en een foutmelding wordt teruggegeven. 
>>>>>>> 8f458e69877bb36fc3c9706148ae8c6a3dea5210
 * Dit voorkomt dat je applicatie vastloopt bij het wachten op een reactie van een trage of niet-beschikbare service.
 */
    @Bean
    public RestTemplate restTemplate() {
<<<<<<< HEAD
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(5000));
        factory.setReadTimeout(Duration.ofMillis(15000));
=======
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(15000);
>>>>>>> 8f458e69877bb36fc3c9706148ae8c6a3dea5210
        return new RestTemplate(factory);
    }

}
