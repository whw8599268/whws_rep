/** 投递表MAC字段增加 */
ALTER TABLE EKB_T_TBIMPORTBIDDING  MODIFY COLUMN V_MAC VARCHAR(2000);
/** 新增水运工程施工的项目经理pm */
UPDATE T_SYS_PARAM SET PARAM_VALUE='{"push_name":{"fjs_ptgl_gdyh_hldjfxyf_v1":"tbPmName","fjs_ptgl_sgjl_hldjf_v1":"tbPmName","fjs_gsgl_ljsg_hldjf_v1":"tbPmName","fjs_sygc_gcsgjl_hldjf_v1":"tbPmName","fjs_sygc_gcsg_hldjf_v1":"tbPmName","fjs_gsgl_lmsg_hldjf_v1":"tbPmName","fjs_gsgl_jdsg_hldjf_v1":"tbPmName"}}' WHERE PARAM_ID='a0124ebd9db111e79535000c29112349'