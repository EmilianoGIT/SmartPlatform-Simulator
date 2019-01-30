package it.filippetti.sp.simulator.model;

public enum Behavior {

    INCREASINGLINEAR("increasing-linear"),// y = x
    DECREASINGLINEAR("decreasing-linear"),// y = -x
    INCREASINGEXPONENTIAL("increasing-exponential"),// y = e^x
    DECREASINGEXPONENTIAL("decreasing-exponential"),// y = e^-x
    SINUSOIDAL("sinusoidal"),// y = sin(x)
    COSINUSOIDAL("cosinusoidal"), // y = cos(x)
    GAUSSIAN("gaussian"), //y = e^(-(x^2))
    QUADRATIC("quadratic"), //y = x^2
    CUBIC("cubic"), // y = x^3
    SQUAREROOT("square-root"), //y = x^(1/2)
    CUBICROOT("cubic-root"), //y = x^(1/3)
    LOGARITHMIC("logarithmic"); //y = ln(x)

    private String t;

    Behavior(String t) {
        this.t = t;
    }

    public String getValue() {
        return t;
    }
}
