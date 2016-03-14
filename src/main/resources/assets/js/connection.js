/**
 * Created by matth on 01/03/2016.
 */

function sendRoomID(){
    var username = $("#nickname").val();
    var room = $("#room").val();

    if(room == ""){
        $("#room").addClass("invalid");
        Materialize.toast('You have to specify a room name.', 3000, 'rounded');
    }
    else if(username == ""){
        $("#nickname").addClass("invalid");
        Materialize.toast('You have to specify a Nickname.', 3000, 'rounded');
    }
    else {
        $.ajax({
            url: "/webrtc/" + room + "/isFull",
            success: function () {
                $.ajax({
                    url: "/webrtc/" + room + "/isValid/" + username,
                    success: function () {
                        document.location.href = "/webrtc/roomID/"+room+"/username/"+username;
                    },
                    error: function () {
                        $("#nickname").addClass("invalid");
                        Materialize.toast('The nickname '+username+' is already used.', 3000, 'rounded');
                    }
                });
            },
            error: function () {
                $("#room").addClass("invalid");
                Materialize.toast('The room is full.', 3000, 'rounded');
            }
        });
    }
}
