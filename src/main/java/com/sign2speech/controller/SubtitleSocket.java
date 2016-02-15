package com.sign2speech.controller;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.felix.ipojo.annotations.Requires;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wisdom.api.DefaultController;
import org.wisdom.api.annotations.*;
import org.wisdom.api.content.Json;
import org.wisdom.api.http.websockets.Publisher;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by matthieu on 15/02/16.
 */
@Controller
public class SubtitleSocket extends DefaultController{

    private static final Logger LOGGER = LoggerFactory.getLogger(SubtitleSocket.class.getName());

    @Requires
    Publisher _publisher;

    @Requires
    Json _json;

    @Opened("/ws/subtitle")
    public void open(@Parameter("client") String client) {
        LOGGER.info("Web socket opened by client: {}", client);
    }

    @Closed("/ws/subtitle")
    public void close(@Parameter("client") String client) {
        LOGGER.info("Web socket closed by client: {}", client);
    }

    @OnMessage("/ws/subtitle")
    public void onMessage(@Parameter("client") String client, @Body String message) {
        LOGGER.info("Receiving message from client: {} with content: {}", client, message);
        _publisher.publish("/ws/subtitle", message);
    }
}
