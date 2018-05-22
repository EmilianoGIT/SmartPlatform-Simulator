package it.filippetti.sp.simulator;
import io.vertx.core.Handler;
import it.filippetti.sp.simulator.model.MeasureType;
import it.filippetti.sp.simulator.model.Scenario;
import it.filippetti.sp.simulator.model.SnapshotModel;
import it.filippetti.sp.simulator.model.TypeOfArray;
import io.vertx.core.AbstractVerticle;
import org.joda.time.DateTime;
import org.joda.time.Seconds;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.*;


public class Engine extends AbstractVerticle {

    public Scenario scenario;
    Logger logger=new Logger(this);
    DateTime simulationStartDate;
    DateTime simulationEndDate;

    public Engine(DateTime simulationStartDate, DateTime simulationEndDate, Scenario scenario) {

        this.scenario=scenario;
        this.simulationStartDate=simulationStartDate;
        this.simulationEndDate=simulationEndDate;

    }

    @Override
    public void start() throws Exception {


            Seconds secondsToStartSimulation = Seconds.secondsBetween(DateTime.now(),simulationStartDate);
            Seconds secondsOfSimulationPeriod = Seconds.secondsBetween(DateTime.now(), simulationEndDate);
            Integer periodOfTimeUntilSimulationStarts=secondsToStartSimulation.getSeconds()*1000; //to get millis
            Integer periodOfTimeOfSimulation=secondsOfSimulationPeriod.getSeconds()*1000; //to get millis
            logger.clearProducedSnapshots();
            vertx.deployVerticle(logger);





        vertx.setTimer(periodOfTimeUntilSimulationStarts, new Handler<Long>() {
            @Override
            public void handle(Long aLong) {
                System.out.println("La simulazione è iniziata in data-ora: " + simulationStartDate.toString());
                for (Sensor s : scenario.getSensors()) {
                    vertx.deployVerticle(s);
                }
            }
        });
        vertx.setTimer(periodOfTimeOfSimulation, new Handler<Long>() {
            @Override
            public void handle(Long aLong) {
                System.out.println("La simulazione è terminata in data-ora: " + simulationEndDate.toString());
                for (Sensor s : scenario.getSensors()) {
                    vertx.undeploy(s.deploymentID());
                }
                vertx.undeploy(deploymentID());
            }
        });

    }

    @Override
    public void stop() throws Exception {

    }


    public Logger getLogger() {
        return this.logger;
    }

    public void setScenario(Scenario scenario)
    {
        this.scenario=scenario;
    }

    public void setSimulationStartDate(DateTime simulationStartDate)
    {
        this.simulationStartDate=simulationStartDate;
    }

    public void setSimulationEndDate(DateTime simulationEndDate){
        this.simulationEndDate=simulationEndDate;
    }





}