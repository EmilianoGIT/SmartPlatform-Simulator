package it.filippetti.sp.simulator.model;

public class TriadOfValueProbabilityVariance {

    Double value;
    Double probability;
    Double variance;

    public TriadOfValueProbabilityVariance(Double value, Double probability, Double variance) {
        this.value = value;
        this.probability = probability;
        this.variance = variance;
    }

    public Double getValue() {
        return this.value;
    }

    public void setValue(Double value) {
        this.value = value;
    }

    public Double getProbability() {
        return this.probability;
    }

    public void setProbability(Double probability) {
        this.probability = probability;
    }

    public Double getVariance() {
        return this.variance;
    }

    public void setVariance(Double variance) {
        this.value = variance;
    }


}
