package it.filippetti.sp.simulator.model;

import java.util.concurrent.atomic.AtomicInteger;

public class MeasureType {

    private static final AtomicInteger COUNTER = new AtomicInteger();
    private final int id;
    String measureTypeName;
    String key;
    String unity;
    String minRange;
    String maxRange;
    String source;
    String destination;
    Double variance;
    Double probability;
    TypeOfArray whichArray;

    public MeasureType(String measureTypeName, String key, String unity, String minRange, String maxRange, String source, String destination, Double probability, Double variance, TypeOfArray whichArray) {

        this.id = COUNTER.getAndIncrement();
        this.measureTypeName = measureTypeName;
        this.key = key;
        this.unity = unity;
        this.minRange = minRange;
        this.maxRange = maxRange;
        this.source = source;
        this.destination = destination;
        this.probability = probability;
        this.variance = variance;
        this.whichArray = whichArray;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setUnity(String u) {
        this.unity = unity;
    }

    public void setMinRange(String minRange) {
        this.minRange = minRange;
    }

    public void setMaxRange(String maxRange) {
        this.maxRange = maxRange;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public void setVariance(Double variance) {
        this.variance = variance;
    }

    public void setProbability(Double probability) {
        this.probability = probability;
    }

    public void setWhichArray(TypeOfArray whichArray) {
        this.whichArray = whichArray;
    }

    public int getId() {
        return this.id;
    }

    public String getMeasureTypeName() {
        return this.measureTypeName;
    }

    public String getKey() {
        return this.key;
    }

    public String getUnity() {
        return this.unity;
    }

    public String getMinRange() {
        return this.minRange;
    }

    public String getMaxRange() {
        return this.maxRange;
    }

    public String getSource() {
        return this.source;
    }

    public String getDestination() {
        return this.destination;
    }

    public Double getVariance() {
        return this.variance;
    }

    public Double getProbability() {
        return this.probability;
    }

    public char getWhichType() {
        return this.whichArray.getValue();
    }


}
