package it.filippetti.sp.simulator;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import it.filippetti.sp.simulator.model.MeasureType;
import it.filippetti.sp.simulator.model.Scenario;
import it.filippetti.sp.simulator.model.SnapshotModel;
import it.filippetti.sp.simulator.model.TypeOfArray;
import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONString;

import java.util.ArrayList;
import java.util.List;


public class Server extends AbstractVerticle {


    Engine engine=null;
    public Server() {

    }

    @Override
    public void start(Future<Void> fut) throws InterruptedException {

        MqttSender mqttSender=new MqttSender();

        vertx.deployVerticle(mqttSender);



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
                        config().getInteger("http.port", 8080),
                        result -> {
                            if (result.succeeded()) {
                                fut.complete();
                            } else {
                                fut.fail(result.cause());
                            }
                        }
                );

        router.route().handler(BodyHandler.create());
        router.post("/api/engine/start_simulation").handler(this::startSimulation);
        router.get("/api/engine/stop_simulation").handler(this::stopSimulation);
        router.get("/api/engine/produced_snapshots").handler(this::getProducedSnapshots);

    }

    @Override
    public void stop() {

    }


    private void startSimulation(RoutingContext routingContext) {

            try {
                if(engine!=null)
                { vertx.undeploy(engine.deploymentID());
                   }


                SetOfInstancesForSimulation setOfInstancesForSimulation=fromJsonForSimulationToInstancesForSimulation(new JSONObject(routingContext.getBodyAsString()));
                engine=new Engine(setOfInstancesForSimulation.getSimulationStartDate(), setOfInstancesForSimulation.getSimulationEndDate(), setOfInstancesForSimulation.getScenario());

                vertx.deployVerticle(engine);
                routingContext.response()
                        .setStatusCode(201)
                        .putHeader("content-type", "application/json; charset=utf-8")
                        .end(new JSONObject().put("result", "Nice JSON").toString());
            } catch (Exception e) {
                e.printStackTrace();
                routingContext.response()
                        .setStatusCode(404)
                        .putHeader("content-type", "application/json; charset=utf-8")
                        .end(new JSONObject().put("result", "Bad JSON").toString());
            }
        }


    private void stopSimulation(RoutingContext routingContext) {

        try{
            if(engine!=null){vertx.undeploy(engine.deploymentID());}

        routingContext.response()
                .putHeader("content-type", "application/json; charset=utf-8")
                .end(Json.encodePrettily("Simulazione terminata"));
    }catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void getProducedSnapshots(RoutingContext routingContext) {

        if(engine!=null)
        {
            routingContext.response()
                    .putHeader("content-type", "application/json; charset=utf-8")
                    .end(this.engine.getLogger().getProducedSnapshots().toString());
        }
       else{
            routingContext.response()
                    .putHeader("content-type", "application/json; charset=utf-8")
                    .end(Json.encodePrettily("L'engine non è in funzione"));
        }

    }


    private SetOfInstancesForSimulation fromJsonForSimulationToInstancesForSimulation(JSONObject jsonOfSimulation) throws Exception, ErrorDate{

        Scenario instanceOfScenario;
        List<Sensor> sensorsToInsertInScenario = new ArrayList<>();
        DateTime ssd;
        DateTime sed;


        try {

            ssd=DateTime.parse(jsonOfSimulation.get("simulationStartDate").toString());
            sed=DateTime.parse(jsonOfSimulation.get("simulationEndDate").toString());

            if(!sed.isAfter(ssd) && !ssd.isAfterNow() && !sed.isAfterNow()) throw new Exception();

            String scenName = jsonOfSimulation.get("scenName").toString();
            JSONArray jsonArrayOfSensors = jsonOfSimulation.getJSONArray("sensors");
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
                        double prob = Double.parseDouble(jsonObjectOfMeasureTypeOfSnapshotModelOfSensor.get("prob").toString());
                        double variance = Double.parseDouble(jsonObjectOfMeasureTypeOfSnapshotModelOfSensor.get("variance").toString());
                        String selArray = jsonObjectOfMeasureTypeOfSnapshotModelOfSensor.get("selArray").toString();

                        if (source.equals("")) source = null;
                        if (destination.equals("")) destination = null;

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
            return new SetOfInstancesForSimulation(instanceOfScenario, ssd, sed);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private class SetOfInstancesForSimulation
    {
        Scenario s;
        DateTime simulationStartDate;
        DateTime simulationEndDate;
        public SetOfInstancesForSimulation(Scenario s,DateTime simulationStartDate, DateTime simulationEndDate)
        {
            this.s=s;
            this.simulationStartDate=simulationStartDate;
            this.simulationEndDate=simulationEndDate;
        }

        public Scenario getScenario()
        {return this.s;}

        public DateTime getSimulationStartDate()
        {
            return this.simulationStartDate;
        }

        public DateTime getSimulationEndDate()
        {
            return this.simulationEndDate;
        }
    }


}