package it.filippetti.sp.simulator;

import io.vertx.core.Vertx;
import it.filippetti.sp.simulator.model.MeasureType;
import it.filippetti.sp.simulator.model.Scenario;
import it.filippetti.sp.simulator.model.Model;
import it.filippetti.sp.simulator.model.TypeOfArray;
import org.joda.time.DateTime;
import org.junit.Test;

import static org.junit.Assert.*;

public class EngineTest {

    @Test(expected = Exception.class)
    public void engineIsNotCreatedIfPeriodOfSimulationTimeIsLowerThan1Second() throws Exception {

        Vertx vertx = Vertx.vertx();
        DateTime simulationStartDateAsParameter = DateTime.parse("2000-10-04T20:02:00"); //data vecchia
        MeasureType measureType1 = new MeasureType("measure1", "key1", "unity1", 20.0, 30.0, "s1", "d1", 3.0, 4.0, TypeOfArray.valueOf("M"));
        MeasureType measureType2 = new MeasureType("measure2", "key2", "unity2", 20.0, 30.0, "s1", "d1", 3.0, 4.0, TypeOfArray.valueOf("M"));
        Model model1 = new Model("model1Test", "0222", 0.6f);
        Model model2 = new Model("model2Test", "0222", 0.4f);
        model1.addMeasureType(measureType1);
        model1.addMeasureType(measureType2);
        model2.addMeasureType(measureType1);
        Sensor sensor = new Sensor("sensoreTest", "refTest", "tipoTest", 1000, "topicTest");
        sensor.addModel(model1);
        sensor.addModel(model2);
        Scenario scenario = new Scenario("scenarioTest", sensor);
        int periodOfSimulationTime = 500; //500 millisecondi
        Engine engine = new Engine(simulationStartDateAsParameter, periodOfSimulationTime, scenario); //data vecchia
        vertx.deployVerticle(engine);
        Thread.sleep(1000);

    }

    @Test
    public void engineModifiesOldSimulationStartDateWithCurrentDate() throws Exception {
        Vertx vertx = Vertx.vertx();
        DateTime simulationStartDateAsParameter = DateTime.parse("2000-10-04T20:02:00"); //data vecchia
        MeasureType measureType1 = new MeasureType("measure1", "key1", "unity1", 20.0, 30.0, "s1", "d1", 3.0, 4.0, TypeOfArray.valueOf("M"));
        MeasureType measureType2 = new MeasureType("measure2", "key2", "unity2", 20.0, 30.0, "s1", "d1", 3.0, 4.0, TypeOfArray.valueOf("M"));
        Model model1 = new Model("model1Test", "0222", 0.6f);
        Model model2 = new Model("model2Test", "0222", 0.4f);
        model1.addMeasureType(measureType1);
        model1.addMeasureType(measureType2);
        model2.addMeasureType(measureType1);
        Sensor sensor = new Sensor("sensoreTest", "refTest", "tipoTest", 1000, "topicTest");
        sensor.addModel(model1);
        sensor.addModel(model2);
        Scenario scenario = new Scenario("scenarioTest", sensor);
        int periodOfSimulationTime = 60000; //1 minuto
        Engine engine = new Engine(simulationStartDateAsParameter, periodOfSimulationTime, scenario); //data vecchia
        vertx.deployVerticle(engine);
        Thread.sleep(1000);
        assertEquals(engine.getSimulationStartDate().plus(1000).withMillisOfSecond(000), DateTime.now().withMillisOfSecond(000));
    }

