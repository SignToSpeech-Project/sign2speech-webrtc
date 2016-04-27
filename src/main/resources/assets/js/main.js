var localVideo;
var remoteVideo;
var peerConnection;
var peerConnectionConfig = {'iceServers': [{'url': 'stun:stun.services.mozilla.com'}, {'url': 'stun:stun.l.google.com:19302'}]};
var serverConnection;
var subtitleConnection;
var chatConnection;
var textContainer;

var timeoutHandle = null;

var server = "ws://localhost:9000";

navigator.getUserMedia = navigator.getUserMedia || navigator.mozGetUserMedia || navigator.webkitGetUserMedia;
window.RTCPeerConnection = window.RTCPeerConnection || window.mozRTCPeerConnection || window.webkitRTCPeerConnection;
window.RTCIceCandidate = window.RTCIceCandidate || window.mozRTCIceCandidate || window.webkitRTCIceCandidate;
window.RTCSessionDescription = window.RTCSessionDescription || window.mozRTCSessionDescription || window.webkitRTCSessionDescription;

$(document).ready(function(){
    $('#input-content').keypress(function(e){
        if(e.keyCode==13)
            $('#submit').click();
    });
});

function pageReady() {
    localVideo = document.getElementById('localVideo');
    remoteVideo = document.getElementById('remoteVideo');

    serverConnection = new WebSocket(server+'/ws/authentication/'+roomID);
    serverConnection.onmessage = gotMessageFromServer;

    subtitleConnection = new WebSocket(server+'/ws/subtitle/'+roomID);
    subtitleConnection.onmessage = gotSubtitleFromServer;

    chatConnection = new WebSocket(server+'/ws/chat/'+roomID);
    chatConnection.onmessage = gotChatMessageFromServer;
    chatConnection.onopen = sendUsername;

    textContainer = $("#text-container");
    textContainer.html("Hello <b>"+username+"</b>! You are logged in the room: <b>"+roomID+"</b> and you are ready to chat! :)<br>");
    textContainer.html(textContainer.html()+"#######################################<br><br>");

    var constraints = {
        video: true,
        audio: true
    };

    if(navigator.getUserMedia) {
        navigator.getUserMedia(constraints, getUserMediaSuccess, errorHandler);
    } else {
        alert('Your browser does not support getUserMedia API');
    }
}

/** Message handlers **/
function gotSubtitleFromServer(message){
    var content = JSON.parse(message.data);
    console.log(content);
    document.getElementById('subtitleContent').innerHTML = content.content;
    if(timeoutHandle != null){
        window.clearTimeout(timeoutHandle);
    }
    $("#subtitleContent").css("display","");
    timeoutHandle = window.setTimeout(function(){
        $("#subtitleContent").fadeOut("fast", "linear");
        timeoutHandle = null;
    }, 3000);
    var date = new Date();
    textContainer.html(textContainer.html() + date.getHours() + ":" + date.getMinutes() + "'" + date.getSeconds() + " - New subtitle: <b>"+content.content+"</b><br>");
    $("#text-container").scrollTop($("#text-container")[0].scrollHeight);
}

function gotChatMessageFromServer(message){
    console.log(message);
    var content = JSON.parse(message.data);
    if(typeof content.connection != 'undefined'){
        textContainer.html(textContainer.html() + "[<b>"+content.connection+"</b> is now connected]<br>");
    }
    else if(typeof content.disconnection != 'undefined'){
        textContainer.html(textContainer.html() + "[<b>"+content.disconnection+"</b> is now disconnected]<br>");
    }
    else if(typeof content.alreadyConnected != 'undefined'){
        textContainer.html(textContainer.html() + "[<b>"+content.alreadyConnected+"</b> is already connected]<br>");
    }
    else {
        textContainer.html(textContainer.html() + "<b>" + content.pseudo + "</b> : " + content.content + "<br>");
    }
    $("#text-container").scrollTop($("#text-container")[0].scrollHeight);
}

function gotMessageFromServer(message) {
    console.log(JSON.parse(message.data));
    if(typeof JSON.parse(message.data).caller != 'undefined'){
        start(true);
    }
    else if(typeof JSON.parse(message.data).disconnection != 'undefined'){
        if(typeof peerConnection != 'undefined') {
            peerConnection.close();
            remoteVideo.src = null;
        }
        $("#loader").css({"display":""});
        start(false);
    }
    else {
        if (!peerConnection) start(false);

        var signal = JSON.parse(message.data);
        if (signal.sdp) {
            peerConnection.setRemoteDescription(new RTCSessionDescription(signal.sdp), function () {
                peerConnection.createAnswer(gotDescription, errorHandler);
            }, errorHandler);
        } else if (signal.ice) {
            peerConnection.addIceCandidate(new RTCIceCandidate(signal.ice));
        }
    }
}

/** Message sending functions **/
function sendUsername(){
    chatConnection.send(JSON.stringify({'isConnection':true,'username':username}));
}

function sendChatMessage(){
    chatConnection.send(JSON.stringify({'content':$("#input-content").val()}));
    $("#input-content").val("");
}

function sendSubtitle(){
    subtitleConnection.send(JSON.stringify({'content':document.getElementById('subtitle').value})); 
}

/** Others **/
function getUserMediaSuccess(stream) {
    localStream = stream;
    localVideo.src = window.URL.createObjectURL(stream);

    serverConnection.send(JSON.stringify({'isReady':true}))
}

function start(isCaller) {
    peerConnection = new RTCPeerConnection(peerConnectionConfig);
    peerConnection.onicecandidate = gotIceCandidate;
    peerConnection.onaddstream = gotRemoteStream;
    peerConnection.addStream(localStream);

    if(isCaller) {
        peerConnection.createOffer(gotDescription, errorHandler);
    }
}

function gotIceCandidate(event) {
    if(event.candidate != null) {
        serverConnection.send(JSON.stringify({'content':{'ice': event.candidate}}));
    }
}

function gotDescription(description) {
    console.log('got description');
    peerConnection.setLocalDescription(description, function () {
        serverConnection.send(JSON.stringify({'content':{'sdp': description}}));
    }, function() {console.log('set description error')});
}

function gotRemoteStream(event) {
    console.log('got remote stream');
    remoteVideo.src = window.URL.createObjectURL(event.stream);
    $("#loader").css({"display":"none"});
}

function errorHandler(error) {
    console.log(error);
}
