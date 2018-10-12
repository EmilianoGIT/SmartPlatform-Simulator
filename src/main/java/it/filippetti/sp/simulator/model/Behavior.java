package it.filippetti.sp.simulator.model;

public enum Behavior {

    INCREASINGLINEAR("increasing-linear"),
    DECREASINGLINEAR("decreasing-linear"),
    INCREASINGEXPONENTIAL("increasing-exponential"),
    DECREASINGEXPONENTIAL("decreasing-exponential"),
    SINUSOIDAL("sinusoidal"),
    COSINUSOIDAL("cosinusoidal"),
    GAUSSIAN("gaussian");

    private String t;

    Behavior(String t) {
        this.t = t;
    }

    String getValue() {
        return t;
    }
}
