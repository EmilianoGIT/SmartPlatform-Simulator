package it.filippetti.sp.simulator.model;

import it.filippetti.sp.simulator.ErrorProbability;
import it.filippetti.sp.simulator.ErrorRefPresentInScenario;
import it.filippetti.sp.simulator.Sensor;

import java.util.Set;
import java.util.HashSet;

public class Scenario {

    String sceName;
    Set<Sensor> sensors = new HashSet<Sensor>();

    public Scenario(String scenName, Sensor sensor) {

        this.sceName=scenName;
        try{
            if(sensor.getSumOfProbability()!=100)
                throw new ErrorProbability();
            this.sensors.add(sensor);
        } catch (ErrorProbability errorProbability) {
            errorProbability.printStackTrace();
        }
    }


    public void addSensor(Sensor sensor) {

        try{
            if(sensor.getSumOfProbability()!=100)
                throw new ErrorProbability();

            for (Sensor s : this.sensors) {
                if (sensor.getRef().equals(s.getRef()))
                    throw new ErrorRefPresentInScenario();
                else this.sensors.add(sensor);

            }
        } catch (ErrorProbability errorProbability) {
            errorProbability.printStackTrace();
        } catch (ErrorRefPresentInScenario errorRefPresentInScenario) {
            errorRefPresentInScenario.printStackTrace();
        }


    }



    public String getScenName()
    {return this.sceName;}

    public Set<Sensor> getSensors() {
        return this.sensors;
    }


    public void setSourceDestination(String ref, String snapshotModelName, String measureTypeName, String source, String destination) {

        Boolean sourceExists = false;
        Boolean destinationExists = false;


        for (Sensor s : this.sensors) {
            if (source != null) {
                if (source.equals(s.getRef())) {
                    sourceExists = true;
                    break;
                }
            } else {
                sourceExists = true;
                break;
            }
        }


        for (Sensor s : this.sensors)
            if (destination != null) {
                if (destination.equals(s.getRef())) {
                    destinationExists = true;
                    break;
                }
            } else {
                destinationExists = true;
                break;
            }


        if (sourceExists && destinationExists) {

            for (Sensor s : this.sensors) {
                if (ref.equals(s.getRef())) {
                    if (s.getSnapshotModels().containsKey(snapshotModelName)) {
                        SnapshotModel sm = s.getSnapshotModels().get(snapshotModelName);
                        if (sm.getMeasureTypes().containsKey(measureTypeName)) {
                            sm.getMeasureTypes().get(measureTypeName).setSource(source);
                            sm.getMeasureTypes().get(measureTypeName).setDestination(destination);

                            return;
                        } else return;

                    } else return;
                }
            }

        }


    }


}















