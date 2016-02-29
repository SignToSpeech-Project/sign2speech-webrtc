var localVideo;
var remoteVideo;
var peerConnection;
var peerConnectionConfig = {'iceServers': [{'url': 'stun:stun.services.mozilla.com'}, {'url': 'stun:stun.l.google.com:19302'}]};
var nickname;
var serverConnection;

var subtitleConnection;

navigator.getUserMedia = navigator.getUserMedia || navigator.mozGetUserMedia || navigator.webkitGetUserMedia;
window.RTCPeerConnection = window.RTCPeerConnection || window.mozRTCPeerConnection || window.webkitRTCPeerConnection;
window.RTCIceCandidate = window.RTCIceCandidate || window.mozRTCIceCandidate || window.webkitRTCIceCandidate;
window.RTCSessionDescription = window.RTCSessionDescription || window.mozRTCSessionDescription || window.webkitRTCSessionDescription;

function pageReady() {
    localVideo = document.getElementById('localVideo');
    remoteVideo = document.getElementById('remoteVideo');

    serverConnection = new WebSocket('ws://127.0.0.1:9000/ws/authentication'+roomID);
    serverConnection.onmessage = gotMessageFromServer;

    subtitleConnection = new WebSocket('ws://127.0.0.1:9000/ws/subtitle'+roomID);
    subtitleConnection.onmessage = gotSubtitleFromServer;

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

function sendSubtitle(){
    subtitleConnection.send(JSON.stringify({'content':document.getElementById('subtitle').value}));
}

function setNickname(){
    nickname = document.getElementById('nickname').value;
    serverConnection.send(JSON.stringify({'isAuthentication':true,'nickname':nickname}));
}

function getUserMediaSuccess(stream) {
    localStream = stream;
    localVideo.src = window.URL.createObjectURL(stream);
}

function start(isCaller) {
    peerConnection = new RTCPeerConnection(peerConnectionConfig);
    peerConnection.onicecandidate = gotIceCandidate;
    peerConnection.onaddstream = gotRemoteStream;
    peerConnection.addStream(localStream);

    if(isCaller) {
        if(typeof nickname != 'undefined') {
            peerConnection.createOffer(gotDescription, errorHandler);
        }
        else{
            alert('Nickname not set');
        }
    }
}

function gotMessageFromServer(message) {
    console.log(JSON.parse(message.data));
    if(typeof JSON.parse(message.data).error != 'undefined'){
        if(JSON.parse(message.data).error == 'used') {
            alert('Nickname already used')
        }
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
        serverConnection.send(JSON.stringify({'isAuthentication':false,'nickname':nickname,'content':{'ice': event.candidate}}));
    }
}

function gotDescription(description) {
    console.log('got description');
    peerConnection.setLocalDescription(description, function () {
        serverConnection.send(JSON.stringify({'isAuthentication':false,'nickname':nickname,'content':{'sdp': description}}));
    }, function() {console.log('set description error')});
}

function gotRemoteStream(event) {
    console.log('got remote stream');
    remoteVideo.src = window.URL.createObjectURL(event.stream);
}

function errorHandler(error) {
    console.log(error);
}
