package it.filippetti.sp.simulator;

import io.netty.handler.codec.mqtt.MqttQoS;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.mqtt.MqttClient;
import io.vertx.mqtt.MqttClientOptions;

public class MqttSender extends AbstractVerticle {

    public MqttSender()
    {

    }


    @Override
    public void start() throws Exception {

        MqttClientOptions options = new MqttClientOptions();

        MqttClient client = MqttClient.create(vertx, options);
       // MqttClient client = MqttClient.create(vertx);
        client.connect(1883, "192.168.1.9", s -> {
        });


        /*
        MessageConsumer<JsonObject> consumer = vertx.eventBus().consumer("snapshot-for-mqtt-sender");
        consumer.handler(message -> {
            String otopic = message.body().getString("topic");
            JsonObject snap = message.body().getJsonObject("snapshot");
        });
        */
        vertx.eventBus().consumer("snapshot-for-mqtt-sender", message -> {


            String[] splitted=message.body().toString().split("tsDelimiter");
         //   System.out.println(splitted[0]);
         //   System.out.println(splitted[1]);
            //fai publish al Broker Mqtt
            client.publish(splitted[0],
                    Buffer.buffer(splitted[1]),
                    MqttQoS.AT_LEAST_ONCE,
                    false,
                    false);

        });
    }

    @Override
    public void stop() throws Exception {

    }

}
