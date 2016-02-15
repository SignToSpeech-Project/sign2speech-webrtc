package com.sign2speech.controller;

import org.apache.felix.ipojo.annotations.Requires;
import org.wisdom.api.DefaultController;
import org.wisdom.api.annotations.*;
import org.wisdom.api.content.Json;
import org.wisdom.api.http.websockets.Publisher;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by matth on 14/02/2016.
 */
@Controller
public class WebRTCSocket extends DefaultController {

    // TODO basic simple websocket example from GITHUB

    @Requires
    Publisher publisher;

    private ConcurrentHashMap<String, String> clients = new ConcurrentHashMap<String, String>();

    @Requires
    Json json;

    @Opened("/ws/{name}")
    public void open(@Parameter("name") String name) {
        System.out.println("Web socket opened => " + name);
    }

    @Closed("/ws/{name}")
    public void close(@Parameter("name") String name) {
        System.out.println("Web socket closed => " + name);
    }

    @OnMessage("/ws/{name}")
    public void onMessage(@Body String message, @Parameter("name") String name) {
        System.out.println("Receiving message on " + name + " : " + message);
        //publisher.publish("/ws/" + name, json.toJson(message.toUpperCase()));
        publisher.publish("/ws/" + name, message);
    }
}
