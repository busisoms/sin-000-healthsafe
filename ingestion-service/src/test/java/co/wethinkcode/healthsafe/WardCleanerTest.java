package co.wethinkcode.healthsafe;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WardCleanerTest {

    @Test
    void mergesRealDuplicateWardIdFromCsv() {
        WardCleaner cleaner = new WardCleaner("wards-outdated.csv");
        cleaner.cleanRecords();

        // 18 raw rows, one duplicate pair (W-05 / w-05) -> 17 merged records
        assertEquals(17, cleaner.recordCount());

        long w05Count = cleaner.records().stream()
                .filter(w -> w.wardId().equals("W-05"))
                .count();
        assertEquals(1, w05Count);

        Ward w05 = cleaner.records().stream()
                .filter(w -> w.wardId().equals("W-05"))
                .findFirst()
                .orElseThrow();

        assertEquals("East Wing", w05.wing());
        assertEquals("Paediatrics", w05.department());
        assertEquals(5, w05.bedsAvailable());
        assertTrue(w05.note().contains("kept 5"));
        assertTrue(w05.note().contains("non-numeric"));
    }

    @Test
    void foldsThreeWayDuplicateFieldByFieldPreferringTheLatestValidValue() {
        WardCleaner cleaner = new WardCleaner("wards-outdated.csv");

        Ward earliest = new Ward("W-99", "Unknown",
                "Oncology", null, "wing empty: flagged for follow up");

        Ward middle   = new Ward("W-99", "North Wing",
                "Oncology", 7, "");

        Ward latest   = new Ward("W-99", "South Wing",
                "Cardiology", 3, "");

        List<Ward> merged = cleaner.dedupeAndMerge(List.of(earliest, middle, latest));

        assertEquals(1, merged.size());
        Ward result = merged.get(0);
        assertEquals("W-99", result.wardId());
        assertEquals("South Wing", result.wing());
        assertEquals("Cardiology", result.department());
        assertEquals(3, result.bedsAvailable());
    }
}
