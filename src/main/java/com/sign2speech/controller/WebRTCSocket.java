package com.sign2speech.controller;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.felix.ipojo.annotations.Requires;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wisdom.api.DefaultController;
import org.wisdom.api.annotations.*;
import org.wisdom.api.content.Json;
import org.wisdom.api.http.websockets.Publisher;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by matth on 14/02/2016.
 */
@Controller
public class WebRTCSocket extends DefaultController {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebRTCSocket.class.getName());

    @Requires
    Publisher _publisher;

    @Requires
    Json _json;

    private ConcurrentHashMap<String, ArrayList<String>> clients = new ConcurrentHashMap<String, ArrayList<String>>();

    @Opened("/ws/authentication/{roomID}")
    public void open(@Parameter("roomID") String roomID, @Parameter("client") String client) {
        LOGGER.info("Web socket opened by client: {} in room : {}", client, roomID);
        if(clients.get(roomID) == null){
            clients.put(roomID, new ArrayList<String>());
        }
        clients.get(roomID).add(client);
    }

    @Closed("/ws/authentication/{roomID}")
    public void close(@Parameter("roomID") String roomID, @Parameter("client") String client) {
        if(clients.get(roomID) != null){
            if(clients.get(roomID).contains(client)) {
                clients.get(roomID).remove(client);
            }
            if(clients.get(roomID).isEmpty()){
                clients.remove(roomID);
            }
        }
        LOGGER.info("Web socket closed by client: {} in room : {}", client, roomID);
    }

    @OnMessage("/ws/authentication/{roomID}")
    public void onMessage(@Parameter("roomID") String roomID, @Parameter("client") String client, @Body String message) {
        LOGGER.info("Receiving message from client: {}  in room : {} with content: {}", client, roomID, message);
        JsonNode parsedContent = _json.parse(message);
        if(parsedContent.get("isReady") != null && parsedContent.get("isReady").asBoolean()) {
            if(clients.get(roomID).size() > 1) {
                _publisher.send("/ws/authentication/" + roomID, client, "{\"caller\":true}");
            }
        }
        else {
            for(String c : clients.get(roomID)){
                if(!c.equals(client)){
                    _publisher.send("/ws/authentication/"+roomID, c, parsedContent.get("content").toString());
                }
            }
        }
    }
}
