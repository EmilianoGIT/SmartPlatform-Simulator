package it.filippetti.sp.simulator;

import io.vertx.core.AbstractVerticle;
import org.json.JSONArray;
import org.json.JSONObject;

public class Logger extends AbstractVerticle {

    JSONArray producedSnapshots = new JSONArray();
    private int limitOfKeptSnapshots = 10;

    public Logger() {


    }

    @Override
    public void start() throws Exception {

        vertx.eventBus().consumer(this.deploymentID(), message -> {

            int i;
            for (i = producedSnapshots.length() - limitOfKeptSnapshots; i > -1; i--) {
                producedSnapshots.remove(i);
            }
            this.producedSnapshots.put(new JSONObject(message.body().toString()));
            System.out.println(getProducedSnapshots().toString());
        });

    }

    @Override
    public void stop() throws Exception {

    }

    public JSONArray getProducedSnapshots() {
        return this.producedSnapshots;
    }


}