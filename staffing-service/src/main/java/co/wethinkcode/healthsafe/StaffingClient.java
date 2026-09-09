package co.wethinkcode.healthsafe;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

public class StaffingClient {
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

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

            int status = response.statusCode();

            if (status == 404) {
                throw new WardNotFoundException("Ward not found: "
                        + id + " at " + wardUrl);
            }

            if (status != 200) {
                throw new UpstreamServiceException("Service at " +
                        wardUrl + " returned error " + status);
            }

            Map<String, Object> body = objectMapper.readValue(response.body(), Map.class);
            Map<String, Object> rawWard = (Map<String, Object>) body.get("data");
            return objectMapper.convertValue(rawWard, Ward.class);
        } catch (IOException e) {
            throw new UpstreamServiceException("Network error calling Ward service at " + wardUrl, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new UpstreamServiceException("Interrupted while calling Ward service at " + wardUrl, e);
        }

    }

    public int fetchCurrentLevel(){
        HttpRequest request = request(levelUrl + "/alert-level");

        try {
            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            int status = response.statusCode();

            if (status != 200) {
                throw new UpstreamServiceException("Service at " +
                        levelUrl + " returned error " + status);
            }

            Map<String, Object> body = objectMapper.readValue(response.body(), Map.class);

            return objectMapper.convertValue(body.get("level"), Integer.class);

        } catch (IOException e) {
            throw new UpstreamServiceException("Network error calling Alert service at " + levelUrl, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new UpstreamServiceException("Interrupted while calling Alert service at " + levelUrl, e);
        }

    }

    private HttpRequest request(String url) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();
        return request;
    }

}
