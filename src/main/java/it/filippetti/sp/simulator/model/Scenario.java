package it.filippetti.sp.simulator.model;

import it.filippetti.sp.simulator.Sensor;
import java.util.Set;
import java.util.HashSet;

public class Scenario {

    String sceName;
    Set<Sensor> sensors = new HashSet<Sensor>();

    public Scenario(String scenName, Sensor sensor) throws Exception {

        this.sceName = scenName;

        if (sensor.getSumOfProbability() != 1)
            throw new Exception("Impossibile inserire un sensore in cui i modelli non abbiano somma delle probabilità uguale a 1");
        else
            this.sensors.add(sensor);
    }


    public void addSensor(Sensor sensor) throws Exception {

        if (sensor.getSumOfProbability() != 1)
            throw new Exception("Impossibile inserire un sensore in cui i modelli non abbiano somma delle probabilità uguale a 1");
        for (Sensor s : this.sensors) {
            if (sensor.getRef().equals(s.getRef()))
                throw new Exception("Impossibile inserire un sensore con ref già presente nello scenario");
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















