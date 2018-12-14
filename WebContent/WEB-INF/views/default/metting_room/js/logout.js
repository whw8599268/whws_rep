function logout() {
    // 发生Ajax请求
    $.ajax({
        async : false,
        cache : false,
        dataType : "jsonp",
        url : 'http://ra-cloud.okap.com/service/authc/logout',
        // 设置请求方法
        type : 'GET',
        // 成功回调
        success : function(result) {
            top.location.href = "${path}${adminPath}/authc/logout";
        },
        // 失败回调
        error : function(XMLHttpRequest, textStatus, errorThrown) {
            top.location.href = "${path}${adminPath}/authc/logout";
        }
    });
}
