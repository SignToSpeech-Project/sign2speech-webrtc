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
 * Controller for the Subtitle WebSocket channel
 */
@Controller
public class SubtitleSocket extends DefaultController{

    private static final Logger LOGGER = LoggerFactory.getLogger(SubtitleSocket.class.getName());

    @Requires
    Publisher _publisher;

    @Requires
    Json _json;

    @Opened("/ws/subtitle/{roomID}")
    public void open(@Parameter("roomID") String roomID, @Parameter("client") String client) {
        LOGGER.info("Web socket opened by client: {} in room : {}", client, roomID);
    }

    @Closed("/ws/subtitle/{roomID}")
    public void close(@Parameter("roomID") String roomID, @Parameter("client") String client) {
        LOGGER.info("Web socket closed by client: {} in room : {}", client, roomID);
    }

    @OnMessage("/ws/subtitle/{roomID}")
    public void onMessage(@Parameter("roomID") String roomID, @Parameter("client") String client, @Body String message) {
        LOGGER.info("Receiving message from client: {}  in room : {} with content: {}", client, roomID, message);
        // dispatch the message to all the clients attached to the room
        _publisher.publish("/ws/subtitle/"+roomID, message);
    }
}
