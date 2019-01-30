package it.filippetti.sp.simulator;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import it.filippetti.sp.simulator.model.Behavior;
import it.filippetti.sp.simulator.model.MeasureType;
import it.filippetti.sp.simulator.model.Model;
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
    public String topic;    //topic MQTT in cui verrano pubblicati i messaggi dal sensore
    public HashMap<Integer, Model> snapshotModels = new HashMap<Integer, Model>();
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


    public static String randomAlphaNumeric(int length, String availableChars) {  //metodo che genera una stringa con determinata lunghezza e con caratteri da usare in modo randomico
        StringBuilder builder = new StringBuilder();
        while (length-- != 0) {
            int character = (int) (Math.random() * availableChars.length());
            builder.append(availableChars.charAt(character));
        }
        return builder.toString();
    }

    public static String genId() {              //metodo utile per generare i campi uuid, e cuid (identificativi dello snapshot prodotto)
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

        System.out.println("Sensor '" + this.getRef() + "' of simulation with id: '" + this.engine.getId() + "' started to produce snapshots");

        JSONObject snapshot = genSnapshot();

        JSONObject jsonObjectToSend = new JSONObject().
                put("topic", getTopic()).
                put("snapshot", snapshot);
        vertx.eventBus().send("snapshot-for-mqtt-sender", jsonObjectToSend.toString());

        DateTime dateTime = DateTime.parse(snapshot.get("tz").toString());
        jsonObjectToSend.put("dateTime", getFixedDateTime(dateTime));
        vertx.eventBus().send(getLogger().deploymentID(), jsonObjectToSend.toString());


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
        System.out.println("Sensor '" + this.getRef() + "' of simulation with id: '" + this.engine.getId() + "' is not producing snapshots anymore");
    }

    public void setEngine(Engine engine) {
        this.engine = engine;
    }

    public void addModel(Model model) {
        try {
            this.snapshotModels.put(model.getId(), model);
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

        for (Map.Entry<Integer, Model> entry : this.snapshotModels.entrySet()) {

            Float entryProb = entry.getValue().getProbability();
            sumOfProb = sumOfProb + entryProb;
        }
        return sumOfProb;
    }

    public HashMap<Integer, Model> getSnapshotModels() {
        return this.snapshotModels;
    }

    private JSONObject genSnapshot() {      //metodo che genera lo snapshot in formato JSON

        Map.Entry<Integer, Model> pickedEntryModel = pickRandomModel();

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

                        else if (measureTypeEntry.getValue().getBehavior() == Behavior.QUADRATIC)
                            value = quadraticBehavior(min, max, passedTime, periodOfTimeOfSimulation);

                        else if (measureTypeEntry.getValue().getBehavior() == Behavior.CUBIC)
                            value = cubicBehavior(min, max, passedTime, periodOfTimeOfSimulation);

                        else if (measureTypeEntry.getValue().getBehavior() == Behavior.SQUAREROOT)
                            value = squareRootBehavior(min, max, passedTime, periodOfTimeOfSimulation);

                        else if (measureTypeEntry.getValue().getBehavior() == Behavior.CUBICROOT)
                            value = cubicRootBehavior(min, max, passedTime, periodOfTimeOfSimulation);

                        else if (measureTypeEntry.getValue().getBehavior() == Behavior.LOGARITHMIC)
                            value = logarithmicBehavior(min, max, passedTime, periodOfTimeOfSimulation);

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

    private Map.Entry<Integer, Model> pickRandomModel() {       //metodo per prendere uno dei modelli in base alla loro probabilità

        float inf = 0;
        float sup = 0;

        float r = (float) Math.random();
        for (Map.Entry<Integer, Model> entry : this.snapshotModels.entrySet()) {
            sup = sup + (entry.getValue().getProbability());
            if (r <= sup && r >= inf)
                return entry;
            else inf = sup;
        }

        return null;

    }

    private String getFixedDateTime(DateTime dateTime) {    //metodo per ottenere il formato stringa "dd/MM/yyyy HH:mm:ss" da DateTime
        DateTimeFormatter fmt = DateTimeFormat.forPattern("dd/MM/yyyy HH:mm:ss");
        String fixedDateTime = fmt.print(dateTime);
        return fixedDateTime;
    }

    private double increasingExponentialBehavior(double min, double max, long passedTime, int duration) //y = e^x
    {
        double relativeXMin = Math.log(0.1);
        double relativeXMax = Math.log(Math.abs(max - min));
        double percentageOfPassedTime = ((double) passedTime * 100) / duration;
        double valueOfXTookFromRange = relativeXMin + ((Math.abs(relativeXMax - relativeXMin) * percentageOfPassedTime) / 100);
        return min + Math.exp(valueOfXTookFromRange);
    }

    private double decreasingExponentialBehavior(double min, double max, long passedTime, int duration) //y = e^-x
    {
        double relativeXMin = -Math.log(Math.abs(max - min));
        double relativeXMax = -Math.log(0.1);
        double percentageOfPassedTime = ((double) passedTime * 100) / duration;
        double valueOfXTookFromRange = relativeXMin + ((Math.abs(relativeXMax - relativeXMin) * percentageOfPassedTime) / 100);
        return min + Math.exp(-valueOfXTookFromRange);
    }

    private double sinusoidalBehavior(double min, double max, long passedTime, int duration)     //y = sin(x)
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

    private double cosinusoidalBehavior(double min, double max, long passedTime, int duration)  //y = cos(x)
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

    private double increasingLinearBehavior(double min, double max, long passedTime, int duration) //y = x
    {
        if (min == max) return min;
        else {
            double percentageOfPassedTime = ((double) passedTime * 100) / duration;
            double valueTookFromRange = (Math.abs((max - min)) * percentageOfPassedTime) / 100;
            return min + valueTookFromRange;
        }
    }

    private double decreasingLinearBehavior(double min, double max, long passedTime, int duration) //y = -x
    {

        double percentageOfPassedTime = ((double) passedTime * 100) / duration;
        double valueTookFromRange = ((Math.abs(max - min)) * percentageOfPassedTime) / 100;
        return max - valueTookFromRange;
    }


    private double gaussianBehavior(double min, double max, long passedTime, int duration)  //y = e^(-(x^2))
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


    private double quadraticBehavior(double min, double max, long passedTime, int duration) //y = x^2
    {
        double relativeXMin = -Math.pow(Math.abs(max - min), 0.5);
        double relativeXMax = Math.pow(Math.abs(max - min), 0.5);
        double percentageOfPassedTime = ((double) passedTime * 100) / duration;
        double valueOfXTookFromRange = relativeXMin + ((Math.abs(relativeXMax - relativeXMin) * percentageOfPassedTime) / 100);
        return min + Math.pow(valueOfXTookFromRange, 2);
    }


    private double cubicBehavior(double min, double max, long passedTime, int duration) //y = x^3
    {
        double relativeXMin = Math.cbrt(-Math.abs(max - min) / 2);
        double relativeXMax = Math.cbrt(Math.abs(max - min) / 2);
        double percentageOfPassedTime = ((double) passedTime * 100) / duration;
        double valueOfXTookFromRange = relativeXMin + ((Math.abs(relativeXMax - relativeXMin) * percentageOfPassedTime) / 100);
        return max - ((Math.abs(max - min) / 2) - Math.pow(valueOfXTookFromRange, 3));
    }

    private double squareRootBehavior(double min, double max, long passedTime, int duration) //y = x^(1/2)
    {
        double relativeX = Math.pow(Math.abs(max - min), 2);
        double percentageOfPassedTime = ((double) passedTime * 100) / duration;
        double valueOfXTookFromRange = ((Math.abs(relativeX) * percentageOfPassedTime) / 100);
        return min + Math.sqrt(valueOfXTookFromRange);
    }

    private double cubicRootBehavior(double min, double max, long passedTime, int duration) //y = x^(1/3)
    {
        double relativeXMin = Math.pow(-Math.abs(max - min) / 2, 3);
        double relativeXMax = Math.pow(Math.abs(max - min) / 2, 3);
        double percentageOfPassedTime = ((double) passedTime * 100) / duration;
        double valueOfXTookFromRange = relativeXMin + ((Math.abs(relativeXMax - relativeXMin) * percentageOfPassedTime) / 100);
        return max - ((Math.abs(max - min) / 2) - Math.cbrt(valueOfXTookFromRange));
    }

    private double logarithmicBehavior(double min, double max, long passedTime, int duration) //y = ln(x)
    {
        double relativeXMin = Math.exp(-6);
        double relativeXMax = Math.exp(1.5);
        double percentageOfPassedTime = ((double) passedTime * 100) / duration;
        double valueOfXTookFromRange = relativeXMin + ((Math.abs(relativeXMax - relativeXMin) * percentageOfPassedTime) / 100);
        double percentageOfTookLog = 100 - ((Math.abs(Math.log(relativeXMax) - Math.log(valueOfXTookFromRange)) * 100) / Math.abs(Math.log(relativeXMax) - Math.log(relativeXMin)));
        return min + ((Math.abs(max - min) * percentageOfTookLog) / 100);
    }
}
