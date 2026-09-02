package co.wethinkcode.healthsafe.models;


/**
 * A serializable DTO for a ward
 */
public record Ward (
        String wardId,
        String wing,
        String department,
        Integer bedsAvailable,
        String note ){
}
