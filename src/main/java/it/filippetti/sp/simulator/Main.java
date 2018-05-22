package it.filippetti.sp.simulator;

import io.vertx.core.Vertx;
import org.json.JSONArray;
import org.json.JSONObject;

public class Main {

    public static void main(String[] args) throws Exception {


        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new Server());

    }


    public void genMeasureTypes() {
        Integer count = 0;
        JSONObject collector = new JSONObject();


        JSONArray measureTypes = new JSONArray();

        JSONObject measureType = new JSONObject();

        do {

            measureType.put("name", "misurazione bella");
            measureType.put("k", "device_temperature");
            measureType.put("u", "C°");
            measureType.put("Min", "20");
            measureType.put("Max", "30");
            measureType.put("SelectArray", "m");
            count++;
            measureTypes.put(measureType);
        } while (count <= 10);


        collector.put("measureTypes", measureTypes);

        System.out.println(collector);
    }


}
