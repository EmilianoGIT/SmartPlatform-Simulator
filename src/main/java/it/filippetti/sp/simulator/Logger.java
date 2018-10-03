package it.filippetti.sp.simulator;

import io.vertx.core.AbstractVerticle;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class Logger extends AbstractVerticle {

    JSONArray producedSnapshots = new JSONArray();

    public Logger() {


    }

    @Override
    public void start() throws Exception {

        vertx.eventBus().consumer(this.deploymentID(), message -> {
            this.producedSnapshots.put(new JSONObject(message.body().toString()));
            System.out.println(producedSnapshots.toString());

        });

    }

    @Override
    public void stop() throws Exception {

    }


    public void clearProducedSnapshots() {
        this.producedSnapshots = new JSONArray();
    }

    public JSONArray getProducedSnapshots() {
        return this.producedSnapshots;
    }

}