package it.filippetti.sp.simulator;

import io.vertx.core.Vertx;
import it.filippetti.sp.simulator.model.*;
import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class SensorTest {

    @Test(expected = Exception.class)
    public void sensorIsNotCreatedIfPollingTimeIsLowerThan1Second() throws Exception {
        Sensor sensor = new Sensor("refTest", "refTest", "typeTest", 10, "topicTest");
    }


    @Test
    public void measureTypeProduceValueBetweenRangeRandomBased() throws Exception {

        Vertx vertx = Vertx.vertx();
        DateTime simulationStartDateAsParameter = DateTime.parse("2000-10-04T20:02:00"); //data vecchia
        MeasureType measureType1 = new MeasureType("measure1", "key1", "unity1", 20.0, 30.0,null, null, null, null, TypeOfArray.valueOf("M"));
        Model model1 = new Model("model1Test", "0222", 1f);
        model1.addMeasureType(measureType1);
        Sensor sensor = new Sensor("sensoreTest", "refTest", "tipoTest", 1000, "topicTest");
        sensor.addModel(model1);
        Scenario scenario = new Scenario("scenarioTest", sensor);
        int periodOfSimulationTime = 5000; //5 secondi
        Engine engine = new Engine(simulationStartDateAsParameter, periodOfSimulationTime, scenario); //data vecchia
        vertx.deployVerticle(engine);
        Thread.sleep(5000);
        JSONArray snapshots=engine.getLogger().getProducedSnapshots();
        for(int i=0; i<snapshots.length(); i++)
        {
            JSONObject jo = (JSONObject) snapshots.getJSONObject(i).get("snapshot");
            JSONArray ja = jo.getJSONArray("m");
            Double v=ja.getJSONObject(0).getDouble("v");
            boolean isBetweenRange;
            if(v>=measureType1.getMinRange() && v<=measureType1.getMaxRange())
                isBetweenRange=true;
            else isBetweenRange=false;
            assertTrue(isBetweenRange);
        }

    }

    @Test
    public void measureTypeProduceValueBetweenRangeBehaviorBased() throws Exception {

        Vertx vertx = Vertx.vertx();
        DateTime simulationStartDateAsParameter = DateTime.parse("2000-10-04T20:02:00"); //data vecchia
        MeasureType measureType1 = new MeasureType("measure1", "key1", "unity1", 20.0, 30.0,null, null, null, null, TypeOfArray.valueOf("M"), Behavior.INCREASINGLINEAR);
        Model model1 = new Model("model1Test", "0222", 1f);
        model1.addMeasureType(measureType1);
        Sensor sensor = new Sensor("sensoreTest", "refTest", "tipoTest", 1000, "topicTest");
        sensor.addModel(model1);
        Scenario scenario = new Scenario("scenarioTest", sensor);
        int periodOfSimulationTime = 5000; //5 secondi
        Engine engine = new Engine(simulationStartDateAsParameter, periodOfSimulationTime, scenario); //data vecchia
        vertx.deployVerticle(engine);
        Thread.sleep(5000);
        JSONArray snapshots=engine.getLogger().getProducedSnapshots();
        for(int i=0; i<snapshots.length(); i++)
        {
            JSONObject jo = (JSONObject) snapshots.getJSONObject(i).get("snapshot");
            JSONArray ja = jo.getJSONArray("m");
            Double v=ja.getJSONObject(0).getDouble("v");
            boolean isBetweenRange;
            if(v>=measureType1.getMinRange() && v<=measureType1.getMaxRange())
                isBetweenRange=true;
            else isBetweenRange=false;
            assertTrue(isBetweenRange);
        }

    }



    @Test
    public void measureTypeProduceValueProbabilityVarianceFollowingTheTriad() throws Exception {

        Vertx vertx = Vertx.vertx();
        DateTime simulationStartDateAsParameter = DateTime.parse("2000-10-04T20:02:00"); //data vecchia

        TriadOfValueProbabilityVariance tvpv1=new TriadOfValueProbabilityVariance(12.0,3.1,4.3);
        TriadOfValueProbabilityVariance tvpv2=new TriadOfValueProbabilityVariance(32.0,0.1,4.3);
        TriadOfValueProbabilityVariance tvpv3=new TriadOfValueProbabilityVariance(6.0,7.2,11.0);

        List<TriadOfValueProbabilityVariance> listOfTvpv = new ArrayList<>();
        listOfTvpv.add(tvpv1);
        listOfTvpv.add(tvpv2);
        listOfTvpv.add(tvpv3);

        MeasureType measureType1 = new MeasureType("measure1", "key1", "unity1",null, null, TypeOfArray.valueOf("M"), listOfTvpv);
        Model model1 = new Model("model1Test", "0222", 1f);
        model1.addMeasureType(measureType1);
        Sensor sensor = new Sensor("sensoreTest", "refTest", "tipoTest", 1000, "topicTest");
        sensor.addModel(model1);
        Scenario scenario = new Scenario("scenarioTest", sensor);
        int periodOfSimulationTime = 3000; //5 secondi
        Engine engine = new Engine(simulationStartDateAsParameter, periodOfSimulationTime, scenario); //data vecchia
        vertx.deployVerticle(engine);
        Thread.sleep(3000);
        JSONArray snapshots=engine.getLogger().getProducedSnapshots();
        for(int i=0; i<snapshots.length(); i++)
        {
            JSONObject jo = (JSONObject) snapshots.getJSONObject(i).get("snapshot");
            JSONArray ja = jo.getJSONArray("m");
            Double v=ja.getJSONObject(0).getDouble("v");
            Double p=ja.getJSONObject(0).getDouble("p");
            Double l=ja.getJSONObject(0).getDouble("l");

            boolean areExactlyThoseOnes;
            if(measureType1.getListOfTriadOfValueProbabilityVariances().get(i).getValue().equals(v) && measureType1.getListOfTriadOfValueProbabilityVariances().get(i).getProbability().equals(p) && measureType1.getListOfTriadOfValueProbabilityVariances().get(i).getVariance().equals(l))
            areExactlyThoseOnes=true;
            else areExactlyThoseOnes=false;
            assertTrue(areExactlyThoseOnes);
        }

    }




}