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
    int periodOfTimeOfSimulation;

    public Engine(DateTime simulationStartDate, int periodOfTimeOfSimulation, Scenario scenario) {

        this.scenario=scenario;
        this.simulationStartDate=simulationStartDate;
        this.periodOfTimeOfSimulation=periodOfTimeOfSimulation;

    }

    @Override
    public void start() throws Exception {
        Integer periodOfTimeUntilSimulationStarts;
if(simulationStartDate.isBeforeNow())
            periodOfTimeUntilSimulationStarts=1;
else   {
    Seconds secondsToStartSimulation = Seconds.secondsBetween(DateTime.now(),simulationStartDate);
    periodOfTimeUntilSimulationStarts=secondsToStartSimulation.getSeconds()*1000; //to get millis
}

            //Seconds secondsOfSimulationPeriod = Seconds.secondsBetween(DateTime.now(), simulationEndDate);

           // Integer periodOfTimeOfSimulation=secondsOfSimulationPeriod.getSeconds()*60000; //to get millis from minute
            logger.clearProducedSnapshots();
            vertx.deployVerticle(logger);





        vertx.setTimer(periodOfTimeUntilSimulationStarts, new Handler<Long>() {
            @Override
            public void handle(Long aLong) {
                System.out.println("La simulazione è iniziata in data-ora: " + DateTime.now().toString());
                for (Sensor s : scenario.getSensors()) {
                    vertx.deployVerticle(s);
                }
            }
        });

            vertx.setTimer(periodOfTimeOfSimulation*60000, new Handler<Long>() {
                @Override
                public void handle(Long aLong) {
                    System.out.println("La simulazione è terminata in data-ora: " + DateTime.now().toString());
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






}