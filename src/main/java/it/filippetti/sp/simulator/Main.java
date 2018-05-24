package it.filippetti.sp.simulator;


import com.mongodb.*;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.util.JSON;
import io.vertx.core.Vertx;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.swing.text.Document;

public class Main {

    public static void main(String[] args) throws Exception {

        MongoClient mongoClient=new MongoClient("localhost",27017 );
        DB db = mongoClient.getDB("simulator");
        DBCollection collection=db.getCollection("scenarios");

        JSONObject jo=new JSONObject();
        jo.put("ciao", 2);

        DBObject dbObject = (DBObject) JSON
                .parse(jo.toString());
        collection.insert(dbObject);

        DBCursor dbCursor=collection.find();
        for(DBObject dbO: dbCursor)
        {System.out.println(dbO.toString());}
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new Server());

    }


    public void genMeasureTypes() {
        Integer count = 0;
        JSONObject collector = new JSONObject();


        JSONArray measureTypes = new JSONArray();

        JSONObject measureType = new JSONObject();

        do {

            measureType.put("name", "misurazione bella");
            measureType.put("k", "device_temperature");
            measureType.put("u", "C°");
            measureType.put("Min", "20");
            measureType.put("Max", "30");
            measureType.put("SelectArray", "m");
            count++;
            measureTypes.put(measureType);
        } while (count <= 10);


        collector.put("measureTypes", measureTypes);

        System.out.println(collector);
    }


}
