package co.wethinkcode.healthsafe;

/**
 * The on-call schedule computed for a single ward.
 *
 * @param wardId the ward this schedule is for
 * @param department the ward's specialist department
 * @param alertLevel the Emergency Status it was computed against, {@code 0..8}
 * @param band the {@link AlertBand} that level falls in, by name
 * @param staffRequired the number of doctors to put on call
 */
public record OnCallSchedule(
        String wardId,
        String department,
        int alertLevel,
        String band,
        int staffRequired
) {
}