    @Test
    public void simulationEndDateOfEngineisEqualToTheSumOfSimulationStartDateAndPeriodOfSimulationTime() throws Exception {
        Vertx vertx = Vertx.vertx();
        DateTime simulationStartDateAsParameter = DateTime.parse("2000-10-04T20:02:00"); //data vecchia
        MeasureType measureType1 = new MeasureType("measure1", "key1", "unity1", 20.0, 30.0, "s1", "d1", 3.0, 4.0, TypeOfArray.valueOf("M"));
        MeasureType measureType2 = new MeasureType("measure2", "key2", "unity2", 20.0, 30.0, "s1", "d1", 3.0, 4.0, TypeOfArray.valueOf("M"));
        Model model1 = new Model("model1Test", "0222", 0.6f);
        Model model2 = new Model("model2Test", "0222", 0.4f);
        model1.addMeasureType(measureType1);
        model1.addMeasureType(measureType2);
        model2.addMeasureType(measureType1);
        Sensor sensor = new Sensor("sensoreTest", "refTest", "tipoTest", 1000, "topicTest");
        sensor.addModel(model1);
        sensor.addModel(model2);
        Scenario scenario = new Scenario("scenarioTest", sensor);
        int periodOfSimulationTime = 60000; //1 minuto
        Engine engine = new Engine(simulationStartDateAsParameter, periodOfSimulationTime, scenario); //data vecchia
        vertx.deployVerticle(engine);
        Thread.sleep(1000);
        assertEquals(engine.getSimulationStartDate().plus(periodOfSimulationTime), engine.getSimulationEndDate());
    }

    @Test
    public void engineIsInWaitingToStartStatusIfSimulationStartDateIsFuture() throws Exception {
        Vertx vertx = Vertx.vertx();
        MeasureType measureType1 = new MeasureType("measure1", "key1", "unity1", 20.0, 30.0, "s1", "d1", 3.0, 4.0, TypeOfArray.valueOf("M"));
        MeasureType measureType2 = new MeasureType("measure2", "key2", "unity2", 20.0, 30.0, "s1", "d1", 3.0, 4.0, TypeOfArray.valueOf("M"));
        Model model1 = new Model("model1Test", "0222", 0.6f);
        Model model2 = new Model("model2Test", "0222", 0.4f);
        model1.addMeasureType(measureType1);
        model1.addMeasureType(measureType2);
        model2.addMeasureType(measureType1);
        Sensor sensor = new Sensor("sensoreTest", "refTest", "tipoTest", 1000, "topicTest");
        sensor.addModel(model1);
        sensor.addModel(model2);
        Scenario scenario = new Scenario("scenarioTest", sensor);
        int periodOfSimulationTime = 60000; //1 minuto
        Engine engine = new Engine(DateTime.now().plusMillis(3600000), periodOfSimulationTime, scenario); //inizia tra 1h
        vertx.deployVerticle(engine);
        Thread.sleep(1000);
        assertEquals(engine.getCurrentState(), "WaitingToStart");
    }

    @Test
    public void engineGoesInStopStatusIfItIsInWaitingToStartStatusAndAStopInputArrives() throws Exception {
        Vertx vertx = Vertx.vertx();
        MeasureType measureType1 = new MeasureType("measure1", "key1", "unity1", 20.0, 30.0, "s1", "d1", 3.0, 4.0, TypeOfArray.valueOf("M"));
        MeasureType measureType2 = new MeasureType("measure2", "key2", "unity2", 20.0, 30.0, "s1", "d1", 3.0, 4.0, TypeOfArray.valueOf("M"));
        Model model1 = new Model("model1Test", "0222", 0.6f);
        Model model2 = new Model("model2Test", "0222", 0.4f);
        model1.addMeasureType(measureType1);
        model1.addMeasureType(measureType2);
        model2.addMeasureType(measureType1);
        Sensor sensor = new Sensor("sensoreTest", "refTest", "tipoTest", 1000, "topicTest");
        sensor.addModel(model1);
        sensor.addModel(model2);
        Scenario scenario = new Scenario("scenarioTest", sensor);
        int periodOfSimulationTime = 60000; //1 minuto
        Engine engine = new Engine(DateTime.now().plusMillis(3600000), periodOfSimulationTime, scenario); //inizia tra 1h
        vertx.deployVerticle(engine);
        Thread.sleep(1000);
        vertx.eventBus().send("commands" + engine.getId(), "stop");
        Thread.sleep(1000);
        assertEquals(engine.getCurrentState(), "Stopped");
    }

