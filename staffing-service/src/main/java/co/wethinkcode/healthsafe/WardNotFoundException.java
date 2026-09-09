package co.wethinkcode.healthsafe;

/**
 * Thrown when ward-service reports that the requested ward doesn't exist
 * (a {@code 404}), so there's nothing to schedule against.
 */
public class WardNotFoundException extends RuntimeException {
    public WardNotFoundException(String message) {
        super(message);
    }
}
