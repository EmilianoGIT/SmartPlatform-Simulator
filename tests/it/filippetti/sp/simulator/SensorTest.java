package it.filippetti.sp.simulator;

import org.junit.Test;

import static org.junit.Assert.*;

public class SensorTest {

    @Test(expected = Exception.class)
    public void sensorIsNotCreatedIfPollingTimeIsLowerThan1Second() throws Exception {
            Sensor sensor = new Sensor("refTest","refTest", "typeTest", 10, "topicTest");
    }

}