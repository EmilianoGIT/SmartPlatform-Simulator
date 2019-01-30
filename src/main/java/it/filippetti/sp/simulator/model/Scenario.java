package it.filippetti.sp.simulator.model;

import it.filippetti.sp.simulator.Sensor;

import java.util.Set;
import java.util.HashSet;

public class Scenario {

    String sceName;
    Set<Sensor> sensors = new HashSet<Sensor>();

    public Scenario(String sceName, Sensor sensor) throws Exception {

        this.sceName = sceName;

        if (sensor.getSumOfProbability() != 1)
            throw new Exception("Unable to add a sensor in which the sum of probabilities is not equal to 1");
        else
            this.sensors.add(sensor);
    }


    public void addSensor(Sensor sensor) throws Exception {

        if (sensor.getSumOfProbability() != 1)
            throw new Exception("Unable to add a sensor in which the sum of probabilities is not equal to 1");
        for (Sensor s : this.sensors) {
            if (sensor.getRef().equals(s.getRef()))
                throw new Exception("Unable to add a sensor which has a ref key already present in the scenario");
            else this.sensors.add(sensor);
        }
    }

    public String getScenName() {
        return this.sceName;
    }

    public Set<Sensor> getSensors() {
        return this.sensors;
    }

}















