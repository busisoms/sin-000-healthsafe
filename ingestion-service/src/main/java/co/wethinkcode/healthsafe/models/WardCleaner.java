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
        if (row.length != 4){
            throw new IllegalStateException("Incomplete record");
        }

    }

    private String normalizeId(String id){
        return id.toUpperCase().strip();
    }

    private Map<String,String> normalizeWing(String wing){
        if (wing.isEmpty()) {
            return Map.of("Unknown",
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
        return Map.of(result.toString().trim(), null);
    }

    private Map<String, String> normalizeDepartment(String department){
        if (department.isEmpty()) {
            return Map.of("Unknown",
                    "Department empty: flagged for follow up"); // there is now clear way to know the correct department
        }

        if (department.matches("^[a-zA-Z]+$")){
            return Map.of("Unknow", "Department numeric (%s): flagged for follow up"
                    .formatted(department));
        }

        department = (department.substring(0, 1).toUpperCase()
                + department.substring(1)).strip();

        if (department.equals("Pediatrics")){
            department = "Paediatrics";
        }

        return Map.of(department, null);

    }

    private Integer normalizeAvailableBeds(String availableBeds){

    }



}
