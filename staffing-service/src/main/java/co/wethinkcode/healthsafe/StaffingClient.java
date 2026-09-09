package co.wethinkcode.healthsafe;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * staffing-service's HTTP client for the two services it depends on before it can
 * schedule: ward-service (to validate the ward) and alert-level-service (to read the
 * current Emergency Status).
 */
public class StaffingClient {
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String wardUrl;
    private final String levelUrl;


    /**
     * @param wardUrl base URL of ward-service, no trailing slash
     *                (e.g. {@code http://localhost:7031})
     * @param levelUrl base URL of alert-level-service, no trailing slash
     */
    public StaffingClient(String wardUrl, String levelUrl) {
        this.wardUrl = wardUrl;
        this.levelUrl = levelUrl;
    }


    /**
     * Looks up a single ward by id.
     *
     * @param id the ward id to look up
     * @return the matching {@link Ward}
     * @throws WardNotFoundException if ward-service has no ward with that id
     * @throws UpstreamServiceException if ward-service is unreachable, times out, or
     *         answers with anything other than {@code 200} or {@code 404}
     */
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
            if (rawWard == null) {
                throw new UpstreamServiceException("Ward service at " + wardUrl + " returned a response without a 'data' field");
            }
            return objectMapper.convertValue(rawWard, Ward.class);
            
        } catch (IOException e) {
            throw new UpstreamServiceException("Network error calling Ward service at " + wardUrl, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new UpstreamServiceException("Interrupted while calling Ward service at " + wardUrl, e);
        }

    }

    /**
     * Reads the hospital's current Emergency Status.
     *
     * @return the level as reported by alert-level-service, normally {@code 0..8}
     * @throws UpstreamServiceException if alert-level-service is unreachable, times
     *         out, or answers with anything other than {@code 200}
     */
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

            Object levelObj = body.get("level");
            if (levelObj == null) {
                throw new UpstreamServiceException("Alert service at " + levelUrl + " returned a response without a 'level' field");
            }
            return objectMapper.convertValue(levelObj, Integer.class);

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
