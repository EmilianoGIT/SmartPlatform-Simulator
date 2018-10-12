package it.filippetti.sp.simulator.model;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class MeasureType {

    private static final AtomicInteger COUNTER = new AtomicInteger();
    private final int id;
    String measureTypeName;
    String key;
    String unity;
    Double minRange;
    Double maxRange;
    String source;
    String destination;
    Double variance;
    Double probability;
    TypeOfArray whichArray;
    Behavior behavior = null;
    List<TriadOfValueProbabilityVariance> triadOfValueProbabilityVariances = null;
    int currentIndexOfTriad=0;

    public MeasureType(String measureTypeName, String key, String unity, Double minRange, Double maxRange, String source, String destination, Double probability, Double variance, TypeOfArray whichArray) { //costruttore per misura con value dipendente da range

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

    public MeasureType(String measureTypeName, String key, String unity, Double minRange, Double maxRange, String source, String destination, Double probability, Double variance, TypeOfArray whichArray, Behavior behavior) { ////costruttore per misura con value dipendente da range e andamento

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
        this.behavior = behavior;

    }

    public MeasureType(String measureTypeName, String key, String unity, String source, String destination, TypeOfArray whichArray, List<TriadOfValueProbabilityVariance> triadOfValueProbabilityVariances) { //costruttore per misura con value, probability e variance predefiniti

        this.id = COUNTER.getAndIncrement();
        this.measureTypeName = measureTypeName;
        this.key = key;
        this.unity = unity;
        this.source = source;
        this.destination = destination;
        this.whichArray = whichArray;
        this.triadOfValueProbabilityVariances = triadOfValueProbabilityVariances;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setUnity(String u) {
        this.unity = unity;
    }

    public void setMinRange(Double minRange) {
        this.minRange = minRange;
    }

    public void setMaxRange(Double maxRange) {
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

    public Double getMinRange() {
        return this.minRange;
    }

    public Double getMaxRange() {
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

    public Behavior getBehavior() {
        return this.behavior;
    }

    public List<TriadOfValueProbabilityVariance> getListOfTriadOfValueProbabilityVariances()
    {
        return this.triadOfValueProbabilityVariances;
    }


    public TriadOfValueProbabilityVariance getCurrentTriad()
    {
      return this.triadOfValueProbabilityVariances.get(this.currentIndexOfTriad);
    }

    public void computeNextTriad()
    {
        if(this.currentIndexOfTriad+1==this.triadOfValueProbabilityVariances.size())
            this.currentIndexOfTriad=0;
        else this.currentIndexOfTriad++;
    }

}
