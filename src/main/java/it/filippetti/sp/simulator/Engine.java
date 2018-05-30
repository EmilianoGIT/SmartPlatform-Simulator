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
import java.util.concurrent.atomic.AtomicInteger;


public class Engine extends AbstractVerticle {

    private static final AtomicInteger COUNTER = new AtomicInteger();
    private final int id;
    public Scenario scenario;
    Logger logger = new Logger();
    DateTime simulationStartDate;
    DateTime simulationEndDate;
    int periodOfTimeOfSimulation;

    public Engine(DateTime simulationStartDate, int periodOfTimeOfSimulation, Scenario scenario) {

        this.id = COUNTER.getAndIncrement();
        this.scenario = scenario;
        this.simulationStartDate = simulationStartDate;
        this.periodOfTimeOfSimulation = periodOfTimeOfSimulation;

    }

    @Override
    public void start() throws Exception {
        Integer periodOfTimeUntilSimulationStarts;
        if (simulationStartDate.isBeforeNow())
        {
            simulationStartDate=DateTime.now();
            periodOfTimeUntilSimulationStarts = 1;
        }
        else {
            Seconds secondsToStartSimulation = Seconds.secondsBetween(DateTime.now(), simulationStartDate);
            periodOfTimeUntilSimulationStarts = secondsToStartSimulation.getSeconds() * 1000; //to get millis
        }
        simulationEndDate=simulationStartDate.plus(periodOfTimeOfSimulation);
        vertx.deployVerticle(logger);


        vertx.setTimer(periodOfTimeUntilSimulationStarts, new Handler<Long>() {
            @Override
            public void handle(Long aLong) {
                simulationStartDate=DateTime.now();
                System.out.println("La simulazione è iniziata in data-ora: " + simulationStartDate.toString());
                for (Sensor s : scenario.getSensors()) {
                    s.setLogger(getLogger());
                    vertx.deployVerticle(s);
                }

                vertx.setTimer(periodOfTimeOfSimulation, new Handler<Long>() {
                    @Override
                    public void handle(Long aLong) {

                        for (Sensor s : scenario.getSensors()) {
                            vertx.undeploy(s.deploymentID());
                        }
                        vertx.undeploy(getLogger().deploymentID());
                        System.out.println("La simulazione è terminata in data-ora: " + DateTime.now().toString());
                        vertx.undeploy(deploymentID());
                    }
                });
            }
        });


    }

    @Override
    public void stop() throws Exception {
simulationEndDate=DateTime.now();
    }


    public Logger getLogger() {
        return this.logger;
    }

    public void setScenario(Scenario scenario) {
        this.scenario = scenario;
    }


    public int getId() {
        return id;
    }

    public DateTime getSimulationStartDate()
    {
        return simulationStartDate;
    }

    public DateTime getSimulationEndDate()
    {
        return simulationEndDate;
    }

    public double getProgressionPercentage()
    {
        double progressionPercentage=0;
        if(simulationStartDate.isAfterNow()) progressionPercentage=0;
        else if(simulationEndDate.isBeforeNow()){
            progressionPercentage=100;
        }
        else {
            Seconds secondsPassedFromStart = Seconds.secondsBetween(simulationStartDate,DateTime.now());
            Integer millisecondsPassedFromStart = secondsPassedFromStart.getSeconds() * 1000; //to get millis
            progressionPercentage=(millisecondsPassedFromStart*100)/periodOfTimeOfSimulation;
        }

        return progressionPercentage;
    }


}