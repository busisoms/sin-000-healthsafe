package co.wethinkcode.healthsafe;


/**
 * A serializable DTO for a ward
 *
 * @param wardId the ward's unique id, normalized to uppercase
 * @param wing the wing the ward is in
 * @param department the specialist department the ward belongs to
 * @param bedsAvailable the number of available beds, or {@code null} if unknown
 * @param note a human-readable data-quality note, or empty if none
 */
public record Ward (
        String wardId,
        String wing,
        String department,
        Integer bedsAvailable,
        String note ){
}
