package co.wethinkcode.healthsafe;

import java.time.Instant;

/**
 * Tracks the hospital's current Emergency Status: an integer level from
 * {@code 0} (normal operations) to {@code 8} (full Code Blue), plus when it
 * was last changed. Starts at {@code 0}.
 */
public class AlertLevel {
    private int level;
    private Instant lastChanged;

    public AlertLevel() {
        this.level = 0;
        this.lastChanged = Instant.now();
    }

    /**
     * @return the current Emergency Status level, in {@code 0..8}
     */
    public int level() {
        return level;
    }

    /**
     * @return when the level was last changed, as an ISO-8601 timestamp
     *         (e.g. {@code 2026-09-07T10:15:30Z})
     */
    public String lastChanged(){
        return lastChanged.toString();
    }

    /**
     * Parses and applies a new Emergency Status level, recording the time of
     * change.
     *
     * @param level the new level, as a string representing an integer in {@code 0..8}
     * @throws IllegalArgumentException if {@code level} isn't a valid integer,
     *         or is outside the {@code 0..8} range
     */
    public void updateLevel(String level) {
        int intLevel;
        try{
            intLevel = Integer.parseInt(level);
        } catch (NumberFormatException e){
            throw new IllegalArgumentException("Invalid level: " +
                    "level must be a number");
        }

        if (intLevel < 0 || intLevel > 8){
            throw new IllegalArgumentException("Invalid level: " +
                    "level has to be greater than 0 " +
                    "and less than or equal to 8");
        }

        this.level = intLevel;
        this.lastChanged = Instant.now();
    }
}
