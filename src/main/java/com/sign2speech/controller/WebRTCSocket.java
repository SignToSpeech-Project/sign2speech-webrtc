package com.sign2speech.controller;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.felix.ipojo.annotations.Requires;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wisdom.api.DefaultController;
import org.wisdom.api.annotations.*;
import org.wisdom.api.content.Json;
import org.wisdom.api.http.HttpMethod;
import org.wisdom.api.http.Result;
import org.wisdom.api.http.websockets.Publisher;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Controller for the WebRTC authentication WebSocket channel
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
        // create the room
        if(clients.get(roomID) == null){
            clients.put(roomID, new ArrayList<String>());
        }
        clients.get(roomID).add(client);
    }

    @Closed("/ws/authentication/{roomID}")
    public void close(@Parameter("roomID") String roomID, @Parameter("client") String client) {
        // remove client from the HashMaps
        if(clients.get(roomID) != null){
            if(clients.get(roomID).contains(client)) {
                clients.get(roomID).remove(client);
            }
            if(clients.get(roomID).isEmpty()){
                clients.remove(roomID);
            }
            else{
                // inform that client X is disconnected
                _publisher.publish("/ws/authentication/"+roomID, "{\"disconnection\":true}");
            }
        }
        LOGGER.info("Web socket closed by client: {} in room : {}", client, roomID);
    }

    @OnMessage("/ws/authentication/{roomID}")
    public void onMessage(@Parameter("roomID") String roomID, @Parameter("client") String client, @Body String message) {
        LOGGER.info("Receiving message from client: {}  in room : {} with content: {}", client, roomID, message);
        JsonNode parsedContent = _json.parse(message);
        // check if the client is ready
        if(parsedContent.get("isReady") != null && parsedContent.get("isReady").asBoolean()) {
            // if the client is not alone in the room assign him the role of caller
            if(clients.get(roomID).size() > 1) {
                _publisher.send("/ws/authentication/" + roomID, client, "{\"caller\":true}");
            }
        }
        else {
            // otherwise dispatch the message to all clients excepted the base client
            for(String c : clients.get(roomID)){
                if(!c.equals(client)){
                    _publisher.send("/ws/authentication/"+roomID, c, parsedContent.get("content").toString());
                }
            }
        }
    }

    /**
     * Check if a room is full or not
     * @param roomID The name of the room to check
     * @return OK if the room is not full, BADREQUEST otherwise
     */
    @Route(method = HttpMethod.GET, uri = "/webrtc/{roomID}/isFull")
    public Result isFull(@Parameter("roomID") String roomID){
        if(clients.get(roomID) != null && clients.get(roomID).size() == 2){
            return badRequest("The requested room is full.").json();
        }
        return ok("{}").json();
    }
}
