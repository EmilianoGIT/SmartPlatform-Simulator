package it.filippetti.sp.simulator.model;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class MeasureType {

    private static final AtomicInteger COUNTER = new AtomicInteger();
    private final int id;
    private String measureTypeName;
    private String key;
    private String unity;
    private Double minRange;
    private Double maxRange;
    private String source;
    private String destination;
    private Double variance;
    private Double probability;
    private TypeOfArray whichArray;
    private Behavior behavior = null;
    private List<TriadOfValueProbabilityVariance> triadOfValueProbabilityVariances = null;
    private int currentIndexOfTriad = 0;

    public MeasureType(String measureTypeName, String key, String unity, Double minRange, Double maxRange, String source, String destination, Double probability, Double variance, TypeOfArray whichArray) throws Exception { //costruttore per misura con value dipendente da range

        if (minRange > maxRange) throw new Exception("Min non può essere > di Max");
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

    public MeasureType(String measureTypeName, String key, String unity, Double minRange, Double maxRange, String source, String destination, Double probability, Double variance, TypeOfArray whichArray, Behavior behavior) throws Exception { ////costruttore per misura con value dipendente da range e andamento

        if (minRange > maxRange) throw new Exception("Min non può essere > di Max");
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

    public MeasureType(String measureTypeName, String key, String unity, String source, String destination, TypeOfArray whichArray, List<TriadOfValueProbabilityVariance> triadOfValueProbabilityVariances) throws Exception { //costruttore per misura con value, probability e variance predefiniti

        if (triadOfValueProbabilityVariances == null || triadOfValueProbabilityVariances.isEmpty())
            throw new Exception("I values non possono essere null o vuoti");
        this.id = COUNTER.getAndIncrement();
        this.measureTypeName = measureTypeName;
        this.key = key;
        this.unity = unity;
        this.source = source;
        this.destination = destination;
        this.whichArray = whichArray;
        this.triadOfValueProbabilityVariances = triadOfValueProbabilityVariances;
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

    public List<TriadOfValueProbabilityVariance> getListOfTriadOfValueProbabilityVariances() {
        return this.triadOfValueProbabilityVariances;
    }


    public TriadOfValueProbabilityVariance getCurrentTriad() {
        return this.triadOfValueProbabilityVariances.get(this.currentIndexOfTriad);
    }

    public void computeNextTriad() {
        if (this.currentIndexOfTriad + 1 == this.triadOfValueProbabilityVariances.size())
            this.currentIndexOfTriad = 0;
        else this.currentIndexOfTriad++;
    }

}
