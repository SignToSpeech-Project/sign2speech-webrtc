var localVideo;
var remoteVideo;
var peerConnection;
var peerConnectionConfig = {'iceServers': [{'url': 'stun:stun.services.mozilla.com'}, {'url': 'stun:stun.l.google.com:19302'}]};
var nickname;
var serverConnection;
var subtitleConnection;
var chatConnection;

navigator.getUserMedia = navigator.getUserMedia || navigator.mozGetUserMedia || navigator.webkitGetUserMedia;
window.RTCPeerConnection = window.RTCPeerConnection || window.mozRTCPeerConnection || window.webkitRTCPeerConnection;
window.RTCIceCandidate = window.RTCIceCandidate || window.mozRTCIceCandidate || window.webkitRTCIceCandidate;
window.RTCSessionDescription = window.RTCSessionDescription || window.mozRTCSessionDescription || window.webkitRTCSessionDescription;

function pageReady() {
    localVideo = document.getElementById('localVideo');
    remoteVideo = document.getElementById('remoteVideo');

    serverConnection = new WebSocket('ws://localhost:9000/ws/authentication/'+roomID);
    serverConnection.onmessage = gotMessageFromServer;

    subtitleConnection = new WebSocket('ws://localhost:9000/ws/subtitle/'+roomID);
    subtitleConnection.onmessage = gotSubtitleFromServer;

    chatConnection = new WebSocket('ws://localhost:9000/ws/chat/'+roomID);
    chatConnection.onmessage = gotChatMessageFromServer;
    chatConnection.onopen = sendUsername;

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

function gotSubtitleFromServer(message){
    var content = JSON.parse(message.data);
    console.log(content);
    document.getElementById('subtitleContent').innerHTML = content.content;
}

function gotChatMessageFromServer(message){
    console.log(message);
    var content = JSON.parse(message.data);
    $("#chat-container").html($("#chat-container").html() + "<b>" + content.pseudo + "</b> : " + content.content + "<br>")
}

function sendUsername(){
    chatConnection.send(JSON.stringify({'isConnection':true,'username':username}));
}

function sendChatMessage(){
    chatConnection.send(JSON.stringify({'content':$("#text-content").val()}));
    $("#text-content").val("");
}

function sendSubtitle(){
    subtitleConnection.send(JSON.stringify({'content':document.getElementById('subtitle').value}));
}

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

function gotMessageFromServer(message) {
    console.log(JSON.parse(message.data));
    //TODO add peerConnection.close(); when client is gone
    if(typeof JSON.parse(message.data).caller != 'undefined'){
        start(true);
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
}

function errorHandler(error) {
    console.log(error);
}
