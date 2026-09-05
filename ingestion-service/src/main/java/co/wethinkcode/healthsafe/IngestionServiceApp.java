package co.wethinkcode.healthsafe;

import io.javalin.Javalin;

import java.util.Map;

public class IngestionServiceApp {

    public static void main(String[] args) {
        Javalin app = Javalin.create().start(7030);
        WardCleaner cleaner = new WardCleaner("wards-outdated.csv");
        cleaner.cleanRecords();

        app.get("/health", ctx -> ctx.result("OK"));
        app.get("/wards", ctx ->
                ctx.json(Map.of("data", cleaner.records()))
        );

    }
}
