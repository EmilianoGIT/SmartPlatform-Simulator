package it.filippetti.sp.simulator;


import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import it.filippetti.sp.simulator.model.*;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class ServerRestSimulation extends AbstractVerticle {

    int port;
    HashMap<Integer, Engine> engines = new HashMap<Integer, Engine>();

    public ServerRestSimulation(int port) {
        this.port = port;
    }

    @Override
    public void start(Future<Void> fut) throws InterruptedException {

        Router router = Router.router(vertx);

        router.route().handler(CorsHandler.create("*")
                .allowedMethod(io.vertx.core.http.HttpMethod.GET)
                .allowedMethod(io.vertx.core.http.HttpMethod.POST)
                .allowedMethod(io.vertx.core.http.HttpMethod.DELETE)
                .allowedMethod(io.vertx.core.http.HttpMethod.PUT)
                .allowedMethod(io.vertx.core.http.HttpMethod.OPTIONS)
                .allowedHeader("Access-Control-Allow-Method")
                .allowedHeader("Access-Control-Allow-Origin")
                .allowedHeader("Access-Control-Allow-Credentials")
                .allowedHeader("Content-Type"));

        vertx
                .createHttpServer()
                .requestHandler(router::accept)
                .listen(
                        // Retrieve the port from the configuration,
                        // default to 8080.
                        config().getInteger("http.port", port),
                        result -> {
                            if (result.succeeded()) {
                                fut.complete();
                            } else {
                                fut.fail(result.cause());
                            }
                        }
                );

        router.route().handler(BodyHandler.create());
        router.post("/api/v1.0/simulator/new_simulation").handler(this::newSimulation);
        router.get("/api/v1.0/simulator/simulations/:id/play").handler(this::playSimulation);
        router.get("/api/v1.0/simulator/simulations/:id/stop").handler(this::stopSimulation);
        router.get("/api/v1.0/simulator/simulations/:id/pause").handler(this::pauseSimulation);
        router.delete("/api/v1.0/simulator/simulations/:id").handler(this::deleteEngine);
        router.get("/api/v1.0/simulator/simulations").handler(this::getAllEngines);
        router.get("/api/v1.0/simulator/simulations/:id").handler(this::getOneEngine);
        router.get("/api/v1.0/simulator/simulations/:id/snapshots").handler(this::getProducedSnapshots);

    }

    @Override
    public void stop() {

    }


    private void newSimulation(RoutingContext routingContext) {


        try {

            SetOfInstancesForSimulation setOfInstancesForSimulation = fromJsonForSimulationToInstancesForSimulation(new JSONObject(routingContext.getBodyAsString()));
            Engine engine = new Engine(setOfInstancesForSimulation.getSimulationStartDate(), setOfInstancesForSimulation.getPeriodOfTimeOfSimulation(), setOfInstancesForSimulation.getScenario());
            engines.put(engine.getId(), engine);
            vertx.deployVerticle(engine);
            routingContext.response()
                    .setStatusCode(201)
                    .putHeader("content-type", "application/json; charset=utf-8")
                    .putHeader("Access-Control-Allow-Origin", "*")
                    .putHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
                    .putHeader("Access-Control-Allow-Credentials", "true")
                    .end(new JSONObject().put("simulationId", engine.getId()).toString());
            System.out.println(routingContext.getBody().toString());
        } catch (Exception e) {
            e.printStackTrace();
            routingContext.response()
                    .setStatusCode(400)
                    .putHeader("content-type", "application/json; charset=utf-8")
                    .putHeader("Access-Control-Allow-Origin", "*")
                    .putHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
                    .putHeader("Access-Control-Allow-Credentials", "true")
                    .end(new JSONObject().put("Error", e.getMessage()).toString());
            System.out.println(routingContext.getBody().toString());
            System.out.println(e.getMessage());
        }


    }


    private void playSimulation(RoutingContext routingContext) {

        routingContext.response()
                .putHeader("content-type", "application/json; charset=utf-8")
                .putHeader("Access-Control-Allow-Origin", "*")
                .putHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
                .putHeader("Access-Control-Allow-Credentials", "true");
        String id = routingContext.request().getParam("id");
        if (id == null) {
            routingContext.response()
                    .setStatusCode(400)
                    .end();
        } else {

            Integer idAsInteger = Integer.valueOf(id);
            if (engines.get(idAsInteger) == null) {
                routingContext.response()
                        .setStatusCode(404)
                        .end();
            } else {
                vertx.eventBus().send("commands" + idAsInteger, "play");
                routingContext.response()
                        .setStatusCode(200).
                        end();
            }

        }
    }

    private void pauseSimulation(RoutingContext routingContext) {

        routingContext.response()
                .putHeader("content-type", "application/json; charset=utf-8")
                .putHeader("Access-Control-Allow-Origin", "*")
                .putHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
                .putHeader("Access-Control-Allow-Credentials", "true");
        String id = routingContext.request().getParam("id");
        if (id == null) {
            routingContext.response()
                    .setStatusCode(400)
                    .end();
        } else {

            Integer idAsInteger = Integer.valueOf(id);
            if (engines.get(idAsInteger) == null) {
                routingContext.response()
                        .setStatusCode(404)
                        .end();
            } else {
                vertx.eventBus().send("commands" + idAsInteger, "pause");
                routingContext.response()
                        .setStatusCode(200).
                        end();
            }

        }
    }


    private void stopSimulation(RoutingContext routingContext) {

        routingContext.response()
                .putHeader("content-type", "application/json; charset=utf-8")
                .putHeader("Access-Control-Allow-Origin", "*")
                .putHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
                .putHeader("Access-Control-Allow-Credentials", "true");
        String id = routingContext.request().getParam("id");
        if (id == null) {
            routingContext.response()
                    .setStatusCode(400)
                    .end();
        } else {

            Integer idAsInteger = Integer.valueOf(id);
            if (engines.get(idAsInteger) == null) {
                routingContext.response()
                        .setStatusCode(404)
                        .end();
            } else {
                vertx.eventBus().send("commands" + idAsInteger, "stop");
                routingContext.response()
                        .setStatusCode(200).
                        end();
            }

        }
    }


    private void deleteEngine(RoutingContext routingContext) {
        routingContext.response()
                .putHeader("Access-Control-Allow-Origin", "*")
                .putHeader("Access-Control-Allow-Methods", "GET, POST, DELETE, OPTIONS")
                .putHeader("Access-Control-Allow-Credentials", "true");
        String id = routingContext.request().getParam("id");
        if (id == null) {
            routingContext.response().setStatusCode(400).end();
        } else {
            Integer idAsInteger = Integer.valueOf(id);

            vertx.undeploy(engines.get(idAsInteger).getLogger().deploymentID());
            vertx.eventBus().send("commands" + idAsInteger, "stop");
            engines.remove(idAsInteger);
        }
        routingContext.response().setStatusCode(204).end();

    }

    private void getAllEngines(RoutingContext routingContext) {

        JSONArray jsonArrayOfEngines = new JSONArray();

        for (Map.Entry<Integer, Engine> entry : this.engines.entrySet()) {

            JSONObject jsonObjectOfEngine = new JSONObject();

            jsonObjectOfEngine.put("id", entry.getKey());
            jsonObjectOfEngine.put("startDate", getFixedDateTime(entry.getValue().getSimulationStartDate()));
            if (entry.getValue().getCurrentState().equals("Paused"))
                jsonObjectOfEngine.put("endDate", "--/--/---- --:--:--");
            else jsonObjectOfEngine.put("endDate", getFixedDateTime(entry.getValue().getSimulationEndDate()));
            jsonObjectOfEngine.put("progressionPercentage", entry.getValue().getProgressOfSimulation());
            jsonObjectOfEngine.put("currentState", entry.getValue().getCurrentState());
            jsonObjectOfEngine.put("sceName", entry.getValue().getScenario().getScenName());

            jsonArrayOfEngines.put(jsonObjectOfEngine);

        }
        routingContext.response()
                .putHeader("content-type", "application/json; charset=utf-8")
                .putHeader("Access-Control-Allow-Origin", "*")
                .putHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
                .putHeader("Access-Control-Allow-Credentials", "true")
                .setStatusCode(200)
                .end(jsonArrayOfEngines.toString());
    }

    private void getOneEngine(RoutingContext routingContext) {

        final String id = routingContext.request().getParam("id");
        if (id == null) {
            routingContext.response().setStatusCode(400).end();
        } else {
            final Integer idAsInteger = Integer.valueOf(id);
            Integer idOfEngine = engines.get(idAsInteger).getId();
            if (idOfEngine == null) {
                routingContext.response().setStatusCode(404).end();
            } else {
                JSONObject jsonObjectOfEngine = new JSONObject();
                jsonObjectOfEngine.put("id", engines.get(idAsInteger).getId());
                jsonObjectOfEngine.put("startDate", getFixedDateTime(engines.get(idAsInteger).getSimulationStartDate()));
                if (engines.get(idAsInteger).getCurrentState().equals("Paused"))
                    jsonObjectOfEngine.put("endDate", "--/--/---- --:--:--");
                else
                    jsonObjectOfEngine.put("endDate", getFixedDateTime(engines.get(idAsInteger).getSimulationEndDate()));
                jsonObjectOfEngine.put("progressionPercentage", engines.get(idAsInteger).getProgressOfSimulation());
                jsonObjectOfEngine.put("currentState", engines.get(idAsInteger).getCurrentState());
                jsonObjectOfEngine.put("sceName", engines.get(idAsInteger).getScenario().getScenName());
                routingContext.response()
                        .putHeader("content-type", "application/json; charset=utf-8")
                        .putHeader("Access-Control-Allow-Origin", "*")
                        .putHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
                        .putHeader("Access-Control-Allow-Credentials", "true")
                        .setStatusCode(200)
                        .end(jsonObjectOfEngine.toString());
            }
        }

    }

    private void getProducedSnapshots(RoutingContext routingContext) {

        routingContext.response()
                .putHeader("content-type", "application/json; charset=utf-8")
                .putHeader("Access-Control-Allow-Origin", "*")
                .putHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
                .putHeader("Access-Control-Allow-Credentials", "true");

        String id = routingContext.request().getParam("id");
        if (id == null) {
            routingContext.response().setStatusCode(400)
                    .end();
        } else {
            Integer idAsInteger = Integer.valueOf(id);
            if (engines.get(idAsInteger) == null)
                routingContext.response().setStatusCode(404).end();
            else {
                routingContext.response()
                        .setStatusCode(200)
                        .end(engines.get(idAsInteger).getLogger().producedSnapshots.toString());

            }
        }

    }


    private SetOfInstancesForSimulation fromJsonForSimulationToInstancesForSimulation(JSONObject jsonOfSimulation) throws Exception {

        Scenario scenario;
        List<Sensor> sensorsToInsertInScenario = new ArrayList<>();
        DateTime simulationStartDate;
        int periodOfTimeOfSimulation; //in secondi


        if (jsonOfSimulation.get("simStartDate").toString().equals(""))
            simulationStartDate = DateTime.now();
        else simulationStartDate = DateTime.parse(jsonOfSimulation.get("simStartDate").toString());

        String regexTimer = "([0-9][0-9]:[0-5][0-9]:[0-5][0-9])|([0-9][0-9]:[0-5][0-9])";  // regular expression per il formato hh:mm:ss o hh:mm
        Pattern pattern = Pattern.compile(regexTimer);
        String simDuration = jsonOfSimulation.get("simDuration").toString();
        if (simDuration.equals("00:00:00") || simDuration.equals("00:00"))
            throw new Exception("La durata di simulazione non può stare a " + simDuration);
        Matcher matcher = pattern.matcher(simDuration);

        if (matcher.find() && ((simDuration.length() == 8) || simDuration.length() == 5)) {
            int numberOfHoursInInt = (Character.getNumericValue(simDuration.charAt(0)) * 10) + (Character.getNumericValue(simDuration.charAt(1)));
            int numberOfMinutesInInt = (Character.getNumericValue(simDuration.charAt(3)) * 10) + (Character.getNumericValue(simDuration.charAt(4)));
            int numberOfSecondsInInt;
            if (simDuration.length() == 8) {
                numberOfSecondsInInt = (Character.getNumericValue(simDuration.charAt(6)) * 10) + (Character.getNumericValue(simDuration.charAt(7)));
            } else numberOfSecondsInInt = 0;

            int hoursInMillis = numberOfHoursInInt * 3600000;
            int minutesInMillis = numberOfMinutesInInt * 60000;
            int secondsInMillis = numberOfSecondsInInt * 1000;

            periodOfTimeOfSimulation = hoursInMillis + minutesInMillis + secondsInMillis;
        } else throw new Exception("Formato durarata di simulazione non inserito correttamente: " + simDuration);

        JSONObject jsonObjectOfScenario = jsonOfSimulation.getJSONObject("scenario");
        String scenName = jsonObjectOfScenario.get("sceName").toString();
        JSONArray jsonArrayOfSensors = jsonObjectOfScenario.getJSONArray("sensors");
        for (int i = 0; i < jsonArrayOfSensors.length(); i++) {

            JSONObject jsonObjectOfSensor = jsonArrayOfSensors.getJSONObject(i);
            String senName = jsonObjectOfSensor.get("senName").toString();
            String ref = jsonObjectOfSensor.get("ref").toString();
            String type = jsonObjectOfSensor.get("type").toString();
            String stringPolling = jsonObjectOfSensor.get("polling").toString();
            long polling = Long.parseLong(stringPolling);
            String topic = jsonObjectOfSensor.get("topic").toString();

            if (polling < 1) throw new Exception("Il polling time del sensore deve essere almeno di 1 secondo");

            Sensor currentSensor = new Sensor(senName, ref, type, polling * 1000, topic);


            JSONArray jsonArrayOfSnapshotModelsOfSensor = jsonObjectOfSensor.getJSONArray("models");

            for (int j = 0; j < jsonArrayOfSnapshotModelsOfSensor.length(); j++) {
                JSONObject jsonObjectOfSnapshotModelOfSensor = jsonArrayOfSnapshotModelsOfSensor.getJSONObject(j);


                String modelName = jsonObjectOfSnapshotModelOfSensor.get("modName").toString();
                String cat = jsonObjectOfSnapshotModelOfSensor.get("cat").toString();
                String stringProbability = jsonObjectOfSnapshotModelOfSensor.get("probability").toString();
                float probability = Float.parseFloat(stringProbability);

                SnapshotModel currentSnapshotModel = new SnapshotModel(modelName, cat, probability);
                currentSensor.addModel(currentSnapshotModel);

                MeasureType currentMeasureType;
                JSONArray jsonArrayOfMeasureTypesOfSnapshotModelOfSensor = jsonObjectOfSnapshotModelOfSensor.getJSONArray("measures");
                for (int k = 0; k < jsonArrayOfMeasureTypesOfSnapshotModelOfSensor.length(); k++) {


                    JSONObject jsonObjectOfMeasureTypeOfSnapshotModelOfSensor = jsonArrayOfMeasureTypesOfSnapshotModelOfSensor.getJSONObject(k);

                    String meaName = jsonObjectOfMeasureTypeOfSnapshotModelOfSensor.get("meaName").toString();
                    String key = jsonObjectOfMeasureTypeOfSnapshotModelOfSensor.get("key").toString();
                    String unity = jsonObjectOfMeasureTypeOfSnapshotModelOfSensor.get("unity").toString();
                    String source = jsonObjectOfMeasureTypeOfSnapshotModelOfSensor.get("source").toString();
                    String destination = jsonObjectOfMeasureTypeOfSnapshotModelOfSensor.get("destination").toString();
                    String selArray = jsonObjectOfMeasureTypeOfSnapshotModelOfSensor.get("selArray").toString();
                    if (source.equals("")) source = null;
                    if (destination.equals("")) destination = null;

                    List<TriadOfValueProbabilityVariance> listOfTriadsOfValueProbabilityVariance = new ArrayList<TriadOfValueProbabilityVariance>();

                    if (jsonObjectOfMeasureTypeOfSnapshotModelOfSensor.has("values")) //1a priorità per i valori predefiniti
                    {
                        JSONArray jsonArrayOfTriadsOfValueProbabilityVariance = jsonObjectOfMeasureTypeOfSnapshotModelOfSensor.getJSONArray("values");
                        if (jsonArrayOfTriadsOfValueProbabilityVariance.length() == 0)
                            throw new Exception("L'array di values è vuoto");
                        else {
                            for (int l = 0; l < jsonArrayOfTriadsOfValueProbabilityVariance.length(); l++) {
                                JSONObject jsonObjectOfTriadOfValueProbabilityVariance = jsonArrayOfTriadsOfValueProbabilityVariance.getJSONObject(l);

                                Double valueOfTriad = jsonObjectOfTriadOfValueProbabilityVariance.getDouble("val");
                                Double probabilityOfTriad = jsonObjectOfTriadOfValueProbabilityVariance.getDouble("prob");
                                Double varianceOfTriad = jsonObjectOfTriadOfValueProbabilityVariance.getDouble("var");

                                listOfTriadsOfValueProbabilityVariance.add(new TriadOfValueProbabilityVariance(valueOfTriad, probabilityOfTriad, varianceOfTriad));
                            }

                        }
                        currentMeasureType = new MeasureType(meaName, key, unity, source, destination, TypeOfArray.valueOf(selArray), listOfTriadsOfValueProbabilityVariance);
                        currentSnapshotModel.addMeasureType(currentMeasureType);
                    } else if (jsonObjectOfMeasureTypeOfSnapshotModelOfSensor.has("behavior"))      //2a priorità all'andamento
                    {

                        Behavior behavior;
                        if (jsonObjectOfMeasureTypeOfSnapshotModelOfSensor.get("behavior").toString().equals("increasing-linear"))
                            behavior = Behavior.INCREASINGLINEAR;

                        else if (jsonObjectOfMeasureTypeOfSnapshotModelOfSensor.get("behavior").toString().equals("decreasing-linear"))
                            behavior = Behavior.DECREASINGLINEAR;

                        else if (jsonObjectOfMeasureTypeOfSnapshotModelOfSensor.get("behavior").toString().equals("increasing-exponential"))
                            behavior = Behavior.INCREASINGEXPONENTIAL;

                        else if (jsonObjectOfMeasureTypeOfSnapshotModelOfSensor.get("behavior").toString().equals("decreasing-exponential"))
                            behavior = Behavior.DECREASINGEXPONENTIAL;

                        else if (jsonObjectOfMeasureTypeOfSnapshotModelOfSensor.get("behavior").toString().equals("sinusoidal"))
                            behavior = Behavior.SINUSOIDAL;

                        else if (jsonObjectOfMeasureTypeOfSnapshotModelOfSensor.get("behavior").toString().equals("cosinusoidal"))
                            behavior = Behavior.COSINUSOIDAL;

                        else if (jsonObjectOfMeasureTypeOfSnapshotModelOfSensor.get("behavior").toString().equals("gaussian"))
                            behavior = Behavior.GAUSSIAN;
                        else
                            throw new Exception("L'andamento " + jsonObjectOfMeasureTypeOfSnapshotModelOfSensor.get("behavior").toString() + " non è supportato dal simulatore");
                        Double min = Double.parseDouble(jsonObjectOfMeasureTypeOfSnapshotModelOfSensor.get("min").toString());
                        Double max = Double.parseDouble(jsonObjectOfMeasureTypeOfSnapshotModelOfSensor.get("max").toString());
                        if (min > max)
                            throw new Exception("Min non può essere > di Max");
                        Double prob;
                        Double variance;

                        if (jsonObjectOfMeasureTypeOfSnapshotModelOfSensor.get("prob").toString().equals(""))
                            prob = null;
                        else
                            prob = Double.parseDouble(jsonObjectOfMeasureTypeOfSnapshotModelOfSensor.get("prob").toString());

                        if (jsonObjectOfMeasureTypeOfSnapshotModelOfSensor.get("variance").toString().equals(""))
                            variance = null;
                        else
                            variance = Double.parseDouble(jsonObjectOfMeasureTypeOfSnapshotModelOfSensor.get("variance").toString());

                        currentMeasureType = new MeasureType(meaName, key, unity, min, max, source, destination, prob, variance, TypeOfArray.valueOf(selArray), behavior);
                        currentSnapshotModel.addMeasureType(currentMeasureType);
                    } else {

                        Double min = Double.parseDouble(jsonObjectOfMeasureTypeOfSnapshotModelOfSensor.get("min").toString());
                        Double max = Double.parseDouble(jsonObjectOfMeasureTypeOfSnapshotModelOfSensor.get("max").toString());
                        if (min > max)
                            throw new Exception("Min non può essere > di Max");
                        Double prob;
                        Double variance;

                        if (jsonObjectOfMeasureTypeOfSnapshotModelOfSensor.get("prob").toString().equals(""))
                            prob = null;
                        else
                            prob = Double.parseDouble(jsonObjectOfMeasureTypeOfSnapshotModelOfSensor.get("prob").toString());

                        if (jsonObjectOfMeasureTypeOfSnapshotModelOfSensor.get("variance").toString().equals(""))
                            variance = null;
                        else
                            variance = Double.parseDouble(jsonObjectOfMeasureTypeOfSnapshotModelOfSensor.get("variance").toString());
                        currentMeasureType = new MeasureType(meaName, key, unity, min, max, source, destination, prob, variance, TypeOfArray.valueOf(selArray));
                        currentSnapshotModel.addMeasureType(currentMeasureType);
                    }
                }
            }
            sensorsToInsertInScenario.add(currentSensor);
        }

        scenario = new Scenario(scenName, sensorsToInsertInScenario.get(0));
        if (sensorsToInsertInScenario.size() - 1 > 0) {
            for (int i = 1; i < sensorsToInsertInScenario.size(); i++) {
                scenario.addSensor(sensorsToInsertInScenario.get(i));
            }
        }
        return new SetOfInstancesForSimulation(scenario, simulationStartDate, periodOfTimeOfSimulation);

    }

    private String getFixedDateTime(DateTime dateTime) {
        DateTimeFormatter fmt = DateTimeFormat.forPattern("dd/MM/yyyy HH:mm:ss");
        String fixedDateTime = fmt.print(dateTime);
        return fixedDateTime;
    }

    private class SetOfInstancesForSimulation {         //classe utile per il raggruppamento delle istanze da passare all'engine
        Scenario scenario;
        DateTime simulationStartDate;
        int periodOfTimeOfSimulation;

        private SetOfInstancesForSimulation(Scenario s, DateTime simulationStartDate, int periodOfTimeOfSimulation) {
            this.scenario = s;
            this.simulationStartDate = simulationStartDate;

            this.periodOfTimeOfSimulation = periodOfTimeOfSimulation;
        }


        private Scenario getScenario() {
            return this.scenario;
        }

        private DateTime getSimulationStartDate() {
            return this.simulationStartDate;
        }

        private int getPeriodOfTimeOfSimulation() {
            return this.periodOfTimeOfSimulation;
        }


    }

}