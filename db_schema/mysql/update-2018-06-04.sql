/* 新增投标人联系表*/
CREATE TABLE EKB_T_BIDDER_PHONE
(
   ID                   	VARCHAR(32) NOT NULL COMMENT 'ID',
   V_TPID                   VARCHAR(32) NOT NULL COMMENT '招标项目ID',
   V_CA_ID       			VARCHAR(32) NOT NULL COMMENT 'CAID,T_CA_USER_INFO表的主键',
   V_ENTERPRIS_NAME       	VARCHAR(32) NOT NULL COMMENT '企业名称',
   V_BIDDER_NAME   			VARCHAR(100) NOT NULL COMMENT '联系人名称',
   V_BIDDER_PHONE     		VARCHAR(20) NOT NULL COMMENT '联系人电话',
   V_JSON_OBJ				LONGTEXT COMMENT '扩展信息',
   PRIMARY KEY (ID)
);
ALTER TABLE EKB_T_BIDDER_PHONE COMMENT '投标人联系电话';