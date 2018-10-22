package it.filippetti.sp.simulator.model;

import it.filippetti.sp.simulator.Sensor;
import org.junit.Test;

import static org.junit.Assert.*;

public class ScenarioTest {

    @Test(expected = Exception.class)
    public void checkIfScenarioDoesnTAddSensorsWithSameRef() throws Exception {

            MeasureType measureType1 = new MeasureType("measure1", "key1", "unity1", 20.0, 30.0, "s1", "d1", 3.0, 4.0, TypeOfArray.valueOf("M"));
            MeasureType measureType2 = new MeasureType("measure2", "key2", "unity2", 20.0, 30.0, "s1", "d1", 3.0, 4.0, TypeOfArray.valueOf("M"));
            SnapshotModel model1 = new SnapshotModel("model1Test", "0222", 0.6f);
            SnapshotModel model2 = new SnapshotModel("model2Test", "0222", 0.4f);
            model1.addMeasureType(measureType1);
            model1.addMeasureType(measureType2);
            model2.addMeasureType(measureType1);
            Sensor sensor1 = new Sensor("sensoreTest", "refTest", "tipoTest", 1000, "topicTest");
            sensor1.addModel(model1);
            sensor1.addModel(model2);

            Sensor sensor2 = new Sensor("sensoreTest2", "refTest", "tipoTest", 1000, "topicTest");
            sensor2.addModel(model1);
            sensor2.addModel(model2);

            Scenario scenario = new Scenario("scenarioTest", sensor1);

            scenario.addSensor(sensor2);
    }

    @Test(expected = Exception.class)
    public void checkIfScenarioDoesnTAddSensorsWithMessedUpProbabilities() throws Exception {

            MeasureType measureType1 = new MeasureType("measure1", "key1", "unity1", 20.0, 30.0, "s1", "d1", 3.0, 4.0, TypeOfArray.valueOf("M"));
            MeasureType measureType2 = new MeasureType("measure2", "key2", "unity2", 20.0, 30.0, "s1", "d1", 3.0, 4.0, TypeOfArray.valueOf("M"));
            SnapshotModel model1 = new SnapshotModel("model1Test", "0222", 0.6f);
            SnapshotModel model2 = new SnapshotModel("model2Test", "0222", 0.9f);
            model1.addMeasureType(measureType1);
            model1.addMeasureType(measureType2);
            model2.addMeasureType(measureType1);
            Sensor sensor1 = new Sensor("sensoreTest", "refTest", "tipoTest", 1000, "topicTest");
            sensor1.addModel(model1);
            sensor1.addModel(model2);

            Scenario scenario = new Scenario("scenarioTest", sensor1);

    }

}