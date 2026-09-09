package co.wethinkcode.healthsafe;

import io.javalin.Javalin;

import java.util.Map;

public class WardServiceApp {
    private final Javalin app;
    private final WardClient client;

    public WardServiceApp() {
        this.app = Javalin.create().start(7031);
        this.client = new WardClient();
        client.fetchWards();
    }

    private void health(){
        app.get("/health", ctx -> ctx.result("OK"));
    }

    private void wards(){
        app.get("/wards", ctx ->
                ctx.json(Map.of("data", client.wards())));
    }

    private void wardsId(){
        app.get("/wards/{id}", ctx -> {
            String id = ctx.pathParam("id");
            Ward ward = client.findById(id);
            if (ward == null) {
                ctx.status(404);
                ctx.json(Map.of("error", "Ward %s Not Found"
                        .formatted(id)));
            }
            else {
                ctx.json(Map.of("data" ,ward));
            }
        });
    }

    private void departments(){
        app.get("/departments", ctx ->
                ctx.json(Map.of("data", client.departments())));
    }

    public static void main(String[] args) {
        WardServiceApp wardServiceApp = new WardServiceApp();
        wardServiceApp.health();
        wardServiceApp.wards();
        wardServiceApp.wardsId();
        wardServiceApp.departments();

    }
}

// MQ TODO: subscribes to ActiveMQ topic MqConfig.TOPIC at MqConfig.BROKER_URL (see co.wethinkcode.healthsafe.mq.MqConfig)
// MQ TODO: publishes to ActiveMQ queue MqConfig.QUEUE when it detects an equipment failure on one of its wards.
