package co.wethinkcode.healthsafe;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

public class StaffingClient {
    private static final HttpClient httpClient = HttpClient.newHttpClient();
    private static final ObjectMapper objectMapper = new ObjectMapper();


    public static Ward getWard(String id){
        String URL = "http://localhost:7031/wards/" + id;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(URL))
                .GET()
                .build();

        try {
            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            Map<String, Object> body = objectMapper.readValue(response.body(), Map.class);

            if (body.containsKey("error")){
                throw new WardNotFoundException((String) body.get("error"));
            }

            Map<String, Object> rawWard = (Map<String, Object>) body.get("data");

            return objectMapper.convertValue(rawWard, Ward.class);

        } catch (IOException | InterruptedException e) {
            throw new UpStreamServiceException(e.getMessage());
        }

    }

    public static int alertLevel(){
        String URL = "http://localhost:7032/alert-level";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(URL))
                .GET()
                .build();

        try {
            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            Map<String, Object> body = objectMapper.readValue(response.body(), Map.class);

            return objectMapper.convertValue(body.get("level"), Integer.class);

        } catch (IOException | InterruptedException e) {
            throw new UpStreamServiceException(e.getMessage());
        }

    }
}
