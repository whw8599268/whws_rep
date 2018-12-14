DROP TABLE IF EXISTS EKB_T_BIDDER_DOC_UNPACK_RECORD;

/*==============================================================*/
/* TABLE: EKB_T_BIDDER_DOC_UNPACK_RECORD                        */
/*==============================================================*/
CREATE TABLE EKB_T_BIDDER_DOC_UNPACK_RECORD
(
   ID                   VARCHAR(32) NOT NULL,
   V_TPID               VARCHAR(32) COMMENT '招标项目ID',
   V_TENDER_PROJECT_NAME VARCHAR(400) COMMENT '招标项目名称',
   V_TBID               VARCHAR(32) COMMENT '投递表ID',
   V_FTID               VARCHAR(32) COMMENT '交易平台FID',
   V_BIDDER_NAME        VARCHAR(200) COMMENT '投标人名称',
   V_ORG_CODE           VARCHAR(20) COMMENT '投标人组织机构代码',
   V_UNIFY_CODE         VARCHAR(20) COMMENT '投标人统一社会信用代码',
   V_ZIPX_FILE_PATH     VARCHAR(255) COMMENT 'ZIPX临时文件路径',
   V_TARGET_DIR_PATH    VARCHAR(255) COMMENT '解压路径',
   N_TIME_CONSUMING     BIGINT COMMENT '消耗时间',
   N_STATUS             TINYINT COMMENT '状态,0尚未开始,1解压成功,-1解压失败',
   N_CREATE_TIME        BIGINT COMMENT '创建时间',
   N_FINSH_TIME         BIGINT COMMENT '结束时间',
   PRIMARY KEY (ID)
);

ALTER TABLE EKB_T_BIDDER_DOC_UNPACK_RECORD COMMENT '三层解压记录表';


INSERT INTO T_SYS_PARAM (PARAM_ID, PARAM_KEY, PARAM_VALUE, PARAM_RULE, PARAM_TIP, PARAM_DEMO) VALUES ('99999999999999999999999999999999', 'aeolus.peration.maintenance.person.phones', '15060446663,15980654325', '.*', '运维人员电话号码列表', '运维人员电话号码列表,多个电话号码用,号隔开');
INSERT INTO T_SYS_PARAM (PARAM_ID, PARAM_KEY, PARAM_VALUE, PARAM_RULE, PARAM_TIP, PARAM_DEMO) VALUES ('88888888888888888888888888888888', 'aeolus.message.switch', 'true', 'true|false', '是否发送短息提示给运维人员', '是否发送短息提示给运维人员');

