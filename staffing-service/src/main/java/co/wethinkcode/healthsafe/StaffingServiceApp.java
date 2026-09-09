package co.wethinkcode.healthsafe;

import io.javalin.Javalin;
import java.util.Map;

public class StaffingServiceApp {
    private final Javalin app;
    private final ScheduleCalculator calculator;

    public StaffingServiceApp() {
        StaffingClient client = new StaffingClient(
                "http://localhost:7031",
                "http://localhost:7032");

        this.app = Javalin.create().start(7033);
        this.calculator = new ScheduleCalculator(client);
    }

    private void health(){
        app.get("/health", ctx ->
                ctx.result("OK"));
    }

    private void onCall(){
        app.get("/on-call/{wardId}", ctx -> {
                OnCallSchedule schedule = calculator.calculate(ctx.pathParam("wardId"));
                ctx.json(Map.of("data", schedule));
        });
    }

    private void exceptions() {
        app.exception(WardNotFoundException.class, (e, ctx) ->
                ctx.status(404).json(Map.of("error", e.getMessage())));
        app.exception(UpstreamServiceException.class, (e, ctx) ->
                ctx.status(502).json(Map.of("error", e.getMessage())));
        app.exception(IllegalArgumentException.class, (e, ctx) ->
                ctx.status(400).json(Map.of("error", e.getMessage())));
    }

    public static void main(String[] args) {
        StaffingServiceApp service = new StaffingServiceApp();
        service.health();
        service.onCall();
        service.exceptions();

    }
}

// MQ TODO: publishes to ActiveMQ topic MqConfig.TOPIC at MqConfig.BROKER_URL (see co.wethinkcode.healthsafe.mq.MqConfig)
