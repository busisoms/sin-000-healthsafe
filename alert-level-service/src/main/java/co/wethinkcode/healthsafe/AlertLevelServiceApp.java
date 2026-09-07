package co.wethinkcode.healthsafe;

import io.javalin.Javalin;

import java.util.Map;

public class AlertLevelServiceApp {
    private final Javalin app;
    private final AlertLevel level;

    public AlertLevelServiceApp() {
        this.app = Javalin.create().start(7032);
        this.level = new AlertLevel();
    }

    private void health(){
        app.get("/health", ctx ->
                ctx.result("OK"));

    }

    private void alertLevel(){
        app.get("/alert-level", ctx -> {
            ctx.json(Map.of("level", level.level(),
                    "time", level.lastChanged()));
        });
    }

    private void update(){
        app.put("/alert-level/{level}", ctx -> {
            String levelVal = ctx.pathParam("level");
            try{
                level.updateLevel(levelVal);
                ctx.json(Map.of("level", level.level(),
                        "time", level.lastChanged()));

            } catch (IllegalArgumentException e) {
                ctx.status(400);
                ctx.json(Map.of("error", e.getMessage()));
            }

        });
    }

    public static void main(String[] args) {
        AlertLevelServiceApp serviceApp = new AlertLevelServiceApp();
        serviceApp.health();
        serviceApp.alertLevel();
        serviceApp.update();

    }
}
