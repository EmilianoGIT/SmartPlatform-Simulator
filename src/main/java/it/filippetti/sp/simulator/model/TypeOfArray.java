package it.filippetti.sp.simulator.model;

public enum TypeOfArray {

    M('m'),
    R('r');

    private char t;

    TypeOfArray(char t) {
        this.t = t;
    }

    char getValue() {
        return t;
    }
}
