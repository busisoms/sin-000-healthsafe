package co.wethinkcode.healthsafe;

public class WardNotFoundException extends RuntimeException {
    public WardNotFoundException(String message) {
        super(message);
    }
}