    @Test
    public void engineGoesInStopStatusIfItIsInRunningStatusAndAStopInputArrives() throws Exception {
        Vertx vertx = Vertx.vertx();
        DateTime simulationStartDateAsParameter = DateTime.parse("2000-10-04T20:02:00"); //data vecchia
        MeasureType measureType1 = new MeasureType("measure1", "key1", "unity1", 20.0, 30.0, "s1", "d1", 3.0, 4.0, TypeOfArray.valueOf("M"));
        MeasureType measureType2 = new MeasureType("measure2", "key2", "unity2", 20.0, 30.0, "s1", "d1", 3.0, 4.0, TypeOfArray.valueOf("M"));
        Model model1 = new Model("model1Test", "0222", 0.6f);
        Model model2 = new Model("model2Test", "0222", 0.4f);
        model1.addMeasureType(measureType1);
        model1.addMeasureType(measureType2);
        model2.addMeasureType(measureType1);
        Sensor sensor = new Sensor("sensoreTest", "refTest", "tipoTest", 1000, "topicTest");
        sensor.addModel(model1);
        sensor.addModel(model2);
        Scenario scenario = new Scenario("scenarioTest", sensor);
        int periodOfSimulationTime = 60000; //1 minuto
        Engine engine = new Engine(simulationStartDateAsParameter, periodOfSimulationTime, scenario); //data vecchia
        vertx.deployVerticle(engine);
        Thread.sleep(1000);
        vertx.eventBus().send("commands" + engine.getId(), "stop");
        Thread.sleep(1000);
        assertEquals(engine.getCurrentState(), "Stopped");
    }

    @Test
    public void engineGoesInPauseStatusIfItIsInRunningStatusAndAPauseInputArrives() throws Exception {
        Vertx vertx = Vertx.vertx();
        DateTime simulationStartDateAsParameter = DateTime.parse("2000-10-04T20:02:00"); //data vecchia
        MeasureType measureType1 = new MeasureType("measure1", "key1", "unity1", 20.0, 30.0, "s1", "d1", 3.0, 4.0, TypeOfArray.valueOf("M"));
        MeasureType measureType2 = new MeasureType("measure2", "key2", "unity2", 20.0, 30.0, "s1", "d1", 3.0, 4.0, TypeOfArray.valueOf("M"));
        Model model1 = new Model("model1Test", "0222", 0.6f);
        Model model2 = new Model("model2Test", "0222", 0.4f);
        model1.addMeasureType(measureType1);
        model1.addMeasureType(measureType2);
        model2.addMeasureType(measureType1);
        Sensor sensor = new Sensor("sensoreTest", "refTest", "tipoTest", 1000, "topicTest");
        sensor.addModel(model1);
        sensor.addModel(model2);
        Scenario scenario = new Scenario("scenarioTest", sensor);
        int periodOfSimulationTime = 60000; //1 minuto
        Engine engine = new Engine(simulationStartDateAsParameter, periodOfSimulationTime, scenario); //data vecchia
        vertx.deployVerticle(engine);
        Thread.sleep(1000);
        vertx.eventBus().send("commands" + engine.getId(), "pause");
        Thread.sleep(1000);
        assertEquals(engine.getCurrentState(), "Paused");
    }


    @Test
    public void engineGoesInEndedStatusIfItIsInRunningStatusAndSimulationTimeEnds() throws Exception {
        Vertx vertx = Vertx.vertx();
        DateTime simulationStartDateAsParameter = DateTime.parse("2000-10-04T20:02:00"); //data vecchia
        MeasureType measureType1 = new MeasureType("measure1", "key1", "unity1", 20.0, 30.0, "s1", "d1", 3.0, 4.0, TypeOfArray.valueOf("M"));
        MeasureType measureType2 = new MeasureType("measure2", "key2", "unity2", 20.0, 30.0, "s1", "d1", 3.0, 4.0, TypeOfArray.valueOf("M"));
        Model model1 = new Model("model1Test", "0222", 0.6f);
        Model model2 = new Model("model2Test", "0222", 0.4f);
        model1.addMeasureType(measureType1);
        model1.addMeasureType(measureType2);
        model2.addMeasureType(measureType1);
        Sensor sensor = new Sensor("sensoreTest", "refTest", "tipoTest", 1000, "topicTest");
        sensor.addModel(model1);
        sensor.addModel(model2);
        Scenario scenario = new Scenario("scenarioTest", sensor);
        int periodOfSimulationTime = 3000; //3 secondi
        Engine engine = new Engine(simulationStartDateAsParameter, periodOfSimulationTime, scenario);
        vertx.deployVerticle(engine);
        Thread.sleep(4000);
        assertEquals(engine.getCurrentState(), "Ended");
    }


}