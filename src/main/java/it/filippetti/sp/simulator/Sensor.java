package it.filippetti.sp.simulator;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import it.filippetti.sp.simulator.model.MeasureType;
import it.filippetti.sp.simulator.model.SnapshotModel;
import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;


public class Sensor extends AbstractVerticle {
    String senName;
    String ref;
    String type;
    long polling;
    String topic;
    HashMap<Integer, SnapshotModel> snapshotModels = new HashMap<Integer, SnapshotModel>();

    Sensor(String senName, String ref, String type, long polling,String topic)       //pollingTime expressed in milliseconds
    {
        this.ref = ref;
        this.type = type;
        this.polling = polling*1000;
        this.topic=topic;
    }


    @Override
    public void start() throws Exception {

        System.out.println("Il sensore " + this.getRef() + " ha iniziato a produrre snapshot");

        vertx.setPeriodic(this.polling, new Handler<Long>() {

            @Override
            public void handle(Long aLong) {

                JSONObject jo=genSnapshot();
                vertx.eventBus().send("snapshot-for-logger", jo.toString());
                vertx.eventBus().send("snapshot-for-mqtt-sender", getTopic()+"tsDelimiter"+jo.toString());

            }
        });

    }

    @Override
    public void stop() throws Exception {
        System.out.println("Il sensore " + this.getRef() + " ha terminato di produrre snapshot");
    }


    public void setRef(String ref) {
        this.ref = ref;
    }

    public void setPollingTime(long polling){
        this.polling = polling;
    }


    public void addModel(SnapshotModel snapshotModel) {

        try {
            this.snapshotModels.put(snapshotModel.getId(), snapshotModel);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public String getSenName()
    {return this.senName;}

    public String getRef() {
        return this.ref;
    }

    public String getType() {
        return this.type;
    }

    public long getPollingTime() {
        return this.polling;
    }

    public String getTopic() {
        return this.topic;
    }

    public Float getSumOfProbability() {

        Float sumOfProb = 0.0f;

        for (Map.Entry<Integer, SnapshotModel> entry : this.snapshotModels.entrySet()) {

            Float entryProb = entry.getValue().getProbability();
            sumOfProb = sumOfProb + entryProb;
        }
        return sumOfProb;
    }

    public HashMap<Integer, SnapshotModel> getSnapshotModels() {
        return this.snapshotModels;
    }



    public JSONObject genSnapshot() {


        Map.Entry<Integer, SnapshotModel> pickedEntryModel = pickRandomModel();

        JSONObject snapshot = new JSONObject();
        JSONArray ruid = new JSONArray();
        JSONArray m = new JSONArray();
        JSONArray r = new JSONArray();


        DateTime timeZone = new DateTime();


        snapshot.put("ref", this.getRef());
        snapshot.put("type", this.getType());
        snapshot.put("cat", pickedEntryModel.getValue().getCategory());
        snapshot.put("t", timeZone.getMillis());
        snapshot.put("tz", timeZone);
        snapshot.put("uuid", genId());
        snapshot.put("cuid", genId());
        snapshot.put("ruid", ruid);


        if (pickedEntryModel.getValue().getMeasureTypes() != null)            //vado a vedere le misure del modello
        {


            for (Map.Entry<Integer, MeasureType> measureTypeEntry : pickedEntryModel.getValue().getMeasureTypes().entrySet()) {

                DateTime timeZoneObjectOfMeasure = new DateTime();

                JSONObject objectOfMeasure = new JSONObject();


                float vMin = Float.parseFloat(measureTypeEntry.getValue().getMinRange());
                float vMax = Float.parseFloat(measureTypeEntry.getValue().getMaxRange());

                Float value = (float) (Math.random() * (vMax - vMin)) + vMin;


                objectOfMeasure.put("k", measureTypeEntry.getValue().getKey());
                objectOfMeasure.put("u", measureTypeEntry.getValue().getUnity());
                objectOfMeasure.put("v", toString().valueOf(value));
                objectOfMeasure.put("t", timeZoneObjectOfMeasure.getMillis());
                objectOfMeasure.put("tz", timeZoneObjectOfMeasure);
                objectOfMeasure.put("s", measureTypeEntry.getValue().getSource());
                objectOfMeasure.put("d", measureTypeEntry.getValue().getDestination());

                //objectOfMeasure.put("l", measureTypeEntry.getValue().getVariance());
                //objectOfMeasure.put("p", measureTypeEntry.getValue().getProbability());

                if (measureTypeEntry.getValue().getWhichType() == 'm') {
                    m.put(objectOfMeasure);
                } else r.put(objectOfMeasure);
            }
        }
        snapshot.put("m", m);
        snapshot.put("r", r);

        return snapshot;

    }




    public static String randomAlphaNumeric(int length, String availableChars) {
        StringBuilder builder = new StringBuilder();
        while (length-- != 0) {
            int character = (int) (Math.random() * availableChars.length());
            builder.append(availableChars.charAt(character));
        }
        return builder.toString();
    }

    public static String genId() {
        String lettersAndDigits = "abcdef0123456789";

        String p1;
        String p2;
        String p3;
        String p4;
        String p5;

        p1 = randomAlphaNumeric(8, lettersAndDigits);
        p2 = randomAlphaNumeric(4, lettersAndDigits);
        p3 = randomAlphaNumeric(4, lettersAndDigits);
        p4 = randomAlphaNumeric(4, lettersAndDigits);
        p5 = randomAlphaNumeric(12, lettersAndDigits);

        return p1 + '-' + p2 + '-' + p3 + '-' + p4 + '-' + p5;
    }


    public Map.Entry<Integer, SnapshotModel> pickRandomModel() {


        float inf = 0;
        float sup = 0;

        float r = (float) Math.random();
        for (Map.Entry<Integer, SnapshotModel> entry : this.snapshotModels.entrySet()) {
            sup = sup + (entry.getValue().getProbability() / 100);
            if (r <= sup && r >= inf)
                return entry;
            else inf = sup;
        }

        return null;

    }


}
