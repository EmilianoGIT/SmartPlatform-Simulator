package it.filippetti.sp.simulator;

import io.vertx.core.Handler;
import io.vertx.core.AbstractVerticle;
import it.filippetti.sp.simulator.model.Scenario;
import org.joda.time.DateTime;
import org.joda.time.Seconds;

import java.util.concurrent.atomic.AtomicInteger;


public class Engine extends AbstractVerticle {

    private static final AtomicInteger COUNTER = new AtomicInteger();
    private final int id;
    private Scenario scenario;
    private Logger logger = new Logger();
    private DateTime simulationStartDate;        //data di inizio simulazione
    private DateTime simulationEndDate;          //data di fine simulazione
    private int periodOfTimeOfSimulation;         //durata della simulazione espressa in secondi
    private String currentState="WaitingToStart";     //gli altri stati sono : Running, Paused, Stopped
    private double progressOfSimulation=0; //in percentuale

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
                currentState="Running";

                vertx.setPeriodic(1000, new Handler<Long>() {
                    @Override
                    public void handle(Long aLong) {
                        calculateProgressOfSimulation();
                    }
                });

                System.out.println("La simulazione è iniziata in data-ora: " + simulationStartDate.toString());
                for (Sensor s : scenario.getSensors()) {
                    s.setLogger(getLogger());
                    vertx.deployVerticle(s);
                }

                vertx.setTimer(periodOfTimeOfSimulation, new Handler<Long>() {
                    @Override
                    public void handle(Long aLong) {
                        vertx.undeploy(deploymentID());
                    }
                });
            }
        });

        vertx.eventBus().consumer("commands"+this.getId(), message -> {

            if(currentState.equals("WaitingToStart"))
            {
                if(message.body().toString().equals("play"))
                {
                    simulationStartDate=DateTime.now();
                    currentState="Running";
                    for (Sensor s : scenario.getSensors()) {
                        s.setLogger(getLogger());
                        vertx.deployVerticle(s);
                    }
                    vertx.setPeriodic(1000, new Handler<Long>() {
                        @Override
                        public void handle(Long aLong) {
                            calculateProgressOfSimulation();
                        }
                    });
                    vertx.setTimer(periodOfTimeOfSimulation, new Handler<Long>() {
                        @Override
                        public void handle(Long aLong) {
                            vertx.undeploy(deploymentID());
                        }
                    });
                }
                else if(message.body().toString().equals("stop"))
                {
                   vertx.undeploy(deploymentID());
                }
            }
            else if(currentState.equals("Running"))
            {
                if(message.body().toString().equals("pause"))
                {
                    currentState="Paused";
                    for (Sensor s : scenario.getSensors()) {
                        vertx.undeploy(s.deploymentID());
                    }
                }
                else if(message.body().toString().equals("stop"))
                {
                    vertx.undeploy(deploymentID());
                }

            }
            else if(currentState.equals("Paused"))
            {
                if(message.body().toString().equals("play"))
                {
                    currentState="Running";
                    for (Sensor s : scenario.getSensors()) {
                        vertx.deployVerticle(s);
                    }
                }
                else if(message.body().toString().equals("stop"))
                {
                    vertx.undeploy(deploymentID());
                }
            }
        });

        }

    @Override
    public void stop() throws Exception {
        currentState="Stopped";
        System.out.println("La simulazione è terminata in data-ora: " + DateTime.now().toString());
        progressOfSimulation=100;
        vertx.undeploy(getLogger().deploymentID());     //fermo il logger
        for (Sensor s : scenario.getSensors()) {        //fermo i sensori
            vertx.undeploy(s.deploymentID());
        }
    }
    public void setScenario(Scenario scenario) {
        this.scenario = scenario;
    }

    public Logger getLogger() {
        return this.logger;
    }

    public Scenario getScenario(){
        return this.scenario;
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

    public double calculateProgressOfSimulation()
    {
            Seconds secondsPassedFromStart = Seconds.secondsBetween(simulationStartDate,DateTime.now());
            Integer millisecondsPassedFromStart = secondsPassedFromStart.getSeconds() * 1000; //to get millis
            progressOfSimulation=(millisecondsPassedFromStart*100)/periodOfTimeOfSimulation;

            return progressOfSimulation;
    }

    public String getCurrentState()
    {
        return currentState;
    }

    public double getProgressOfSimulation() {
        return progressOfSimulation;
    }
}