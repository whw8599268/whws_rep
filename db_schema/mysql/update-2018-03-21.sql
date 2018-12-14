/** 新增后台推送投标人菜单 */
INSERT INTO T_SYS_FUNC_INFO (FUNC_ID, FUNC_CODE, FUNC_NAME, FUNC_TYPE, HTTP_URL, FUNC_ICON, ORDER_KEY, IS_LEAF, IS_AVAILABLE, PARENT_ID, FUNC_MEMO) 
VALUES('20e79dd990a840ba9d61da282c08d93f','00090003','推送投标人','1','view/eokb/bus/nodetool/push.bidder.tool.html',NULL,'1','1','1','a2e300dcf06b433ab259eba71819a04b','推送投标人');

/** 修改项目经理的推送信息的格式 */
UPDATE T_SYS_PARAM
SET PARAM_VALUE = '{"fjs_ptgl_gdyh_hldjfxyf_v1":"tbPmName","fjs_ptgl_sgjl_hldjf_v1":"tbPmName","fjs_gsgl_ljsg_hldjf_v1":"tbPmName","fjs_sygc_gcsgjl_hldjf_v1":"tbPmName","fjs_sygc_gcsg_hldjf_v1":"tbPmName","fjs_gsgl_lmsg_hldjf_v1":"tbPmName","fjs_gsgl_jdsg_hldjf_v1":"tbPmName"}'
WHERE PARAM_ID = 'a0124ebd9db111e79535000c29112349';

/** 新增房建市政预清标范本重新结束开标功能*/
INSERT INTO T_SYS_PARAM (PARAM_ID, PARAM_KEY, PARAM_VALUE, PARAM_RULE, PARAM_TIP, PARAM_DEMO)
VALUES ('8ee78a6a2d9c11e88066448a5ba93d00', 'aeolus.eokb.re.end.status', '0', '0|1', '房建市政预清标范本重新结束开标按钮', '0：不显示，1显示');