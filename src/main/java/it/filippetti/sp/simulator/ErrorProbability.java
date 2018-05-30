package it.filippetti.sp.simulator;

public class ErrorProbability extends Exception {

    public ErrorProbability() {
        super("Problema con la probabilità : ");
    }

    @Override
    public String toString() {
        return getMessage() + "La somma delle probabilità dei modelli di snapshot del sensore che stai inserendo non ha raggiunto il 100%";
    }
}
