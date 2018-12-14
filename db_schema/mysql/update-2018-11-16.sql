/**获取证书信息地址**/
INSERT INTO t_sys_param (PARAM_ID, PARAM_KEY, PARAM_VALUE, PARAM_RULE, PARAM_TIP, PARAM_DEMO) 
VALUES('71065333736511e8be47106530297fc2','aeolus.oauth2.mobile.get.cert.info.url','http://192.168.1.194:8080/ra_cloud/service/rsapi/mobile/certinfo','.*','获取证书信息接口地址不允许为空!','获取证书信息接口地址');
/**获取证书对应的用户信息地址**/
INSERT INTO t_sys_param (PARAM_ID, PARAM_KEY, PARAM_VALUE, PARAM_RULE, PARAM_TIP, PARAM_DEMO) 
VALUES('297fc233736511e8be47106530297fc2','aeolus.oauth2.mobile.get.user.info.url','http://192.168.1.194:8080/ra_cloud/service/rsapi/mobile/userinfo','.*','获取证书对应的用户信息接口地址不允许为空!','获取证书对应的用户信息接口地址');
