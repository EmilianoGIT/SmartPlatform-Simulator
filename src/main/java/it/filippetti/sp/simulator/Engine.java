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
    private String currentState = "";     // WaitingToStart, Running, Paused, Stopped, Ended
    private float progressOfSimulation = 0.0f; //in percentuale
    private long timerIDForProgressCalculation;
    private long timerIDForEndOfSimulation;
    private long timerIDForCountdown;
    private long timerIDForSimulationToStart;
    private long passedTime = 0;  //tempo trascorso della simulazione

    public Engine(DateTime simulationStartDate, int periodOfTimeOfSimulation, Scenario scenario) throws Exception {

        if(periodOfTimeOfSimulation<1000) throw new Exception("Il tempo di simulazione non può essere minore di 1 secondo");
        this.id = COUNTER.getAndIncrement();
        this.scenario = scenario;
        this.simulationStartDate = simulationStartDate;
        this.periodOfTimeOfSimulation = periodOfTimeOfSimulation;

    }

    @Override
    public void start() throws Exception {

        for (Sensor s : scenario.getSensors()) {
            s.setLogger(getLogger());
            s.setEngine(this);
        }


        if (simulationStartDate.isBeforeNow()) {
            currentState = "Running";
            simulationStartDate = DateTime.now();
            System.out.println("La simulazione è iniziata in data-ora: " + simulationStartDate.toString());
            simulationEndDate = simulationStartDate.plus(periodOfTimeOfSimulation);
            System.out.println("La simulazione terminerà in data-ora: " + simulationEndDate.toString());
            deploySensorsAndLogger();
            createTimerForEndOfSimulation();
            createTimerForProgressOfSimulation();
            countdownOn();
        } else {
            currentState = "WaitingToStart";
            int periodOfTimeUntilSimulationStarts;
            Seconds secondsToStartSimulation = Seconds.secondsBetween(DateTime.now(), simulationStartDate);
            periodOfTimeUntilSimulationStarts = secondsToStartSimulation.getSeconds() * 1000; //to get millis
            vertx.deployVerticle(logger);
            //simulationStartDate=DateTime.now().plus(periodOfTimeUntilSimulationStarts);
            simulationEndDate = simulationStartDate.plus(periodOfTimeOfSimulation);
            System.out.println("La simulazione inizierà in data-ora: " + simulationStartDate.toString());
            System.out.println("La simulazione terminerà in data-ora: " + simulationEndDate.toString());
            timerIDForSimulationToStart = vertx.setTimer(periodOfTimeUntilSimulationStarts, new Handler<Long>() {
                @Override
                public void handle(Long aLong) {
                    currentState = "Running";
                    simulationStartDate = DateTime.now();
                    System.out.println("La simulazione è iniziata in data-ora: " + simulationStartDate.toString());
                    deploySensorsAndLogger();
                    createTimerForEndOfSimulation();
                    createTimerForProgressOfSimulation();
                    countdownOn();
                }
            });

        }

        vertx.eventBus().consumer("commands" + this.getId(), message -> {

            if (currentState.equals("WaitingToStart")) {
                if (message.body().toString().equals("play")) {
                    currentState = "Running";
                    vertx.cancelTimer(timerIDForSimulationToStart);
                    vertx.cancelTimer(timerIDForEndOfSimulation);
                    vertx.cancelTimer(timerIDForProgressCalculation);
                    vertx.cancelTimer(timerIDForCountdown);
                    simulationStartDate = DateTime.now();
                    createTimerForEndOfSimulation();
                    createTimerForProgressOfSimulation();
                    countdownOn();
                } else if (message.body().toString().equals("stop")) {
                    currentState = "Stopped";
                    vertx.undeploy(deploymentID());
                }
            } else if (currentState.equals("Running")) {
                if (message.body().toString().equals("pause")) {
                    currentState = "Paused";
                    undeploySensorsAndLogger();
                    vertx.cancelTimer(timerIDForEndOfSimulation);
                    vertx.cancelTimer(timerIDForProgressCalculation);
                    vertx.cancelTimer(timerIDForCountdown);
                } else if (message.body().toString().equals("stop")) {
                    currentState = "Stopped";
                    vertx.undeploy(deploymentID());
                }
            } else if (currentState.equals("Paused")) {
                if (message.body().toString().equals("play")) {
                    currentState = "Running";
                    deploySensorsAndLogger();
                    simulationEndDate = DateTime.now().plus(periodOfTimeOfSimulation - (passedTime * 1000));
                    createTimerForEndOfSimulation();
                    createTimerForProgressOfSimulation();
                    countdownOn();
                } else if (message.body().toString().equals("stop")) {
                    currentState = "Stopped";
                    vertx.undeploy(deploymentID());
                }
            }
        });

    }

    @Override
    public void stop() throws Exception {
        undeploySensorsAndLogger();
        simulationEndDate = DateTime.now();
        System.out.println("La simulazione è terminata in data-ora: " + simulationEndDate.toString());
        vertx.cancelTimer(timerIDForSimulationToStart);
        vertx.cancelTimer(timerIDForEndOfSimulation);
        vertx.cancelTimer(timerIDForCountdown);
        vertx.cancelTimer(timerIDForProgressCalculation);
        if (currentState.equals("Ended"))
            progressOfSimulation = 100;

    }

    public Logger getLogger() {
        return this.logger;
    }

    public Scenario getScenario() {
        return this.scenario;
    }

    public void setScenario(Scenario scenario) {
        this.scenario = scenario;
    }

    public int getId() {
        return id;
    }

    public DateTime getSimulationStartDate() {
        return simulationStartDate;
    }

    public DateTime getSimulationEndDate() {
        return simulationEndDate;
    }

    public void calculateProgressOfSimulation() {
        progressOfSimulation = (passedTime * 100) / (float) periodOfTimeOfSimulation * 1000;
    }

    public String getCurrentState() {
        return currentState;
    }

    public int getPeriodOfTimeOfSimulation() {
        return this.periodOfTimeOfSimulation;
    }

    public long getPassedTime() {
        return this.passedTime;
    }

    public int getProgressOfSimulation() {
        return (int) progressOfSimulation;
    }

    public void createTimerForEndOfSimulation() {
        long remainingTime = periodOfTimeOfSimulation - passedTime * 1000;
        timerIDForEndOfSimulation = vertx.setTimer(remainingTime, new Handler<Long>() {
            @Override
            public void handle(Long aLong) {
                currentState = "Ended";
                vertx.undeploy(deploymentID());
            }
        });
    }

    public void countdownOn() {
        timerIDForCountdown = vertx.setPeriodic(1000, new Handler<Long>() {
            @Override
            public void handle(Long aLong) {
                passedTime++;
                if (!(passedTime <= periodOfTimeOfSimulation)) {
                    currentState = "Ended";
                    vertx.undeploy(deploymentID());
                }
            }
        });
    }

    public void createTimerForProgressOfSimulation() {
        timerIDForProgressCalculation = vertx.setPeriodic(1000, new Handler<Long>() {
            @Override
            public void handle(Long aLong) {
                calculateProgressOfSimulation();
            }
        });
    }


    public void undeploySensorsAndLogger() {
        for (Sensor s : scenario.getSensors()) {        //fermo i sensori
            vertx.undeploy(s.deploymentID());
        }
        vertx.undeploy(getLogger().deploymentID());     //fermo il logger
    }

    public void deploySensorsAndLogger() {
        vertx.deployVerticle(logger);
        for (Sensor s : scenario.getSensors()) {
            vertx.deployVerticle(s);
        }
    }
}



