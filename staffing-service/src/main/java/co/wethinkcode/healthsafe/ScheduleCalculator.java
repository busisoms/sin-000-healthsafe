package co.wethinkcode.healthsafe;

import java.util.Map;

/**
 * Computes on-call schedules: looks up the ward and the current Emergency Status
 * through {@link StaffingClient}, then scales the department's baseline doctor
 * count by the {@link AlertBand} that status falls in.
 */
public class ScheduleCalculator {

    private  final Map<String, Integer> baselines = Map.of(
            "icu", 3,
            "cardiology", 2, "maternity", 2, "paediatrics", 2,
            "oncology", 1, "radiology", 1);

    private final StaffingClient client;

    /**
     * @param client the client used to reach ward-service and alert-level-service
     */
    public ScheduleCalculator(StaffingClient client) {
        this.client = client;
    }

    /**
     * Builds the on-call schedule for one ward.
     *
     * @param wardId the ward to schedule for
     * @return the computed schedule
     * @throws WardNotFoundException if ward-service has no ward with that id
     * @throws UpstreamServiceException if either downstream service is unreachable,
     *         times out, or answers with something unusable
     */
    public OnCallSchedule calculate(String wardId){
        Ward ward = client.fetchWard(wardId);
        int level = client.fetchCurrentLevel();

        AlertBand band = AlertBand.fromLevel(level);
        String department = ward.department();

        int baseline = baselines.getOrDefault(department.toLowerCase(), 1);
        int numberOfStaff = band.staffCall(baseline);

        return new OnCallSchedule(
                ward.wardId(),
                department,
                level,
                band.name(),
                numberOfStaff
        );
    }
}
