package it.filippetti.sp.simulator.model;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class SnapshotModel {

    private static final AtomicInteger COUNTER = new AtomicInteger();
    private final int id;
    String modelName;
    String cat;
    Float probability;
    HashMap<Integer, MeasureType> measureTypes = new HashMap<Integer, MeasureType>();

    public SnapshotModel(String modelName, String cat, Float probability) {
        this.id = COUNTER.getAndIncrement();
        this.modelName = modelName;
        this.cat = cat;
        this.probability = probability;
    }

    public void addMeasureType(MeasureType measureType) {
        try {
            this.measureTypes.put(measureType.getId(), measureType);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int getId() {
        return this.id;
    }

    public String getModelName() {
        return this.modelName;
    }

    public String getCategory() {
        return this.cat;
    }


    public Float getProbability() {
        return this.probability;
    }

    public HashMap<Integer, MeasureType> getMeasureTypes() {
        return this.measureTypes;
    }
}
