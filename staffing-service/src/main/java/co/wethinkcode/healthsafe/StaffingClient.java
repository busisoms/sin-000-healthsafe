package co.wethinkcode.healthsafe;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

public class StaffingClient {
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String wardUrl;
    private final String levelUrl;


    public StaffingClient(String wardUrl, String levelUrl) {
        this.wardUrl = wardUrl;
        this.levelUrl = levelUrl;
    }


    public Ward fetchWard(String id){
        HttpRequest request = request(wardUrl + "/wards/" +  id);

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
            throw new UpstreamServiceException(e.getMessage());
        }

    }

    public int fetchCurrentLevel(){
        HttpRequest request = request(levelUrl + "/alert-level");

        try {
            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            Map<String, Object> body = objectMapper.readValue(response.body(), Map.class);

            return objectMapper.convertValue(body.get("level"), Integer.class);

        } catch (IOException | InterruptedException e) {
            throw new UpstreamServiceException(e.getMessage());
        }

    }

    private HttpRequest request(String url) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();
        return request;
    }

}
