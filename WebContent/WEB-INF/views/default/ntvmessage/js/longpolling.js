;(function($){
  
  $.longpolling = function(options){
    var settings = $.extend({}, $.longpolling.defaults, options);
    var Poll = {
        longpolling: function(){
        var pollURL = settings.pollURL;   
		$.ajax({
          type: 'GET',
          url: pollURL,
          async: true,
          cache: false,
		  ifModified: true,
		  dataType: "json",
          success: function(data, textStatus, jqXHR){
            if(settings.successFunction != null) settings.successFunction(data, textStatus, jqXHR);
			if(settings.ntvLongpoll){
	            setTimeout(Poll.longpolling(), settings.pollTime);
			}
			else{
				setTimeout(Poll.longpolling, settings.pollTime);
			}
          },
          error: function(jqXHR, textStatus, errorThrown){
            if(settings.errorFunction != null) settings.errorFunction(jqXHR, textStatus, errorThrown);
			if(settings.ntvLongpoll){
				setTimeout(Poll.longpolling(), settings.pollErrorTime);
			}else{
				setTimeout(Poll.longpolling, settings.pollErrorTime);
			}
          }
        });
      }
    };

    Poll.longpolling();
  }
  
  $.longpolling.defaults = {
    pollURL: '',
    pollTime: 1000,
    pollErrorTime: 5000,
	ntvLongpoll: true,
    successFunction: null,
    errorFunction: null,
    timestamp: null,
    timestampURLVar: 'timestamp'
  }
  
})(jQuery);

/*
//replaced by startReader
$(function(){
  $.longpolling({
    pollURL: 'http://192.168.1.230:8000/msg_reader/1',
    successFunction: pollSuccess,
    errorFunction: pollError
  });
});
*/