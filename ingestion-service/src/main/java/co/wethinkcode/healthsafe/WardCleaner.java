package co.wethinkcode.healthsafe;

import com.opencsv.*;
import com.opencsv.exceptions.CsvValidationException;

import java.io.*;
import java.util.*;

/**
 * A utility class for cleaning up
 *  wards from a csv file to ward object {@link Ward}
 */
public class WardCleaner {
    private final String csvFile;
    private List<Ward> records;

    /**
     * A single normalized value paired with an optional data-quality note.
     * The note is {@code null} when nothing noteworthy happened during normalization.
     *
     * @param result the normalized value
     * @param note a human-readable flag for follow-up, or {@code null} if none
     * @param <T> the type of the normalized value
     */
    record Result<T>(T result, String note){}

    /**
     * Creates a cleaner that reads ward records from the given classpath resource.
     *
     * @param csvFile the name of the CSV resource (looked up on the classloader)
     */
    public WardCleaner(String csvFile){
        this.csvFile = csvFile;
        this.records = new ArrayList<>();

    }

    /**
     * Returns the wards cleaned so far.
     *
     * @return an unmodifiable view of the cleaned {@link Ward} records
     */
    public List<Ward> records(){
        return Collections.unmodifiableList(records);
    }

    /**
     * Reads {@link #csvFile} from the classpath, skips its header row, normalizes
     * every remaining row into a {@link Ward}, and merges rows that share a
     * (normalized) {@code wardId} into a single record.
     *
     * @throws IllegalArgumentException if {@link #csvFile} cannot be found on the classpath
     * @throws RuntimeException if the file cannot be read or parsed as CSV
     */
    public void cleanRecords(){
        List<Ward> cleaned = new ArrayList<>();

        try (InputStream is = WardCleaner.class.getClassLoader().getResourceAsStream(csvFile)) {
            if (is == null) {
                throw new IllegalArgumentException("File not found in resources: " + csvFile);
            }

            try (CSVReader reader = new CSVReader(new InputStreamReader(is))) {
                reader.readNext();

                String[] row;
                while ((row = reader.readNext()) != null) {
                    cleaned.add(clean(row));
                }
            }

        } catch (IOException | CsvValidationException ex) {
            throw new RuntimeException(ex);
        }

        records = dedupeAndMerge(cleaned);
    }

    /**
     * Groups cleaned wards by {@code wardId} (already normalized by {@link #normalizeId})
     * and merges each group into a single {@link Ward}, preserving first-seen order.
     *
     * @param cleaned the individually-normalized wards, in encounter order
     * @return one merged {@link Ward} per distinct wardId
     */
    List<Ward> dedupeAndMerge(List<Ward> cleaned){
        Map<String, Ward> byId = new LinkedHashMap<>();
        for (Ward ward : cleaned) {
            byId.merge(ward.wardId(), ward, this::mergeDuplicate);
        }
        return new ArrayList<>(byId.values());
    }

    /**
     * Merges two {@link Ward} records that share a wardId, field by field: for each
     * field, a valid value wins over a placeholder/missing one, and if both are valid,
     * {@code incoming} (the later-encountered row) wins. The merged note documents what
     * was kept/dropped and carries forward each row's own note text.
     *
     * @param existing the ward accumulated so far for this wardId
     * @param incoming the next duplicate row for this wardId
     * @return the merged {@link Ward}
     */
    private Ward mergeDuplicate(Ward existing, Ward incoming){
        List<String> notes = new ArrayList<>();
        notes.add("Duplicate wardId detected — merged with another record for '%s'"
                .formatted(incoming.wardId()));

        Result<String> wing = mergeText("wing", existing.wing(), incoming.wing());
        addNote(wing.note(), notes);
        Result<String> department = mergeText("department", existing.department(), incoming.department());
        addNote(department.note(), notes);
        Result<Integer> beds = mergeBeds(existing.bedsAvailable(), incoming.bedsAvailable());
        addNote(beds.note(), notes);

        if (existing.note() != null && !existing.note().isBlank()) notes.add(existing.note());
        if (incoming.note() != null && !incoming.note().isBlank()) notes.add(incoming.note());

        return new Ward(existing.wardId(), wing.result(), department.result(), beds.result(), notes(notes));
    }

