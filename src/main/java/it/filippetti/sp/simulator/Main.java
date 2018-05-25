package it.filippetti.sp.simulator;


import com.mongodb.*;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.util.JSON;
import io.vertx.core.Vertx;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.swing.text.Document;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {

    public static void main(String[] args) throws Exception {

        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new Server());


    }





}
