package it.filippetti.sp.simulator;

import io.vertx.core.Handler;
import io.vertx.core.AbstractVerticle;
import it.filippetti.sp.simulator.model.Scenario;
import org.joda.time.DateTime;
import org.joda.time.Seconds;

public class Engine extends AbstractVerticle {

    private final String id;
    private Scenario scenario;
    private Logger logger = new Logger();
    private DateTime simulationStartDate;        //data di inizio simulazione
    private DateTime simulationEndDate;          //data di fine simulazione
    private int periodOfTimeOfSimulation;         //durata della simulazione espressa in secondi
    private String currentState = "";                // WaitingToStart, Running, Paused, Stopped, Ended
    private float progressOfSimulation = 0.0f;          //in percentuale
    private long timerIDForProgressCalculation;
    private long timerIDForEndOfSimulation;
    private long timerIDForCountdown;
    private long timerIDForSimulationToStart;
    private long passedTime = 0;  //tempo trascorso della simulazione

    public Engine(DateTime simulationStartDate, int periodOfTimeOfSimulation, Scenario scenario) throws Exception {

        if (periodOfTimeOfSimulation < 1000)
            throw new Exception("Simulation time can't be less than 1 second...");
        this.id = randomAlphaNumeric(32, "abcdef0123456789");       //assegno come id un esadecimale random da 32 caratteri
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
        vertx.deployVerticle(this.logger);


        if (simulationStartDate.isBeforeNow()) {
            currentState = "Running";
            simulationStartDate = DateTime.now();
            System.out.println("Simulation with id '" + getId() + "' started at : " + simulationStartDate.toString());
            simulationEndDate = simulationStartDate.plus(periodOfTimeOfSimulation);
            System.out.println("Simulation with id '" + getId() + "' will end at : " + simulationEndDate.toString());
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
            simulationEndDate = simulationStartDate.plus(periodOfTimeOfSimulation);
            System.out.println("Simulation with id '" + getId() + "' will start at: " + simulationStartDate.toString());
            System.out.println("Simulation with id '" + getId() + "' will finish at: " + simulationEndDate.toString());
            if (periodOfTimeUntilSimulationStarts < 1)
                periodOfTimeUntilSimulationStarts = 1;
            timerIDForSimulationToStart = vertx.setTimer(periodOfTimeUntilSimulationStarts, new Handler<Long>() {
                @Override
                public void handle(Long aLong) {
                    currentState = "Running";
                    simulationStartDate = DateTime.now();
                    System.out.println("Simulation with id '" + getId() + "' started at: " + simulationStartDate.toString());
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
        System.out.println("Simulation with id '" + this.getId() + "' ended");
        vertx.cancelTimer(timerIDForSimulationToStart);
        vertx.cancelTimer(timerIDForEndOfSimulation);
        vertx.cancelTimer(timerIDForCountdown);
        vertx.cancelTimer(timerIDForProgressCalculation);
        if (currentState.equals("Ended"))
            progressOfSimulation = 100;

    }


    public String randomAlphaNumeric(int length, String availableChars) {       //metodo per generare stringhe randomiche
        StringBuilder builder = new StringBuilder();
        while (length-- != 0) {
            int character = (int) (Math.random() * availableChars.length());
            builder.append(availableChars.charAt(character));
        }
        return builder.toString();
    }

    public Logger getLogger() {
        return this.logger;
    }

    public Scenario getScenario() {
        return this.scenario;
    }

    public String getId() {
        return this.id;
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

    public void createTimerForEndOfSimulation() {       //timer che allo scadere del tempo termina la simulazione
        long remainingTime = periodOfTimeOfSimulation - passedTime * 1000;
        timerIDForEndOfSimulation = vertx.setTimer(remainingTime, new Handler<Long>() {
            @Override
            public void handle(Long aLong) {
                currentState = "Ended";
                vertx.undeploy(deploymentID());
            }
        });
    }


    public void countdownOn() {         //time per tenere il conto del tempo passato
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

    public void createTimerForProgressOfSimulation() {              //timer per il calcolo della percentuale di completamento
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



