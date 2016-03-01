package com.sign2speech.controller;

import org.wisdom.api.DefaultController;
import org.wisdom.api.annotations.Controller;
import org.wisdom.api.annotations.Parameter;
import org.wisdom.api.annotations.Route;
import org.wisdom.api.annotations.View;
import org.wisdom.api.http.HttpMethod;
import org.wisdom.api.http.Result;
import org.wisdom.api.templates.Template;

import java.net.URL;

/**
 * Created by matth on 12/02/2016.
 */
@Controller
public class WebRTCController extends DefaultController {

    /**
     * Injects a template named 'welcome'.
     */
    @View("chat-room")
    Template chatRoom;

    @Route(method = HttpMethod.GET, uri = "/webrtc")
    public Result connection() {
        URL indexPage = this.getClass().getClassLoader().getResource("/assets/connection.html");
        return ok(indexPage);
    }

    @Route(method = HttpMethod.GET, uri = "/webrtc/{roomID}")
    public Result chatRoom(@Parameter("roomID") String roomID) {
        return ok(render(chatRoom, "roomID", roomID));
    }

}
