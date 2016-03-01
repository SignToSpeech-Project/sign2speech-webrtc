/**
 * Created by matth on 01/03/2016.
 */

function sendRoomID(){
    var room = $("#room").val();
    if(room == ""){
        $("#room").addClass("invalid");
        Materialize.toast('You have to specify a room name.', 3000, 'rounded')
    }
    else{
        document.location.href = "/webrtc/"+room;
    }
}
