package it.filippetti.sp.simulator;

import io.vertx.core.AbstractVerticle;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class Logger extends AbstractVerticle {


    JSONArray producedSnapshots = new JSONArray();
    Engine engine;

    public Logger(Engine engine) {
        this.engine = engine;
    }

    @Override
    public void start() throws Exception {

        vertx.eventBus().consumer("snapshot-for-logger", message -> {
            this.producedSnapshots.put(new JSONObject(message.body().toString()));
            System.out.println(message.body().toString());
        });

    }

    @Override
    public void stop() throws Exception {

    }



    public void clearProducedSnapshots() {
        this.producedSnapshots=new JSONArray();
    }

    public JSONObject getProducedSnapshots() {


        return new JSONObject().put("snapshots", this.producedSnapshots);
    }

}