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

    private ConcurrentHashMap<String, String> clients = new ConcurrentHashMap<String, String>();
    private ConcurrentHashMap<String, String> clientsReversed = new ConcurrentHashMap<String, String>();

    @Opened("/ws/authentication")
    public void open(@Parameter("client") String client) {
        LOGGER.info("Web socket opened by client: {}", client);
    }

    @Closed("/ws/authentication")
    public void close(@Parameter("client") String client) {
        if(clientsReversed.get(client) != null) {
            clients.remove(clientsReversed.get(client));
            clientsReversed.remove(client);
        }
        LOGGER.info("Web socket closed by client: {}", client);
    }

    @OnMessage("/ws/authentication")
    public void onMessage(@Parameter("client") String client, @Body String message) {
        LOGGER.info("Receiving message from client: {} with content: {}", client, message);
        JsonNode parsedContent = _json.parse(message);
        String clientNickname = parsedContent.get("nickname").asText();
        if(parsedContent.get("isAuthentication").asBoolean()) {
            if(clients.containsKey(clientNickname)){
                LOGGER.error("Client: {} demanding nickname: {} but already in use", client, clientNickname);
                _publisher.send("/ws/authentication", client, "{\"error\":\"used\"}");
            }
            else {
                clients.put(clientNickname, client);
                clientsReversed.put(client, clientNickname);
            }
        }
        else {
            for(String c : clients.keySet()){
                if(!c.equals(clientNickname)){
                    _publisher.send("/ws/authentication", clients.get(c), parsedContent.get("content").toString());
                }
            }
        }
    }
}
