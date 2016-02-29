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

    private ConcurrentHashMap<String, ConcurrentHashMap<String, String>> clients =
            new ConcurrentHashMap<String, ConcurrentHashMap<String, String>>();
    private ConcurrentHashMap<String, ConcurrentHashMap<String, String>> clientsReversed =
            new ConcurrentHashMap<String, ConcurrentHashMap<String, String>>();

    @Opened("/ws/authentication/{roomID}")
    public void open(@Parameter("roomID") String roomID, @Parameter("client") String client) {
        LOGGER.info("Web socket opened by client: {} in room : {}", client, roomID);
    }

    @Closed("/ws/authentication/{roomID}")
    public void close(@Parameter("roomID") String roomID, @Parameter("client") String client) {
        if(clientsReversed.get(roomID) != null){
            if(clientsReversed.get(roomID).get(client) != null) {
                clients.get(roomID).remove(clientsReversed.get(roomID).get(client));
                clientsReversed.get(roomID).remove(client);
            }
            if(clients.get(roomID).isEmpty()){
                clients.remove(roomID);
                clientsReversed.remove(roomID);
            }
        }
        LOGGER.info("Web socket closed by client: {} in room : {}", client, roomID);
    }

    @OnMessage("/ws/authentication/{roomID}")
    public void onMessage(@Parameter("roomID") String roomID, @Parameter("client") String client, @Body String message) {
        LOGGER.info("Receiving message from client: {}  in room : {} with content: {}", client, roomID, message);
        JsonNode parsedContent = _json.parse(message);
        String clientNickname = parsedContent.get("nickname").asText();
        if(parsedContent.get("isAuthentication").asBoolean()) {
            if(clients.get(roomID) != null){
                clients.put(roomID, new ConcurrentHashMap<String, String>());
                clientsReversed.put(roomID, new ConcurrentHashMap<String, String>());
            }
            if(clients.get(roomID).containsKey(clientNickname)){
                LOGGER.error("Client: {} demanding nickname: {} but already in use", client, clientNickname);
                _publisher.send("/ws/authentication/"+roomID, client, "{\"error\":\"used\"}");
            }
            else {
                clients.get(roomID).put(clientNickname, client);
                clientsReversed.get(roomID).put(client, clientNickname);
            }

        }
        else {
            for(String c : clients.get(roomID).keySet()){
                if(!c.equals(clientNickname)){
                    _publisher.send("/ws/authentication/"+roomID, clients.get(roomID).get(c), parsedContent.get("content").toString());
                }
            }
        }
    }
}
