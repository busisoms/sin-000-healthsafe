package co.wethinkcode.healthsafe;

/**
 * Thrown when a service staffing-service depends on is unreachable, times out, or
 * answers with something staffing-service can't use. Distinct from
 * {@link WardNotFoundException}, which is a valid answer meaning "no such ward".
 */
public class UpstreamServiceException extends RuntimeException {
    public UpstreamServiceException(String message) {
        super(message);
    }

    public UpstreamServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
