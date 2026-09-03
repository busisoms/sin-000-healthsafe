package co.wethinkcode.healthsafe.models;

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

    private record Result<T>(T result, String note){}

    public WardCleaner(String csvFile){
        this.csvFile = csvFile;
        this.records = new ArrayList<>();

    }

    public List<Ward> records(){
        return Collections.unmodifiableList(records);
    }

    public Ward searchById(String id){
        for (Ward record : records){
            if (record.wardId().equals(id)) return record;
        }
        return null;
    }

    public void cleanRecords(){

        try (InputStream is = WardCleaner.class.getClassLoader().getResourceAsStream(csvFile)) {
            if (is == null) {
                throw new IllegalArgumentException("File not found in resources: " + csvFile);
            }

            CSVReader reader = new CSVReader(new InputStreamReader(is));
            reader.readNext();

            String[] row;
            while ((row = reader.readNext()) != null) {
                records.add(clean(row));
            }

        } catch (IOException | CsvValidationException ex) {
            throw new RuntimeException(ex);
        }

    }

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


    private void addNote(String note, List<String> notes){
        if (note != null) notes.add(note);
    }

    private String notes(List<String> notes){
        StringBuilder sb = new StringBuilder();
        for (String note : notes){
            sb.append(note).append("\n");
        }

        return sb.toString();
    }

    private String normalizeId(String id){
        return id.toUpperCase().strip();
    }

    private Result<String> normalizeWing(String wing){
        if (wing.isEmpty()) {
            return new Result<>("Unknown",
                    "Wing empty: flagged for follow up"); // there is now clear way to know the correct wing
        }

        String[] words = wing.split("\\s+");
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

    private Result<String> normalizeDepartment(String department){

        department = (department.substring(0, 1).toUpperCase()
                + department.substring(1)).strip();
        if (department.isEmpty() || department.equals(" ")) {
            return new Result<>( "Unknown",
                    "Department empty: flagged for follow up"); // there is now clear way to know the correct department
        }

        if (!department.matches("^[a-zA-Z]+$")){
            return new Result<>("Unknow", "Department numeric (%s): flagged for follow up"
                    .formatted(department));
        }

        if (department.equals("Pediatrics")){
            department = "Paediatrics";
        }

        return new Result<>(department, null);
    }

    private Result<Integer> normalizeAvailableBeds(String availableBeds){
        Set<String> place_holders =
                Set.of("n/a", "na", "nan", "-", "tbd", "unknown", "");

        String trimmed = availableBeds == null ? "" : availableBeds.strip();

        if (place_holders.contains(trimmed.toLowerCase())) {
            return new Result<>(null, "bedsAvailable was missing ('%s')"
                    .formatted(availableBeds));
        }

        int number = 0;
        try {
            if (availableBeds != null){
                number = Integer.parseInt(availableBeds);
            }
        } catch (NumberFormatException e) {
            return new Result<>(null, "bedsAvailable was non-numeric ('%s')"
                    .formatted(availableBeds));
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
