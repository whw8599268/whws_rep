/* 新增投标人球号表*/
CREATE TABLE EKB_T_BIDDER_NO
(
   ID                   	VARCHAR(32) NOT NULL COMMENT 'ID',
   V_TPID                   VARCHAR(32) NOT NULL COMMENT '招标项目ID',
   V_BIDDER_ID       		VARCHAR(32) NOT NULL COMMENT '投标人主键',
   V_BIDDER_NO     		    VARCHAR(20) NOT NULL COMMENT '投标人球号',
   N_CREATE_TIME     		BIGINT(20) NOT NULL COMMENT '创建时间',
   V_JSON_OBJ				LONGTEXT COMMENT '扩展信息',
   PRIMARY KEY (ID)
);
ALTER TABLE EKB_T_BIDDER_NO COMMENT '投标人球号表';

/* 投递标新增CPU序列号*/
ALTER TABLE EKB_T_TBIMPORTBIDDING ADD COLUMN V_CPUID VARCHAR(2000) DEFAULT NULL COMMENT 'CPU序列号' AFTER V_MAC;
