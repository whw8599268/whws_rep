drop VIEW if exists EKB_V_DECRYPT_STATUS;
DROP TABLE IF EXISTS EKB_T_BDDU_RECORD;
/*==============================================================*/
/* TABLE: EKB_T_BIDDER_DOC_UNPACK_RECORD                        */
/*==============================================================*/
CREATE TABLE EKB_T_BDDU_RECORD
(
   ID                   VARCHAR(32) NOT NULL COMMENT '主键,保持和投递表主键一致',
   V_TPID               VARCHAR(32) COMMENT '招标项目ID',
   V_TBID               VARCHAR(32) COMMENT '投递表ID',
   V_FTID               VARCHAR(32) COMMENT '交易平台FID',
   V_BIDDER_NAME        VARCHAR(200) COMMENT '投标人名称',
   V_ORG_CODE           VARCHAR(20) COMMENT '投标人组织机构代码',
   V_UNIFY_CODE         VARCHAR(20) COMMENT '投标人统一社会信用代码',
   N_USE_CASE           INT COMMENT '投标文件类型,-3投标文件（采用预审）-2资格审查（采用预审）-1投标文件0第一数字信封1第二数字信封2第三数字信封',
   V_EFBX_FILE_PATH     VARCHAR(255) COMMENT 'EFBX临时文件路径',
   V_TARGET_DIR_PATH    VARCHAR(255) COMMENT '解压路径',
   N_ALGORITHM_TYPE     INT COMMENT '对称加密算法类型10,20,30',
   V_PWD                VARCHAR(100) COMMENT '对称加密密码明文',
   V_ELEMENT_JSON       LONGTEXT COMMENT '唱标要素JSON',
   V_FILE_INFO_JSON     LONGTEXT COMMENT '投标文件描述JSON',
   N_DECRYPT_CONSUMING  BIGINT COMMENT '解密消耗时间,单位毫秒',
   N_UNPACK_CONSUMING   BIGINT COMMENT '解压消耗时间,单位毫秒',
   N_STATUS             TINYINT COMMENT '状态,0未处理,1:密码解密成功,-1密码解密失败,2投标信息摘要解密成功,-2投标信息摘要解密失败,3投标文件解密成功,-3投标文件解密失败,4解压成功,-4解压失败',
   V_CREATE_USER        VARCHAR(32) comment '创建人',
   N_CREATE_TIME        BIGINT COMMENT '创建时间',
   PRIMARY KEY (ID)
);

ALTER TABLE EKB_T_BDDU_RECORD COMMENT '解密解压记录表';

/*==============================================================*/
/* View: EKB_V_DECRYPT_STATUS                                      */
/*==============================================================*/
CREATE VIEW  EKB_V_DECRYPT_STATUS
    AS
    SELECT TD.ID,
       TD.V_ORG_CODE,
       TD.V_UNIFY_CODE,
       TD.V_BIDDER_NAME,
       TD.V_FILESIZE,
       TD.V_TIPSNAME,
       TD.V_FILEADDR AS V_STBX_NAME,
       TD.V_FOLDERPATHALL AS V_STBX_PATH,
       TD.V_DELIVER_TIME,
       TD.V_DELIVER_NO,
       TP.V_TENDER_PROJECT_CODE,
       TP.V_INVITENO,
       TP.V_INVITENOTRUE,
       TP.V_TENDER_PROJECT_NAME,
       S.V_BID_SECTION_CODE,
       S.V_BID_SECTION_GROUP_CODE,
       S.V_BID_SECTION_NAME,
       S.N_BIDDER_NUMBER,
       TEMP.ID AS V_TEMP_ID,
       TEMP.V_TPID,
       TEMP.N_USE_CASE,
       TEMP.V_EFBX_FILE_PATH,
       TEMP.V_TARGET_DIR_PATH,
       TEMP.N_ALGORITHM_TYPE,
       TEMP.V_PWD,
       TEMP.V_ELEMENT_JSON,
       TEMP.V_FILE_INFO_JSON,
       TEMP.N_DECRYPT_CONSUMING,
       TEMP.N_UNPACK_CONSUMING,
       IFNULL(TEMP.N_STATUS, -99) AS N_STATUS,
       TEMP.N_CREATE_TIME
  FROM EKB_T_TBIMPORTBIDDING TD
  JOIN EKB_T_TENDER_PROJECT_INFO TP ON (TD.V_TPID = TP.ID)
  JOIN EKB_T_SECTION_INFO S ON (TD.V_BID_SECTION_ID = S.V_BID_SECTION_ID)
  LEFT JOIN EKB_T_BDDU_RECORD TEMP ON (TD.ID = TEMP.V_TBID);


ALTER TABLE EKB_T_DECRYPT_INFO ADD COLUMN V_UNIFY_CODE VARCHAR(20) NULL COMMENT '投标人统一社会信用代码' AFTER V_BID_SECTION_ID;

ALTER TABLE EKB_T_TENDER_LIST ADD COLUMN V_UNIFY_CODE VARCHAR(20) NULL COMMENT '投标人统一社会信用代码' AFTER V_BID_SECTION_GROUP_CODE;

ALTER TABLE EKB_T_COMPANY_CREDIT_SCORE_INFO ADD COLUMN V_UNIFY_CODE VARCHAR(20) NULL COMMENT '统一社会信用代码' AFTER V_COMPANY_NAME;

ALTER TABLE EKB_T_COMPANY_CREDIT_SCORE_INFO ADD COLUMN V_JSON_OBJ LONGTEXT NULL COMMENT 'JSON 扩展信息' AFTER N_TCXW;

ALTER TABLE EKB_T_INDUSTRY_AVG_CREDIT_SCORE_INFO ADD COLUMN V_JSON_OBJ LONGTEXT NULL COMMENT 'JSON 扩展信息' AFTER N_TCXW;

INSERT INTO T_SYS_PARAM (PARAM_ID, PARAM_KEY, PARAM_VALUE, PARAM_RULE, PARAM_TIP, PARAM_DEMO)
VALUES ('fffffffffffffff111110ed21234c6f5', 'aeolus.oauth2.rs.monitor.qr.code.url', 'http://ra-cloud.okap.com/service/qrcode/monitor/${id}', '.*', '二维码轮询地址不允许为空!', '二维码轮询地址');
INSERT INTO T_SYS_PARAM (PARAM_ID, PARAM_KEY, PARAM_VALUE, PARAM_RULE, PARAM_TIP, PARAM_DEMO)
VALUES ('fsxxxxfffffffzzzzz110ed21234c6f5', 'aeolus.oauth2.rs.generate.qr.code.url', 'http://ra-cloud.okap.com/service/qrcode/gener/${t}/${w}/${h}', '.*', '二维码生成地址不允许为空!', '二维码生成地址');


