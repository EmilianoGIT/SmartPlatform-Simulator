package it.filippetti.sp.simulator.model;

public class TriadOfValueProbabilityVariance {

    Double value;
    Double probability;
    Double variance;

    public TriadOfValueProbabilityVariance(Double value, Double probability, Double variance)
    {
        this.value=value;
        this.probability=probability;
        this.variance=variance;
    }

    public void setValue(Double value)
    {
        this.value=value;
    }

    public void setProbability(Double probability)
    {
        this.probability=probability;
    }

    public void setVariance(Double variance)
    {
        this.value=variance;
    }

    public Double getValue()
    {
        return this.value;
    }

    public Double getProbability()
    {
        return this.probability;
    }

    public Double getVariance()
    {
        return this.variance;
    }


}
