package it.filippetti.sp.simulator.model;

import org.junit.Test;

public class MeasureTypeTest {

    @Test(expected = Exception.class)
    public void measureTypeIsNotCreatedIfMinIsGreaterThanMax() throws Exception {
            MeasureType measureType1 = new MeasureType("measure1", "key1", "unity1", 50.0, 30.0, "s1", "d1", 3.0, 4.0, TypeOfArray.valueOf("M"));
    }

    @Test(expected = Exception.class)
    public void measureTypeWithBehaviorIsNotCreatedIfMinIsGreaterThanMax() throws Exception {
            MeasureType measureType1 = new MeasureType("measure1", "key1", "unity1", 50.0, 30.0, "s1", "d1", 3.0, 4.0, TypeOfArray.valueOf("M"), Behavior.INCREASINGLINEAR);
    }

    @Test(expected = Exception.class)
    public void measureTypeWithValuesIsNotCreatedIfMinIsGreaterThanMax() throws Exception {
        MeasureType measureType1 = new MeasureType("measure1", "key1", "unity1", 50.0, 30.0, "s1", "d1", 3.0, 4.0, TypeOfArray.valueOf("M"), Behavior.INCREASINGLINEAR);
    }


}