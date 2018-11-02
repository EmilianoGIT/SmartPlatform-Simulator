package it.filippetti.sp.simulator;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import it.filippetti.sp.simulator.model.Behavior;
import it.filippetti.sp.simulator.model.MeasureType;
import it.filippetti.sp.simulator.model.SnapshotModel;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;


public class Sensor extends AbstractVerticle {
    public String senName;
    public String ref;
    public String type;
    public long polling;   //espresso in millisecondi
    public String topic;
    public HashMap<Integer, SnapshotModel> snapshotModels = new HashMap<Integer, SnapshotModel>();
    Logger logger;
    Engine engine;

    public Sensor(String senName, String ref, String type, long polling, String topic) throws Exception       //pollingTime expressed in milliseconds
    {
        if (polling < 1000) throw new Exception("Il polling time non può essere almeno di 1 secondo");
        this.senName = senName;
        this.ref = ref;
        this.type = type;
        this.polling = polling;
        this.topic = topic;
    }

    public static double increasingLinearBehavior(double min, double max, long passedTime, int duration) //y = x
    {
        if (min == max) return min;
        else {
            double percentageOfPassedTime = ((double) passedTime * 100) / duration;
            double valueTookFromRange = (Math.abs((max - min)) * percentageOfPassedTime) / 100;
            return min + valueTookFromRange;
        }
    }

    public static double decreasingLinearBehavior(double min, double max, long passedTime, int duration) //y = -x
    {

        double percentageOfPassedTime = ((double) passedTime * 100) / duration;
        double valueTookFromRange = ((Math.abs(max - min)) * percentageOfPassedTime) / 100;
        return max - valueTookFromRange;
    }

    public static double increasingExponentialBehavior(double min, double max, long passedTime, int duration) //y = e^(-x)
    {
        double relativeXMin = Math.log(0.1);
        double relativeXMax = Math.log(Math.abs(max - min));
        double percentageOfPassedTime = ((double) passedTime * 100) / duration;
        double valueOfXTookFromRange = relativeXMin + ((Math.abs(relativeXMax - relativeXMin) * percentageOfPassedTime) / 100);
        if ((min + Math.exp(valueOfXTookFromRange)) > max) return max;
        else
            return min + Math.exp(valueOfXTookFromRange);
    }

