package it.filippetti.sp.simulator;


import io.vertx.core.Vertx;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

public class Main {

    public static void main(String[] args) throws Exception {

        ServerRestSimulation serverRestSimulation;
        MqttSender mqttSender;

        Vertx vertx = Vertx.vertx();

        File configFile = new File("config.properties");

        try {
            FileReader reader = new FileReader(configFile);
            Properties props = new Properties();
            props.load(reader);

            String serverRestPort = props.getProperty("server_rest_port");
            String mqttBrokerAddress = props.getProperty("mqtt_broker_address");
            String mqttBrokerPort = props.getProperty("mqtt_broker_port");

            System.out.println("Server Rest port: " + serverRestPort);
            System.out.println("MQTT Broker address: " + mqttBrokerAddress);
            System.out.println("MQTT Broker port" + mqttBrokerPort);
            reader.close();

            if (serverRestPort == null || mqttBrokerAddress == null || mqttBrokerPort == null)
                throw new Exception();

            serverRestSimulation = new ServerRestSimulation(Integer.parseInt(serverRestPort));
            mqttSender = new MqttSender(mqttBrokerAddress, Integer.parseInt(mqttBrokerPort));

            vertx.deployVerticle(serverRestSimulation);
            vertx.deployVerticle(mqttSender);

        } catch (FileNotFoundException ex) {
            // file does not exist
        } catch (IOException ex) {
            // I/

        } catch (Exception e) {

        }


    }


}
