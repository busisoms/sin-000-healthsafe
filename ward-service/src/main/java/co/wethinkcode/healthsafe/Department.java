package co.wethinkcode.healthsafe;

/**
 * Aggregated view of one department across all its wards.
 *
 * @param department the department name (matches {@link Ward#department()}, including
 *                    the {@code "Unknown"} sentinel for wards with no department on record)
 * @param wardCount the number of wards in this department
 */
public record Department(String department, int wardCount) {
}
