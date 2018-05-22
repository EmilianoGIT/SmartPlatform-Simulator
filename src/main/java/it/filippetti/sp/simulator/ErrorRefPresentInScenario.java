package it.filippetti.sp.simulator;

public class ErrorRefPresentInScenario extends Exception {

    public ErrorRefPresentInScenario()
    {
        super("Problema con la ref : ");
    }

    @Override
    public String toString()
    {
        return getMessage() + "Non è possibile inserire sensori con ref già presente nello scenarario";
    }
}
