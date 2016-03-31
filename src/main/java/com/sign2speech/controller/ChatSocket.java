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

import java.util.concurrent.ConcurrentHashMap;

/**
 * Controller for the chat WebSocket channel
 */
@Controller
public class ChatSocket extends DefaultController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChatSocket.class.getName());

    @Requires
    Publisher _publisher;

    @Requires
    Json _json;

    // Username --> Key
    private ConcurrentHashMap<String, ConcurrentHashMap<String, String>> clients =
            new ConcurrentHashMap<String, ConcurrentHashMap<String, String>>();
    // Key --> Username
    private ConcurrentHashMap<String, ConcurrentHashMap<String, String>> clientsReversed =
            new ConcurrentHashMap<String, ConcurrentHashMap<String, String>>();

    @Opened("/ws/chat/{roomID}")
    public void open(@Parameter("roomID") String roomID, @Parameter("client") String client) {
        LOGGER.info("Web socket opened by client: {} in room : {}", client, roomID);
        // create the room
        if(clients.get(roomID) == null){
            clients.put(roomID, new ConcurrentHashMap<String, String>());
            clientsReversed.put(roomID, new ConcurrentHashMap<String, String>());
        }
    }

    @Closed("/ws/chat/{roomID}")
    public void close(@Parameter("roomID") String roomID, @Parameter("client") String client) {
        // remove client from HashMaps
        if(clients.get(roomID) != null){
            String username = clientsReversed.get(roomID).get(client);

            if(clientsReversed.get(roomID).containsKey(client)) {
                clients.get(roomID).remove(username);
                clientsReversed.get(roomID).remove(client);
            }

            // if the room is empty remove the room
            if(clients.get(roomID).isEmpty()){
                clients.remove(roomID);
            }
            else{
                // if not inform all remaining clients that the client X is disconnected
                for(String c : clientsReversed.get(roomID).keySet()) {
                    if(!c.equals(client)) {
                        _publisher.send("/ws/chat/" + roomID, c, "{\"disconnection\":\"" + username + "\"}");
                    }
                }
            }
            if(clientsReversed.get(roomID).isEmpty()){
                clientsReversed.remove(roomID);
            }
        }
        LOGGER.info("Web socket closed by client: {} in room : {}", client, roomID);
    }

    @OnMessage("/ws/chat/{roomID}")
    public void onMessage(@Parameter("roomID") String roomID, @Parameter("client") String client, @Body String message) {
        LOGGER.info("Receiving message from client: {}  in room : {} with content: {}", client, roomID, message);
        JsonNode parsedContent = _json.parse(message);

        // check if the message contains information about the client (username)
        if(parsedContent.get("isConnection") != null && parsedContent.get("isConnection").asBoolean()){
            if(parsedContent.get("username") != null) {
                // add client to the HashMaps
                clients.get(roomID).put(parsedContent.get("username").asText(), client);
                for(String c : clientsReversed.get(roomID).keySet()) {
                    // inform that client X is connected
                    _publisher.send("/ws/chat/" + roomID, c, "{\"connection\":" + parsedContent.get("username") + "}");
                    // inform client X who is already connected
                    _publisher.send("/ws/chat/" + roomID, client, "{\"alreadyConnected\":\"" + clientsReversed.get(roomID).get(c) + "\"}");
                }
                clientsReversed.get(roomID).put(client, parsedContent.get("username").asText());
            }
        }
        else{
            String toSend = "{\"content\":" + parsedContent.get("content").toString()
                    + ", \"pseudo\":\"" + clientsReversed.get(roomID).get(client) + "\"}";
            _publisher.publish("/ws/chat/"+roomID, toSend);
        }
    }

    /**
     * Check if the username is already used or not
     * @param roomID The name of the room where you want to find if username exists
     * @param username The username to check
     * @return OK if username is not already used, BADREQUEST otherwise
     */
    @Route(method = HttpMethod.GET, uri = "/webrtc/{roomID}/isValid/{username}")
    public Result isValidUsername(@Parameter("roomID") String roomID, @Parameter("username") String username){
        if(clients.get(roomID) != null && clients.get(roomID).containsKey(username)){
            return badRequest("username already used").json();
        }
        return ok("{}").json();
    }
}