    public static double gaussianBehavior(double min, double max, long passedTime, int duration)  //y = e^(-(x^2))
    {
        double percentageOfPassedTime = ((double) passedTime * 100) / duration;
        double leftBorder = -2.0;
        double rightBorder = 2.0;
        double rangeBetweenBorders = Math.abs(rightBorder - leftBorder);
        double valueOfXTookFromRange = leftBorder + ((rangeBetweenBorders * percentageOfPassedTime) / 100);
        double valueOfYTookFromGaussian = Math.exp(-(Math.pow(valueOfXTookFromRange, 2)));
        double percentageOfRangeOfGaussian = (valueOfYTookFromGaussian * 100) / 1;
        double valueForMinToSum = ((Math.abs(max - min)) * percentageOfRangeOfGaussian) / 100;

        return min + valueForMinToSum;
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

    @Override
    public void start() throws Exception {

        System.out.println("Il sensore " + this.getRef() + " della simulazione con id:" + this.engine.getId() + " ha iniziato a produrre snapshot");
        vertx.setPeriodic(this.getPollingTime(), new Handler<Long>() {

            @Override
            public void handle(Long aLong) {

                JSONObject snapshot = genSnapshot();

                JSONObject jsonObjectToSend = new JSONObject().
                        put("topic", getTopic()).
                        put("snapshot", snapshot);
                vertx.eventBus().send("snapshot-for-mqtt-sender", jsonObjectToSend.toString());

                DateTime dateTime = DateTime.parse(snapshot.get("tz").toString());
                jsonObjectToSend.put("dateTime", getFixedDateTime(dateTime));
                vertx.eventBus().send(getLogger().deploymentID(), jsonObjectToSend.toString());

            }
        });

    }

    @Override
    public void stop() throws Exception {
        System.out.println("Il sensore " + this.getRef() + " della simulazione " + this.engine.getId() + " ha terminato di produrre snapshot");
    }

    public void setEngine(Engine engine) {
        this.engine = engine;
    }

    public void addModel(SnapshotModel snapshotModel) {
        try {
            this.snapshotModels.put(snapshotModel.getId(), snapshotModel);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getSenName() {
        return this.senName;
    }

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

    public Logger getLogger() {
        return this.logger;
    }

    public void setLogger(Logger logger) {
        this.logger = logger;
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

    private JSONObject genSnapshot() {

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

        if (pickedEntryModel.getValue().getMeasureTypes() != null)  //vado a vedere le misure del modello
        {
            for (Map.Entry<Integer, MeasureType> measureTypeEntry : pickedEntryModel.getValue().getMeasureTypes().entrySet()) {

                DateTime timeZoneObjectOfMeasure = new DateTime();

                JSONObject objectOfMeasure = new JSONObject();

                Double value = null;
                Double probability;
                Double variance;

                if (measureTypeEntry.getValue().getListOfTriadOfValueProbabilityVariances() != null)      //1a priorità per i valori predefiniti
                {
                    value = measureTypeEntry.getValue().getCurrentTriad().getValue();
                    probability = measureTypeEntry.getValue().getCurrentTriad().getProbability();
                    variance = measureTypeEntry.getValue().getCurrentTriad().getVariance();

                    measureTypeEntry.getValue().computeNextTriad();
                } else if (measureTypeEntry.getValue().getBehavior() != null)  //2a priorità per l'andamento
                {
                    double min = measureTypeEntry.getValue().getMinRange();
                    double max = measureTypeEntry.getValue().getMaxRange();
                    long passedTime = this.engine.getPassedTime() * 1000;
                    int periodOfTimeOfSimulation = this.engine.getPeriodOfTimeOfSimulation();

                    if (min == max) value = min;
                    else {
                        if (measureTypeEntry.getValue().getBehavior() == Behavior.INCREASINGLINEAR)
                            value = increasingLinearBehavior(min, max, passedTime, periodOfTimeOfSimulation);

                        else if (measureTypeEntry.getValue().getBehavior() == Behavior.DECREASINGLINEAR)
                            value = decreasingLinearBehavior(min, max, passedTime, periodOfTimeOfSimulation);

                        else if (measureTypeEntry.getValue().getBehavior() == Behavior.INCREASINGEXPONENTIAL)
                            value = increasingExponentialBehavior(min, max, passedTime, periodOfTimeOfSimulation);

                        else if (measureTypeEntry.getValue().getBehavior() == Behavior.DECREASINGEXPONENTIAL)
                            value = decreasingExponentialBehavior(min, max, passedTime, periodOfTimeOfSimulation);

                        else if (measureTypeEntry.getValue().getBehavior() == Behavior.SINUSOIDAL)
                            value = sinusoidalBehavior(min, max, passedTime, periodOfTimeOfSimulation);

                        else if (measureTypeEntry.getValue().getBehavior() == Behavior.COSINUSOIDAL)
                            value = cosinusoidalBehavior(min, max, passedTime, periodOfTimeOfSimulation);

                        else if (measureTypeEntry.getValue().getBehavior() == Behavior.GAUSSIAN)
                            value = gaussianBehavior(min, max, passedTime, periodOfTimeOfSimulation);
                    }

                    probability = measureTypeEntry.getValue().getProbability();
                    variance = measureTypeEntry.getValue().getVariance();

                } else        //3a priorità per il valore randomico dipendente dal range
                {
                    double min = measureTypeEntry.getValue().getMinRange();
                    double max = measureTypeEntry.getValue().getMaxRange();
                    value = (Math.random() * (max - min)) + min;
                    probability = measureTypeEntry.getValue().getProbability();
                    variance = measureTypeEntry.getValue().getVariance();
                }


                objectOfMeasure.put("k", measureTypeEntry.getValue().getKey());
                objectOfMeasure.put("u", measureTypeEntry.getValue().getUnity());
                objectOfMeasure.put("t", timeZoneObjectOfMeasure.getMillis());
                objectOfMeasure.put("tz", timeZoneObjectOfMeasure);
                objectOfMeasure.put("v", value);
                objectOfMeasure.put("s", measureTypeEntry.getValue().getSource());
                objectOfMeasure.put("d", measureTypeEntry.getValue().getDestination());
                objectOfMeasure.put("l", variance);
                objectOfMeasure.put("p", probability);

                if (measureTypeEntry.getValue().getWhichType() == 'm') {
                    m.put(objectOfMeasure);
                } else r.put(objectOfMeasure);
            }
        }
        snapshot.put("m", m);
        snapshot.put("r", r);

        return snapshot;

    }

    private Map.Entry<Integer, SnapshotModel> pickRandomModel() {

        float inf = 0;
        float sup = 0;

        float r = (float) Math.random();
        for (Map.Entry<Integer, SnapshotModel> entry : this.snapshotModels.entrySet()) {
            sup = sup + (entry.getValue().getProbability());
            if (r <= sup && r >= inf)
                return entry;
            else inf = sup;
        }

        return null;

    }

    private String getFixedDateTime(DateTime dateTime) {
        DateTimeFormatter fmt = DateTimeFormat.forPattern("dd/MM/yyyy HH:mm:ss");
        String fixedDateTime = fmt.print(dateTime);
        return fixedDateTime;
    }

    public double decreasingExponentialBehavior(double min, double max, long passedTime, int duration) //y = e^x
    {
        double relativeXMin = Math.log(0.1);
        double relativeXMax = Math.log(Math.abs(max - min));
        double percentageOfPassedTime = ((double) passedTime * 100) / duration;
        double valueOfXTookFromRange = relativeXMin + ((Math.abs(relativeXMax - relativeXMin) * percentageOfPassedTime) / 100);
        if ((max - Math.exp(valueOfXTookFromRange)) < min) return min;
        else
            return max - Math.exp(valueOfXTookFromRange);
    }

    public double sinusoidalBehavior(double min, double max, long passedTime, int duration)     //y = sin(x)
    {
        double percentageOfPassedTime = ((double) passedTime * 100) / duration;
        double valueOfXInDegrees = (360 * percentageOfPassedTime) / 100;
        double radians = Math.toRadians(valueOfXInDegrees);
        double valueOfSin = Math.sin(radians);
        double valueTookFromRangeOfSin = valueOfSin + 1;  //che va da 0 a 2
        double percentageOfRangeOfSin = (valueTookFromRangeOfSin * 100) / 2;
        double valueForMinToSum = ((Math.abs(max - min)) * percentageOfRangeOfSin) / 100;

        return min + valueForMinToSum;
    }

    public double cosinusoidalBehavior(double min, double max, long passedTime, int duration)  //y = cos(x)
    {
        double percentageOfPassedTime = ((double) passedTime * 100) / duration;
        double valueOfXInDegrees = (360 * percentageOfPassedTime) / 100;
        double radians = Math.toRadians(valueOfXInDegrees);
        double valueOfSin = Math.cos(radians);
        double valueTookFromRangeOfSin = valueOfSin + 1;  //che va da 0 a 2
        double percentageOfRangeOfSin = (valueTookFromRangeOfSin * 100) / 2;
        double valueForMinToSum = ((Math.abs(max - min)) * percentageOfRangeOfSin) / 100;

        return min + valueForMinToSum;
    }


}
