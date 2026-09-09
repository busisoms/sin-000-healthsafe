package co.wethinkcode.healthsafe;

/**
 * How hard the hospital's Emergency Status leans on staffing. The nine levels
 * collapse into three bands, each carrying a multiplier applied to a department's
 * baseline doctor count.
 */
public enum AlertBand {
    NORMAL(1),
    ELEVATED(2),
    CRITICAL(3);

    private final int multiplier;

    AlertBand(int multiplier) {
        this.multiplier = multiplier;
    }

    /**
     * Maps an Emergency Status level to its band: {@code 0-2} NORMAL, {@code 3-5}
     * ELEVATED, {@code 6-8} CRITICAL.
     *
     * @param level the current Emergency Status, {@code 0..8}
     * @return the band that level falls in
     * @throws IllegalArgumentException if {@code level} is outside {@code 0..8}
     */
    public static AlertBand fromLevel(int level){
        return switch (level){
            case 0, 1, 2 -> NORMAL;
            case 3, 4, 5 -> ELEVATED;
            case 6, 7, 8 -> CRITICAL;
            default -> throw new IllegalArgumentException("Invalid level: " + level);
        };
    }

    /**
     * Scales a department's baseline headcount for this band.
     *
     * @param baseline the department's normal-operations doctor count
     * @return the number of doctors to put on call
     */
    public int staffCall(int baseline) {
        return baseline * multiplier;
    }
}
