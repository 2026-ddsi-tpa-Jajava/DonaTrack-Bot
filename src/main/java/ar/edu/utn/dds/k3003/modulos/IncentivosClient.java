package ar.edu.utn.dds.k3003.modulos;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class IncentivosClient {

    private final RestClient restClient;

public IncentivosClient(@Value("${incentivos.url:https://incentivos-yuse.onrender.com}") String baseUrl) {
        String urlValida = (baseUrl != null) ? baseUrl : "https://incentivos-yuse.onrender.com";
        this.restClient = RestClient.create(urlValida);
    }

    public String consultarEstado(String donadorID) {
        try {
            return restClient.get()
                    .uri("/donadores/{id}/estado", donadorID)
                    .retrieve()
                    .body(String.class);
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 404) {
                return "No encontramos estadísticas para ese donador.";
            }
            return "Error al consultar la API de Incentivos: " + e.getStatusCode().value();
        } catch (Exception e) {
            return "No nos pudimos comunicar con el módulo de Incentivos.";
        }
    }
}