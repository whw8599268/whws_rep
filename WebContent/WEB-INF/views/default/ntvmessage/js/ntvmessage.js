//Reader
function startLongPollReader(url)
{
	$.longpolling({
		pollURL: url,
		successFunction: onMsgReceive,
		errorFunction: onMsgError
  });
}

function initNtvMsgReader(msgServer,objID){
  if(arguments.length==1){
	  startLongPollReader(msgServer); 
  }else{
	  var url = "http://" + msgServer + "/msg_reader/" + objID;
	  startLongPollReader(url); 
  }
}

//Writor
function postJsonData(url,jsonString)
{
	 $.ajax({
        type: "POST",
        url: url,
        contentType: "application/json", 
        dataType: "json",
        data: jsonString,
        success: function (jsonResult) {
            //返回处理，忽略
        },
		error: function(jqXHR, textStatus, errorThrown){
			//
        }
    });
}

var ntvMsgPostUrl = "";
function initNtvMsgWritor(msgServer,objID){
  if(arguments.length==1){
	  ntvMsgPostUrl = msgServer;
  }else{
	  ntvMsgPostUrl = "http://" + msgServer + "/msg_writor/" + objID;
  }
}

function postJsonMsg(jsonString) {
	if(ntvMsgPostUrl=="") return;
	postJsonData(ntvMsgPostUrl,jsonString);
}

//Counter,set ntvLongpoll as false.
function startLongPollCounter(url)
{
	$.longpolling({
		pollURL: url,
		ntvLongpoll: false,
		pollTime: 5000,
        pollErrorTime: 10000,
		successFunction: onCounterMsgReceive,
		errorFunction: onMsgError
  });
}

function initNtvUserCounter(msgServer,objID){
  if(arguments.length==1){
	startLongPollCounter(msgServer);
  }else{
	var url = "http://" + msgServer + "/msg_writor/" + objID;
    startLongPollCounter(url);
  }
}
/*
//replaced by startLongPollReader
$(function(){
  $.longpolling({
    pollURL: 'http://192.168.1.230:8000/msg_reader/1',
    successFunction: pollSuccess,
    errorFunction: pollError
  });
});
*/

function writeMsgQueue(queueId,code,data){
	var json = [];
    var row  = {};
    row.id   = queueId;
    row.code = code;
    row.data = data;
    json.push(row);

    var msg  = JSON.stringify(json);
	postJsonMsg(msg);
}
