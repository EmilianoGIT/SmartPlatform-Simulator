package it.filippetti.sp.simulator;

public class ErrorDate extends Exception{

    public ErrorDate()
    {
        super("Problema con le date : ");
    }

    @Override
    public String toString()
    {
        return getMessage() + "Hai sbagliato ad impostare le date";
    }
}
