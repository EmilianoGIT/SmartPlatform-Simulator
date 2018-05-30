package it.filippetti.sp.simulator;


import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import it.filippetti.sp.simulator.model.MeasureType;
import it.filippetti.sp.simulator.model.Scenario;
import it.filippetti.sp.simulator.model.SnapshotModel;
import it.filippetti.sp.simulator.model.TypeOfArray;
import org.joda.time.DateTime;
import org.joda.time.Seconds;
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
        router.route("/").handler(routingContext -> {
            HttpServerResponse response = routingContext.response();
            response
                    .putHeader("content-type", "text/html")
                    .end("<h1>Root delle api REST con Vertx</h1>");
        });


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
        router.post("/api/simulator/new_simulation").handler(this::startSimulation);
        router.get("/api/simulator/engines").handler(this::getAllEngines);
        router.get("/api/simulator/engines/:id").handler(this::getOneEngine);
        router.get("/api/simulator/engines/:id/stop_simulation").handler(this::stopSimulation);
        router.delete("/api/simulator/engines/:id").handler(this::deleteEngine);
        router.get("/api/simulator/engines/:id/snapshots").handler(this::getProducedSnapshots);

    }

    @Override
    public void stop() {

    }


    private void startSimulation(RoutingContext routingContext) {

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
                    .end(new JSONObject().put("result", "Nice JSON").toString());
            System.out.println(routingContext.getBody().toString());
        } catch (Exception e) {
            e.printStackTrace();
            routingContext.response()
                    .setStatusCode(404)
                    .putHeader("content-type", "application/json; charset=utf-8")
                    .putHeader("Access-Control-Allow-Origin", "*")
                    .putHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
                    .putHeader("Access-Control-Allow-Credentials", "true")
                    .end(new JSONObject().put("result", "Bad JSON").toString());
            System.out.println(routingContext.getBody().toString());
        }
    }


    private void stopSimulation(RoutingContext routingContext) {

        try {

            String id = routingContext.request().getParam("id");
            if (id == null) {
                routingContext.response().setStatusCode(400)
                        .putHeader("content-type", "application/json; charset=utf-8")
                        .putHeader("Access-Control-Allow-Origin", "*")
                        .putHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
                        .putHeader("Access-Control-Allow-Credentials", "true")
                        .end();
            } else {
                Integer idAsInteger = Integer.valueOf(id);
                if (engines.get(idAsInteger) == null)
                    routingContext.response().setStatusCode(404).end();

                else vertx.undeploy(engines.get(idAsInteger).deploymentID());
            }
            routingContext.response().setStatusCode(204)
                    .putHeader("content-type", "application/json; charset=utf-8")
                    .putHeader("Access-Control-Allow-Origin", "*")
                    .putHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
                    .putHeader("Access-Control-Allow-Credentials", "true")
                    .end();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void deleteEngine(RoutingContext routingContext) {

        try {

            String id = routingContext.request().getParam("id");
            if (id == null) {
                routingContext.response().setStatusCode(400)
                        .putHeader("content-type", "application/json; charset=utf-8")
                        .putHeader("Access-Control-Allow-Origin", "*")
                        .putHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
                        .putHeader("Access-Control-Allow-Credentials", "true")
                        .end();
            } else {
                Integer idAsInteger = Integer.valueOf(id);
                if (engines.get(idAsInteger) == null)
                    routingContext.response().setStatusCode(404).end();
                else {
                    vertx.undeploy(engines.get(idAsInteger).deploymentID());
                    engines.remove(idAsInteger);
                }

            }
            routingContext.response().setStatusCode(204)
                    .putHeader("content-type", "application/json; charset=utf-8")
                    .putHeader("Access-Control-Allow-Origin", "*")
                    .putHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
                    .putHeader("Access-Control-Allow-Credentials", "true")
                    .end();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void getAllEngines(RoutingContext routingContext) {

        JSONArray jsonArrayOfEngines = new JSONArray();

        for (Map.Entry<Integer, Engine> entry : this.engines.entrySet()) {

            JSONObject jsonObjectOfEngine = new JSONObject();

            jsonObjectOfEngine.put("id", entry.getKey());
            jsonObjectOfEngine.put("simulationStartDate", entry.getValue().getSimulationStartDate().toString());
            jsonObjectOfEngine.put("simulationEndDate", entry.getValue().getSimulationEndDate().toString());
            jsonObjectOfEngine.put("progressionPercentage", entry.getValue().getProgressionPercentage());

            jsonArrayOfEngines.put(jsonObjectOfEngine);

        }

        routingContext.response()
                .putHeader("content-type", "application/json; charset=utf-8")
                .putHeader("Access-Control-Allow-Origin", "*")
                .putHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
                .putHeader("Access-Control-Allow-Credentials", "true")
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
                JSONObject jsonObjectOfEngine=new JSONObject();
                jsonObjectOfEngine.put("id", engines.get(idAsInteger).getId());
                jsonObjectOfEngine.put("simulationStartDate", engines.get(idAsInteger).getSimulationStartDate().toString());
                jsonObjectOfEngine.put("simulationEndDate", engines.get(idAsInteger).getSimulationEndDate().toString());
                jsonObjectOfEngine.put("progressionPercentage", engines.get(idAsInteger).getProgressionPercentage());
                routingContext.response()
                        .putHeader("content-type", "application/json; charset=utf-8")
                        .putHeader("Access-Control-Allow-Origin", "*")
                        .putHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
                        .putHeader("Access-Control-Allow-Credentials", "true")
                        .end(jsonObjectOfEngine.toString());
            }
        }

    }

    private void getProducedSnapshots(RoutingContext routingContext) {


        String id = routingContext.request().getParam("id");
        if (id == null) {
            routingContext.response().setStatusCode(400)
                    .putHeader("content-type", "application/json; charset=utf-8")
                    .putHeader("Access-Control-Allow-Origin", "*")
                    .putHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
                    .putHeader("Access-Control-Allow-Credentials", "true")
                    .end();
        } else {
            Integer idAsInteger = Integer.valueOf(id);
            if (engines.get(idAsInteger) == null)
                routingContext.response().setStatusCode(404).end();
            else {
                routingContext.response()
                        .putHeader("content-type", "application/json; charset=utf-8")
                        .putHeader("Access-Control-Allow-Origin", "*")
                        .putHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
                        .putHeader("Access-Control-Allow-Credentials", "true")
                        .end(engines.get(idAsInteger).getLogger().producedSnapshots.toString());
            }
        }


    }


    private SetOfInstancesForSimulation fromJsonForSimulationToInstancesForSimulation(JSONObject jsonOfSimulation) throws Exception, ErrorDate {

        Scenario instanceOfScenario;
        List<Sensor> sensorsToInsertInScenario = new ArrayList<>();
        DateTime ssd;
        DateTime sed;
        int periodOfTimeOfSimulation; //in minutes


        try {
            if (jsonOfSimulation.get("simStartDate").toString().equals(""))
                ssd = DateTime.now();
            else ssd = DateTime.parse(jsonOfSimulation.get("simStartDate").toString());

            String regexTimer = "[0-9][0-9]:[0-5][0-9]:[0-5][0-9]";
            Pattern pattern = Pattern.compile(regexTimer);
            String simDuration = jsonOfSimulation.get("simDuration").toString();
            Matcher matcher = pattern.matcher(simDuration);

            if (matcher.find()) {
                int numberOfHoursInInt = (Character.getNumericValue(simDuration.charAt(0)) * 10) + (Character.getNumericValue(simDuration.charAt(1)));
                int numberOfMinutesInInt = (Character.getNumericValue(simDuration.charAt(3)) * 10) + (Character.getNumericValue(simDuration.charAt(4)));
                int numberOfSecondsInInt = (Character.getNumericValue(simDuration.charAt(6)) * 10) + (Character.getNumericValue(simDuration.charAt(7)));

                int hoursInMillis = numberOfHoursInInt * 3600000;
                int minutesInMillis = numberOfMinutesInInt * 60000;
                int secondsInMillis = numberOfSecondsInInt * 1000;


                periodOfTimeOfSimulation = hoursInMillis + minutesInMillis + secondsInMillis;
            } else throw new Exception();


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

                Sensor currentSensor = new Sensor(senName, ref, type, polling, topic);


                JSONArray jsonArrayOfSnapshotModelsOfSensor = jsonObjectOfSensor.getJSONArray("models");

                for (int j = 0; j < jsonArrayOfSnapshotModelsOfSensor.length(); j++) {
                    JSONObject jsonObjectOfSnapshotModelOfSensor = jsonArrayOfSnapshotModelsOfSensor.getJSONObject(j);


                    String modelName = jsonObjectOfSnapshotModelOfSensor.get("modName").toString();
                    String cat = jsonObjectOfSnapshotModelOfSensor.get("cat").toString();
                    String stringProbability = jsonObjectOfSnapshotModelOfSensor.get("probability").toString();
                    float probability = Float.parseFloat(stringProbability);

                    SnapshotModel currentSnapshotModel = new SnapshotModel(modelName, cat, probability);
                    currentSensor.addModel(currentSnapshotModel);


                    JSONArray jsonArrayOfMeasureTypesOfSnapshotModelOfSensor = jsonObjectOfSnapshotModelOfSensor.getJSONArray("measures");
                    for (int k = 0; k < jsonArrayOfMeasureTypesOfSnapshotModelOfSensor.length(); k++) {

                        JSONObject jsonObjectOfMeasureTypeOfSnapshotModelOfSensor = jsonArrayOfMeasureTypesOfSnapshotModelOfSensor.getJSONObject(k);

                        String meaName = jsonObjectOfMeasureTypeOfSnapshotModelOfSensor.get("meaName").toString();
                        String key = jsonObjectOfMeasureTypeOfSnapshotModelOfSensor.get("key").toString();
                        String unity = jsonObjectOfMeasureTypeOfSnapshotModelOfSensor.get("unity").toString();
                        String min = jsonObjectOfMeasureTypeOfSnapshotModelOfSensor.get("min").toString();
                        String max = jsonObjectOfMeasureTypeOfSnapshotModelOfSensor.get("max").toString();
                        String source = jsonObjectOfMeasureTypeOfSnapshotModelOfSensor.get("source").toString();
                        String destination = jsonObjectOfMeasureTypeOfSnapshotModelOfSensor.get("destination").toString();
                        String selArray = jsonObjectOfMeasureTypeOfSnapshotModelOfSensor.get("selArray").toString();

                       Double prob;
                       Double variance;

                        if (source.equals("")) source = null;
                        if (destination.equals("")) destination = null;

                        if (jsonObjectOfMeasureTypeOfSnapshotModelOfSensor.get("prob").toString().equals("")) prob = null;
                        else prob = Double.parseDouble(jsonObjectOfMeasureTypeOfSnapshotModelOfSensor.get("prob").toString());

                        if (jsonObjectOfMeasureTypeOfSnapshotModelOfSensor.get("variance").toString().equals("")) variance = null;
                        else variance = Double.parseDouble(jsonObjectOfMeasureTypeOfSnapshotModelOfSensor.get("variance").toString());

                        MeasureType currentMeasureType = new MeasureType(meaName, key, unity, min, max, source, destination, prob, variance, TypeOfArray.valueOf(selArray));
                        currentSnapshotModel.addMeasureType(currentMeasureType);
                    }
                }
                sensorsToInsertInScenario.add(currentSensor);
            }

            instanceOfScenario = new Scenario(scenName, sensorsToInsertInScenario.get(0));
            if (sensorsToInsertInScenario.size() - 1 > 0) {
                for (int i = 1; i < sensorsToInsertInScenario.size(); i++) {
                    instanceOfScenario.addSensor(sensorsToInsertInScenario.get(i));
                }
            }
            return new SetOfInstancesForSimulation(instanceOfScenario, ssd, periodOfTimeOfSimulation);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private class SetOfInstancesForSimulation {
        Scenario s;
        DateTime simulationStartDate;
        DateTime simulationEndDate;
        int periodOfTimeOfSimulation;

        public SetOfInstancesForSimulation(Scenario s, DateTime simulationStartDate, int periodOfTimeOfSimulation) {
            this.s = s;
            this.simulationStartDate = simulationStartDate;

            this.periodOfTimeOfSimulation = periodOfTimeOfSimulation;
        }

        public Scenario getScenario() {
            return this.s;
        }

        public DateTime getSimulationStartDate() {
            return this.simulationStartDate;
        }

        public int getPeriodOfTimeOfSimulation() {
            return this.periodOfTimeOfSimulation;
        }
    }


}