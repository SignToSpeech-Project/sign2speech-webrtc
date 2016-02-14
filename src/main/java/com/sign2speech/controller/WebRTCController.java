package com.sign2speech.controller;

import org.wisdom.api.DefaultController;
import org.wisdom.api.annotations.Controller;
import org.wisdom.api.annotations.Route;
import org.wisdom.api.http.HttpMethod;
import org.wisdom.api.http.Result;

import java.net.URL;

/**
 * Created by matth on 12/02/2016.
 */
@Controller
public class WebRTCController extends DefaultController {

    @Route(method = HttpMethod.GET, uri = "/webrtc")
    public Result welcome() {
        URL indexPage = this.getClass().getClassLoader().getResource("/assets/index.html");
        return ok(indexPage);
    }

}
