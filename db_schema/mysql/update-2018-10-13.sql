/* 投递标新增投递MAC地址*/
ALTER TABLE EKB_T_TBIMPORTBIDDING ADD COLUMN V_JSON_OBJ longtext DEFAULT NULL COMMENT '用户上传的mac地址' ;
/*公告时间*/
ALTER TABLE EKB_T_TENDER_PROJECT_INFO ADD COLUMN V_NOTICE_PUBLIC_TIME VARCHAR(20) DEFAULT NULL COMMENT '公告发布时间';
/*封标标识*/
ALTER TABLE EKB_T_TENDER_PROJECT_INFO ADD COLUMN N_FB_STATUS INT(4) DEFAULT 0 COMMENT '封标标识';

/*同步信用分菜单*/
INSERT INTO T_SYS_FUNC_INFO (FUNC_ID, FUNC_CODE, FUNC_NAME, FUNC_TYPE, HTTP_URL, FUNC_ICON, ORDER_KEY, IS_LEAF, IS_AVAILABLE, PARENT_ID, FUNC_MEMO) VALUES('e550173e30d64244af0a2e495fd86977','00040010','同步信用分','1','view/job/fjsz.credit.list.html',NULL,'4091','1','1','1b06c07fa30a471ea3c554c86f0c814b','同步信用分');

/*更新投标文件存放目录验证规则*/
UPDATE T_SYS_PARAM SET PARAM_RULE = '.*' WHERE PARAM_ID='6439f1b86a1711e8be47106530297fc2';