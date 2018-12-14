/**投递数量接口**/
INSERT INTO T_SYS_PARAM (`PARAM_ID`, `PARAM_KEY`, `PARAM_VALUE`, `PARAM_RULE`, `PARAM_TIP`, `PARAM_DEMO`) 
VALUES('321d4ebd9db111e79535000c29188e1f','aeolus.ede.entbid.count.url','http://ebid-api.okap.com/authorize/api/ekb/tbcount',NULL,'','');

INSERT INTO T_SYS_PARAM (`PARAM_ID`, `PARAM_KEY`, `PARAM_VALUE`, `PARAM_RULE`, `PARAM_TIP`, `PARAM_DEMO`)
VALUES('51724ebd9db111e79535000c2918a012','aeolus.ede.entbid.count.xm.fjsz.url','http://192.168.10.15:7170/exchange/authorize/xmjy/ekb/tbcount',NULL,'','');

/**标段表新增新旧投递文件目录标识**/
ALTER TABLE EKB_T_SECTION_INFO ADD COLUMN N_TB_NEW_DIC INT(4) DEFAULT 0 COMMENT '0：招标编号+标段名称  1：标段编号';