    /**
     * Merges a text field (wing/department) across two duplicate rows. {@code "Unknown"}
     * is treated as the placeholder/invalid sentinel, matching what {@link #normalizeWing}
     * and {@link #normalizeDepartment} emit for blank/invalid input.
     *
     * @param field the field name, used only for the note text
     * @param existingVal the accumulated value so far
     * @param incomingVal the next duplicate row's value
     * @return the merged value and, if a value was dropped, a note explaining why
     */
    Result<String> mergeText(String field, String existingVal, String incomingVal){
        boolean existingValid = !"Unknown".equals(existingVal);
        boolean incomingValid = !"Unknown".equals(incomingVal);
        if (incomingValid) {
            String note = (existingValid && !existingVal.equals(incomingVal))
                    ? "%s: kept '%s' (more recent duplicate), dropped '%s'"
                            .formatted(field, incomingVal, existingVal)
                    : null;
            return new Result<>(incomingVal, note);
        }
        if (existingValid) {
            return new Result<>(existingVal, "%s: kept '%s' from earlier duplicate (later row was Unknown)"
                    .formatted(field, existingVal));
        }
        return new Result<>("Unknown", null);
    }

    /**
     * Merges {@code bedsAvailable} across two duplicate rows. {@code null} is treated as
     * the missing/invalid sentinel, matching {@link #normalizeAvailableBeds}.
     *
     * @param existingVal the accumulated value so far
     * @param incomingVal the next duplicate row's value
     * @return the merged value and, if a value was dropped, a note explaining why
     */
    Result<Integer> mergeBeds(Integer existingVal, Integer incomingVal){
        if (incomingVal != null) {
            String note = (existingVal != null && !existingVal.equals(incomingVal))
                    ? "bedsAvailable: kept %d (more recent duplicate), dropped %d"
                            .formatted(incomingVal, existingVal)
                    : null;
            return new Result<>(incomingVal, note);
        }
        if (existingVal != null) {
            return new Result<>(existingVal, "bedsAvailable: kept %d from earlier duplicate (later row was invalid)"
                    .formatted(existingVal));
        }
        return new Result<>(null, null);
    }

    /**
     * Normalizes one CSV row (ward id, wing, department, beds available) into a
     * {@link Ward}, collecting any data-quality notes raised along the way.
     *
     * @param row the raw CSV row, expected to have exactly 4 columns
     * @return the cleaned {@link Ward}
     * @throws IllegalStateException if the row does not have exactly 4 columns
     */
    private Ward clean(String[] row){
        List<String> notes = new ArrayList<>();

        if (row.length != 4){
            throw new IllegalStateException("Incomplete record");
        }

        String id = normalizeId(row[0]);
        Result<String> wing = normalizeWing(row[1]);
        addNote(wing.note(), notes);
        Result<String> department = normalizeDepartment(row[2]);
        addNote(department.note(), notes);
        Result<Integer> bedsAvailable = normalizeAvailableBeds(row[3]);
        addNote(bedsAvailable.note(), notes);

        String strNotes = notes(notes);

        return new Ward(id,wing.result(),
                department.result(),
                bedsAvailable.result(),
                strNotes);
    }


    /**
     * Appends {@code note} to {@code notes} if it is non-null.
     *
     * @param note the note to add, or {@code null} to add nothing
     * @param notes the list to append to
     */
    private void addNote(String note, List<String> notes){
        if (note != null) notes.add(note);
    }

