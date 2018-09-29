package it.filippetti.sp.simulator;


import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import it.filippetti.sp.simulator.model.MeasureType;
import it.filippetti.sp.simulator.model.Scenario;
import it.filippetti.sp.simulator.model.SnapshotModel;
import it.filippetti.sp.simulator.model.TypeOfArray;
import org.joda.time.DateTime;
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
                    .end(new JSONObject().put("result", "Nice JSON")
                            .put("simulationId", engine.getId()).toString());
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



    private void playSimulation(RoutingContext routingContext) {

        routingContext.response()
                .putHeader("content-type", "application/json; charset=utf-8")
                .putHeader("Access-Control-Allow-Origin", "*")
                .putHeader("Access-Control-Allow-Methods","GET, POST, OPTIONS")
                .putHeader("Access-Control-Allow-Credentials", "true");
        String id = routingContext.request().getParam("id");
        if (id == null) {
            routingContext.response()
                    .setStatusCode(400)
                    .end();
        } else {

            Integer idAsInteger = Integer.valueOf(id);
            if (engines.get(idAsInteger) == null){
                routingContext.response()
                        .setStatusCode(404)
                        .end();
            }
            else{
                vertx.eventBus().send("commands"+idAsInteger,"play");
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
                .putHeader("Access-Control-Allow-Methods","GET, POST, OPTIONS")
                .putHeader("Access-Control-Allow-Credentials", "true");
        String id = routingContext.request().getParam("id");
        if (id == null) {
            routingContext.response()
                    .setStatusCode(400)
                    .end();
        } else {

            Integer idAsInteger = Integer.valueOf(id);
            if (engines.get(idAsInteger) == null){
                routingContext.response()
                        .setStatusCode(404)
                        .end();
            }
            else{
                vertx.eventBus().send("commands"+idAsInteger,"pause");
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
                .putHeader("Access-Control-Allow-Methods","GET, POST, OPTIONS")
                .putHeader("Access-Control-Allow-Credentials", "true");
        String id = routingContext.request().getParam("id");
        if (id == null) {
            routingContext.response()
                    .setStatusCode(400)
                    .end();
        } else {

            Integer idAsInteger = Integer.valueOf(id);
            if (engines.get(idAsInteger) == null){
                routingContext.response()
                        .setStatusCode(404)
                        .end();
            }
            else{
                vertx.eventBus().send("commands"+idAsInteger,"stop");
                routingContext.response()
                        .setStatusCode(200).
                        end();
            }

        }
    }



    private void deleteEngine(RoutingContext routingContext) {

        routingContext.response()
                .putHeader("content-type", "application/json; charset=utf-8")
                .putHeader("Access-Control-Allow-Origin", "*")
                .putHeader("Access-Control-Allow-Methods","GET, POST, OPTIONS")
                .putHeader("Access-Control-Allow-Credentials", "true");
        String id = routingContext.request().getParam("id");
        if (id == null) {
            routingContext.response()
                    .setStatusCode(400)
                    .end();
        } else {

            Integer idAsInteger = Integer.valueOf(id);
            if (engines.get(idAsInteger) == null){
                routingContext.response()
                        .setStatusCode(404)
                        .end();
            }
            else{

                vertx.undeploy( engines.get(idAsInteger).getLogger().deploymentID());     //fermo il logger
                for (Sensor s : engines.get(idAsInteger).getScenario().getSensors()) {        //fermo i sensori
                    vertx.undeploy(s.deploymentID());
                }
                engines.remove(idAsInteger);
                routingContext.response()
                        .setStatusCode(200).
                        end();
            }

        }
    }

    private void getAllEngines(RoutingContext routingContext) {

        JSONArray jsonArrayOfEngines = new JSONArray();

        for (Map.Entry<Integer, Engine> entry : this.engines.entrySet()) {

            JSONObject jsonObjectOfEngine = new JSONObject();

            jsonObjectOfEngine.put("id", entry.getKey());
            jsonObjectOfEngine.put("startDate", entry.getValue().getSimulationStartDate().toString());
            jsonObjectOfEngine.put("endDate", entry.getValue().getSimulationEndDate().toString());
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
                jsonObjectOfEngine.put("startDate", engines.get(idAsInteger).getSimulationStartDate().toString());
                jsonObjectOfEngine.put("endDate", engines.get(idAsInteger).getSimulationEndDate().toString());
                jsonObjectOfEngine.put("progressionPercentage", engines.get(idAsInteger).getProgressOfSimulation());
                jsonObjectOfEngine.put("currentState", engines.get(idAsInteger).getCurrentState());
                jsonObjectOfEngine.put("sceName", engines.get(idAsInteger).getScenario().getScenName());
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
            if (simDuration.equals("00:00:00")) throw new Exception();
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