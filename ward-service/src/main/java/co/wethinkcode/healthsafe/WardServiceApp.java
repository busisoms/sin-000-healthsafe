package co.wethinkcode.healthsafe;

import io.javalin.Javalin;

import java.util.Map;

public class WardServiceApp {

    public static void main(String[] args) {
        Javalin app = Javalin.create().start(7031);
        WardClient client = new WardClient();
        client.fetchWards();

        app.get("/health", ctx -> ctx.result("OK"));

        app.get("/wards/{id}", ctx -> {
            String id = ctx.pathParam("id");
           Ward ward = client.findById(id);
           if (ward == null) {
               ctx.status(404);
               ctx.json(Map.of("ERROR", "Ward %s Not Found"
                       .formatted(id)));
           }
           else {
               ctx.json(ward);
           }
        });

        // TODO (Provides lists of wards and departments.)
        // Add domain endpoints for ward-service here.
    }
}

// MQ TODO: subscribes to ActiveMQ topic MqConfig.TOPIC at MqConfig.BROKER_URL (see co.wethinkcode.healthsafe.mq.MqConfig)
// MQ TODO: publishes to ActiveMQ queue MqConfig.QUEUE when it detects an equipment failure on one of its wards.