    /**
     * Joins a list of notes into a single newline-separated string.
     *
     * @param notes the notes to join
     * @return the notes joined by newlines, or an empty string if there are none
     */
    private String notes(List<String> notes){
        return String.join("\n", notes);
    }

    /**
     * Normalizes a raw ward id by trimming whitespace and upper-casing it.
     *
     * @param id the raw ward id
     * @return the normalized ward id
     */
    private String normalizeId(String id){
        return id.toUpperCase().strip();
    }

    /**
     * Normalizes a raw wing name to title case (e.g. {@code " east wing "} becomes
     * {@code "East Wing"}). A blank wing is flagged as {@code "Unknown"}.
     *
     * @param wing the raw wing name
     * @return the normalized wing and, if it was blank, a follow-up note
     */
    private Result<String> normalizeWing(String wing){
        String trimmed = wing == null ? "" : wing.strip();

        if (trimmed.isEmpty()) {
            return new Result<>("Unknown",
                    "Wing empty: flagged for follow up"); // there is no clear way to know the correct wing
        }

        String[] words = trimmed.split("\\s+");
        StringBuilder result = new StringBuilder();

        for (String word : words) {
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1).toLowerCase())
                        .append(" ");
            }
        }
        return new Result<>(result.toString().trim(), null);
    }

    /**
     * Normalizes a raw department name to title case (e.g. {@code "CARDIOLOGY"} becomes
     * {@code "Cardiology"}) and corrects the "Pediatrics" spelling to "Paediatrics".
     * A blank department is flagged as {@code "Unknown"}; a department containing digits
     * or symbols is flagged and left as {@code "Unknown"} as well.
     *
     * @param department the raw department name
     * @return the normalized department and, if it was blank or non-alphabetic, a follow-up note
     */
    private Result<String> normalizeDepartment(String department){
        String trimmed = department == null ? "" : department.strip();

        if (trimmed.isEmpty()) {
            return new Result<>("Unknown",
                    "Department empty: flagged for follow up"); // there is no clear way to know the correct department
        }

        if (!trimmed.matches("^[a-zA-Z]+$")){
            return new Result<>("Unknown", "Department numeric (%s): flagged for follow up"
                    .formatted(trimmed));
        }

        String normalized = Character.toUpperCase(trimmed.charAt(0))
                + trimmed.substring(1).toLowerCase();

        if (normalized.equals("Pediatrics")){
            normalized = "Paediatrics";
        }

        return new Result<>(normalized, null);
    }

    /**
     * Normalizes a raw available-beds value to a non-negative {@link Integer}.
     * Known placeholder strings (e.g. {@code "n/a"}, {@code "tbd"}, {@code "unknown"}),
     * non-numeric text, negative numbers, and implausibly large numbers (over 50) are
     * all treated as missing/invalid and flagged with a follow-up note.
     *
     * @param availableBeds the raw beds-available value
     * @return the normalized bed count, or {@code null} with a follow-up note if invalid
     */
    private Result<Integer> normalizeAvailableBeds(String availableBeds){
        Set<String> placeholders =
                Set.of("n/a", "na", "nan", "-", "tbd", "unknown", "");

        String trimmed = availableBeds == null ? "" : availableBeds.strip();

        if (placeholders.contains(trimmed.toLowerCase())) {
            return new Result<>(null, "bedsAvailable was missing ('%s')"
                    .formatted(trimmed));
        }

        int number;
        try {
            number = Integer.parseInt(trimmed);
        } catch (NumberFormatException e) {
            return new Result<>(null, "bedsAvailable was non-numeric ('%s')"
                    .formatted(trimmed));
        }

        if (number < 0){
            return new Result<>(null, "bedsAvailable was negative ('%d')"
                    .formatted(number));
        }

        if (number > 50){
            return new Result<>(null, "bedsAvailable was impossible for number of beds ('%d')"
                    .formatted(number));
        }

        return new Result<>(number, null);

    }
    
}
