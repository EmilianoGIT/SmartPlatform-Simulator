package it.filippetti.sp.simulator;

import io.netty.handler.codec.mqtt.MqttQoS;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.mqtt.MqttClient;
import io.vertx.mqtt.MqttClientOptions;
import org.json.JSONObject;

public class MqttSender extends AbstractVerticle {

    String brokerMQTTAddress;
    int port;
    String username;
    String password;
    boolean useSSL;


    public MqttSender(String brokerMQTTAddress, int port, String username, String password, boolean useSSL) {
        this.brokerMQTTAddress = brokerMQTTAddress;
        this.port = port;
        this.username = username;
        this.password = password;
        this.useSSL = useSSL;
    }


    @Override
    public void start() throws Exception {

        MqttClientOptions options = new MqttClientOptions();
        options.setAutoGeneratedClientId(true);

        if (!username.equals(""))
            options.setUsername(username);
        if (!password.equals(""))
            options.setPassword(password);

        if (useSSL)
            options.setSsl(true);

        MqttClient client = MqttClient.create(vertx, options);
        client.connect(port, brokerMQTTAddress, s -> {
        });


        vertx.eventBus().consumer("snapshot-for-mqtt-sender", message -> {

            JSONObject jsonObjectForBroker = new JSONObject(message.body().toString());

            String topic = jsonObjectForBroker.get("topic").toString();
            String snapshot = jsonObjectForBroker.getJSONObject("snapshot").toString();

            client.publish(topic,
                    Buffer.buffer(snapshot),
                    MqttQoS.AT_LEAST_ONCE,
                    false,
                    false);

        });
    }

    @Override
    public void stop() throws Exception {

    }

}
