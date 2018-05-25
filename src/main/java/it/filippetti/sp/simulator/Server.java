package it.filippetti.sp.simulator;

import com.mongodb.*;
import com.mongodb.util.JSON;
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
import org.bson.types.ObjectId;
import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Server extends AbstractVerticle {


    Engine engine=null;
    MqttSender mqttSender=null;
    MongoClient mongoClient=null;
    DB db=null;

    public Server() {

    }

    @Override
    public void start(Future<Void> fut) throws InterruptedException {
/*
        mqttSender=new MqttSender("localhost", 1883);
        vertx.deployVerticle(mqttSender);
*/

/*
        mongoClient=new com.mongodb.MongoClient("localhost",27017 );
        db = mongoClient.getDB("simulator");
        */



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
        router.get("/api/repository/simulator/scenarios/:id").handler(this::getScenario);
       // router.post("/api/repository/simulator/scenarios").handler(this::insertScenario);

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
                engine=new Engine(setOfInstancesForSimulation.getSimulationStartDate(), setOfInstancesForSimulation.getPeriodOfTimeOfSimulation(), setOfInstancesForSimulation.getScenario());

                vertx.deployVerticle(engine);
                routingContext.response()
                        .setStatusCode(201)
                        .putHeader("content-type", "application/json; charset=utf-8")
                        .end(new JSONObject().put("result", "Nice JSON").toString());
                System.out.println(routingContext.getBody().toString());
            } catch (Exception e) {
                e.printStackTrace();
                routingContext.response()
                        .setStatusCode(404)
                        .putHeader("content-type", "application/json; charset=utf-8")
                        .end(new JSONObject().put("result", "Bad JSON").toString());
                System.out.println(routingContext.getBody().toString());
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
    private void getScenario(RoutingContext routingContext) {
        String id = routingContext.request().getParam("id");
        if (id == null) {
            routingContext.response().setStatusCode(400).end();
        } else {

            DBCollection collection=db.getCollection("scenarios");

            BasicDBObject query = new BasicDBObject();
            query.put("_id", new ObjectId(id));
            DBObject dbObject=collection.findOne(query);
            dbObject.removeField("_id");
            routingContext.response()
                    .putHeader("content-type", "application/json; charset=utf-8")
                    .setStatusCode(200).
                    end(dbObject.toString());
        }
        /*
        JSONObject jo=new JSONObject();
        jo.put("ciao", 2);

        DBObject dbObject = (DBObject) JSON
                .parse(jo.toString());

        DBCollection collection=db.getCollection("scenarios");
        collection.insert(dbObject);

        DBCursor dbCursor=collection.find().limit(3);
        for(DBObject dbO: dbCursor)
        {System.out.println(dbO.toString());}
*/

    }





    private SetOfInstancesForSimulation fromJsonForSimulationToInstancesForSimulation(JSONObject jsonOfSimulation) throws Exception, ErrorDate{

        Scenario instanceOfScenario;
        List<Sensor> sensorsToInsertInScenario = new ArrayList<>();
        DateTime ssd;
        DateTime sed;
        int periodOfTimeOfSimulation; //in minutes





        try {
          if(jsonOfSimulation.get("simStartDate").toString().equals(""))
              ssd=DateTime.now();
          else ssd=DateTime.parse(jsonOfSimulation.get("simStartDate").toString());

            String regexTimer = "[0-9][0-9]:[0-5][0-9]:[0-5][0-9]";
            Pattern pattern = Pattern.compile(regexTimer);
            String simDuration=jsonOfSimulation.get("simDuration").toString();
            Matcher matcher = pattern.matcher(simDuration);

            if(matcher.find())
            {
                int numberOfHoursInInt=(Character.getNumericValue(simDuration.charAt(0))*10)+(Character.getNumericValue(simDuration.charAt(1)));
                int numberOfMinutesInInt=(Character.getNumericValue(simDuration.charAt(3))*10)+(Character.getNumericValue(simDuration.charAt(4)));
                int numberOfSecondsInInt=(Character.getNumericValue(simDuration.charAt(6))*10)+(Character.getNumericValue(simDuration.charAt(7)));

                int hoursInMillis=numberOfHoursInInt*3600000;
                int minutesInMillis=numberOfMinutesInInt*60000;
                int secondsInMillis=numberOfSecondsInInt*1000;


                periodOfTimeOfSimulation=hoursInMillis+minutesInMillis+secondsInMillis;
            }
            else throw new Exception();




          //periodOfTimeOfSimulation=Integer.parseInt(jsonOfSimulation.get("simDuration").toString());

            //sed=DateTime.parse(jsonOfSimulation.get("simulationEndDate").toString());

            //if(!sed.isAfter(ssd) && !ssd.isAfterNow() && !sed.isAfterNow()) throw new Exception();

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
            return new SetOfInstancesForSimulation(instanceOfScenario, ssd, periodOfTimeOfSimulation);
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
        int periodOfTimeOfSimulation;
        public SetOfInstancesForSimulation(Scenario s,DateTime simulationStartDate,int periodOfTimeOfSimulation)
        {
            this.s=s;
            this.simulationStartDate=simulationStartDate;

            this.periodOfTimeOfSimulation=periodOfTimeOfSimulation;
        }

        public Scenario getScenario()
        {return this.s;}

        public DateTime getSimulationStartDate()
        {
            return this.simulationStartDate;
        }

        public int getPeriodOfTimeOfSimulation()
        {
            return this.periodOfTimeOfSimulation;
        }
    }


}