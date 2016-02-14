package com.sign2speech.component;

import org.apache.felix.ipojo.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by matth on 13/02/2016.
 */
@Component
@Provides
@Instantiate
public class WebRTCAuthentication {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebRTCAuthentication.class.getName());

    private ConcurrentHashMap _ipToID = new ConcurrentHashMap();

    public void registerNewClient(){

    }

    public void removeClient(){

    }

    @Validate
    public void start(){

    }

    @Invalidate
    public void stop(){

    }
}
