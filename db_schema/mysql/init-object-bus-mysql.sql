drop table if exists EKB_T_SECTION_INFO;
drop table if exists EKB_T_TENDER_LIST;
drop table if exists EKB_T_TENDER_PROJECT_INFO;
drop table if exists EKB_T_DECRYPT_TEMP;
drop table if exists T_OPERATION_LOG;
drop table if exists EKB_T_BIDDER_DOC_UNPACK_RECORD;
DROP TABLE IF EXISTS EKB_T_ELECTRONICS;

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


/*==============================================================*/
/* Table: T_OPERATION_LOG                                    */
/*==============================================================*/
create table T_OPERATION_LOG
(
   ID                   varchar(32) not null comment 'ID',
   V_CALL_METHOD_NAME   varchar(300) not null comment '方法名称',
   V_CALL_METHOD_DESC   varchar(300) comment '方法描述',
   V_CALL_PARAMS        longtext comment '方法参数',
   V_OPERATOR_ID        varchar(32) comment '操作人',
   N_OPERAT_TIME        bigint comment '操作时间',
   primary key (ID)
) ENGINE=MyISAM DEFAULT CHARSET=utf8 COMMENT='操作日志表';

/*==============================================================*/
/* Table: EKB_T_DECRYPT_TEMP                                    */
/*==============================================================*/
create table EKB_T_DECRYPT_TEMP
(
   ID                   varchar(32) not null comment 'ID',
   V_TPID               varchar(32) comment '开标库中的招标项目表主键',
   V_TBID               varchar(32)          comment '投递表ID',
   V_FTID               varchar(75)          comment '文件表ID',
   V_BEM_CODE           varchar(100)         comment '评标办法编码',
   V_SOURCE_PATH        varchar(765) comment '源文件路径',
   V_TEMP_DIR_PATH      varchar(765) comment '临时文件路径',
   V_TARGET_DIR_PATH    varchar(765) comment '解密解压路径',
   V_EFB_FILE_PATH      varchar(765) comment '加密块文件路径',
   V_CIPHERTEXT_JSON    longtext comment '密文JSON',
   N_CREATE_TIME        bigint comment '创建时间',
   V_CREATE_USER        varchar(32) comment '创建人',
   N_STATUS             tinyint comment '解密状态(1解密成功,0解析成功)',
   primary key (ID)
);

alter table EKB_T_DECRYPT_TEMP comment '解密信息临时表';


/*==============================================================*/
/* Table: EKB_T_SECTION_INFO                                    */
/*==============================================================*/
create table EKB_T_SECTION_INFO
(
   ID                   VARCHAR(32) not null,
   V_TPID               varchar(32) comment '开标库中的招标项目表主键',
   V_BID_SECTION_ID     VARCHAR(32) comment '标段ID',
   V_BID_SECTION_CODE   VARCHAR(100) comment '标段编号',
   V_BID_SECTION_GROUP_CODE VARCHAR(100) comment '标段组编号',
   V_BID_SECTION_NAME   VARCHAR(500) comment '标段名称',
   N_BIDDER_NUMBER      INT comment '投标人数量',
   N_EVALUATION_PRICE   double(19,4) comment '评标基准价',
   V_BID_OPEN_STATUS    VARCHAR(10) comment '开标状态0:未开标,1-1:第一信封开标中,1-2:第二信封开标中,2-1:第一信封开标完成  2:开标完成,10-0:流标(启动流标)10-1:流标(解密后流标)10-2:流标(第二信封流标)依次类推,第x信封等等即10-x',
   V_BID_EVALUATION_STATUS VARCHAR(10) comment '评标状态0:未评标,2-1:第一信封评标完成,2:评标完成,10:评标',
   V_CREATE_USER        varchar(32) comment '创建人',
   N_CREATE_TIME        bigint comment '创建时间',
   V_UPDATE_USER        varchar(32) comment '修改人',
   N_UPDATE_TIME        bigint comment '修改时间',
   primary key (ID)
);

alter table EKB_T_SECTION_INFO comment '标段信息表';

/*==============================================================*/
/* Table: EKB_T_TENDER_LIST                                     */
/*==============================================================*/
create table EKB_T_TENDER_LIST
(
   ID                   VARCHAR(32) not null comment 'ID',
   V_TPID               varchar(32) comment '开标库中的招标项目表主键',
   N_BIDDER_NUMBER      int(11) DEFAULT NULL COMMENT '投标人编号',
   V_BID_SECTION_ID     varchar(32) comment '标段ID',
   V_BID_SECTION_CODE   varchar(100) comment '标段编号',
   V_BID_SECTION_GROUP_CODE varchar(100) comment '标段组编号',
   V_BIDDER_ORG_CODE    varchar(20) comment '投标人代码',
   V_BIDDER_NAME        varchar(200) comment '投标人名称',
   N_SORT_FILE_BID      int comment '递交文件顺序号',
   N_SORT_FILE_DECRYPT  int comment '解密文件顺序号',
   N_PRICE              double(19,4) comment '投标报价(元)',
   N_VOIDTAGTMP         tinyint comment '作废临时状态标识(1正常,0作废)',
   V_CREATE_USER        varchar(32) comment '创建人',
   N_CREATE_TIME        bigint comment '创建时间',
   V_UPDATE_USER        varchar(32) comment '修改人',
   N_UPDATE_TIME        bigint comment '修改时间',
   V_JSON_OBJ           longtext comment '其他信息',
   primary key (ID)
);

alter table EKB_T_TENDER_LIST comment '投标人信息表';

/*==============================================================*/
/* Table: EKB_T_TENDER_PROJECT_INFO                             */
/*==============================================================*/
create table EKB_T_TENDER_PROJECT_INFO
(
   ID                   VARCHAR(32) not null comment 'ID',
   V_TENDER_PROJECT_ID  VARCHAR(32) comment '交易平台招标项目表ID',
   V_TENDER_PROJECT_CODE VARCHAR(100) comment '招标项目编号',
   V_BID_SECTION_ID     VARCHAR(20) comment '对应的标段编号,房建市政专用',
   V_INVITENO           varchar(100) comment '招标编号',
   V_INVITENOTRUE       varchar(100) comment '实际的招标编号，无论何时，值为：招标编号',
   V_TENDER_PROJECT_NAME VARCHAR(400) comment '招标项目名称',
   V_BIDOPEN_TIME       VARCHAR(20) comment '开标时间',
   V_TENDERER_CODE      VARCHAR(50) comment '招标人组织机构代码',
   V_TENDER_AGENCY_CODE VARCHAR(50) comment '招标代理组织机构代码',
   V_ADDRESS            varchar(200) comment '开标地点',
   V_PROTYPE            VARCHAR(10) comment '招标类型',
   N_PRICEALERT         DOUBLE(19,2) comment '成本预警价',
   V_JSON_OBJ           longtext comment '其他信息,JSON信息',
   V_BEM_INFO_JSON      text comment '评标办法JSON信息,从招标文件中获取到的评标办法JSON对象',
   V_CREATE_USER        varchar(32) comment '创建人',
   N_CREATE_TIME        bigint comment '创建时间',
   V_UPDATE_USER        varchar(32) comment '修改人',
   N_UPDATE_TIME        bigint comment '修改时间',
   primary key (ID)
);

alter table EKB_T_TENDER_PROJECT_INFO comment '招标项目信息表';

/*==============================================================*/
/* Table: T_CA_USER_INFO                                        */
/*==============================================================*/
create table T_CA_USER_INFO
(
   ID                   varchar(32) not null comment 'ID',
   V_SYS_ID             varchar(100) comment '开放式授权返回的ID',
   V_NAME               varchar(300) comment '用户名称',
   V_USER_TYPE          varchar(6) comment '用户类型1企业2个人',
   V_SOCIALCREDIT_NO    varchar(50) comment '统一社会信用编码',
   V_REG_NO             varchar(50) comment '工商注册号',
   V_LEGAL_NO           varchar(50) comment '单位法人证书事证号',
   V_COMPANY_CODE       varchar(50) comment '组织机构代码',
   V_TAX_NO             varchar(50) comment '税务登记号',
   V_RENT_NO            varchar(50) comment '地税电脑编码',
   V_SOCIAL_NO          varchar(50) comment '社保代码号',
   V_OTHER_NO           varchar(50) comment '其他证件号码',
   V_LEGAL_NAME         varchar(300) comment '法定代表姓名',
   V_ID_NO              varchar(50) comment '企业的是经办身份证,个人为个人的身份证',
   V_UNI_TTEL           varchar(50) comment '单位电话',
   V_REG_ADDRESS        varchar(300) comment '登记住所',
   N_CREATE_TIME        bigint comment '插入时间',
   N_UPDATE_TIME        bigint comment '修改时间',
   primary key (ID)
);

alter table T_CA_USER_INFO comment 'CA用户信息表';

/*==============================================================*/
/* Table: T_CERT_INFO                                           */
/*==============================================================*/
create table T_CERT_INFO
(
   ID                   varchar(32) not null comment '关联用户基本信息表(T_SYS_USER_BASE)的USER_ID字段',
   V_CA_ID              varchar(100) comment '开放式授权返回的主键',
   V_SERIAL             varchar(50) comment '证书序列号',
   N_TYPE               int comment '证书类型,1软证,0介质证',
   V_START_TIME         varchar(50) comment '有效开始时间',
   V_END_TIME           varchar(50) comment '有效结束时间',
   V_CERT_BASE64        mediumtext comment '证书base64字符串',
   V_LOGIN_NAME         varchar(200) comment '登录名',
   V_KEY_NAME           varchar(200) comment 'key名',
   V_CA_USER_ID         varchar(32) comment '关联的CA用户ID',
   N_CREATE_TIME        bigint comment '插入时间',
   N_UPDATE_TIME        bigint comment '修改时间',
   primary key (ID)
);

alter table T_CERT_INFO comment 'CA证书信息表';

/*==============================================================*/
/* View: V_CERT_USER_INFO                                       */
/*==============================================================*/
create VIEW  V_CERT_USER_INFO
    as
    SELECT CERT.ID AS V_CERT_ID,
           CERT.V_CA_ID,
           CERT.V_SERIAL,
           CERT.N_TYPE,
           CERT.V_START_TIME,
           CERT.V_END_TIME,
           CERT.V_CERT_BASE64,
           CERT.V_LOGIN_NAME,
           CERT.V_KEY_NAME,
           CERT.V_CA_USER_ID,
           CERT.N_CREATE_TIME AS N_CERT_CT,
           CERT.N_UPDATE_TIME AS N_CERT_UT,
           CA.V_SYS_ID,
           CA.V_NAME,
           CA.V_USER_TYPE,
           CA.V_SOCIALCREDIT_NO,
           CA.V_REG_NO,
           CA.V_LEGAL_NO,
           CA.V_COMPANY_CODE,
           CA.V_TAX_NO,
           CA.V_RENT_NO,
           CA.V_SOCIAL_NO,
           CA.V_OTHER_NO,
           CA.V_LEGAL_NAME,
           CA.V_ID_NO,
           CA.V_UNI_TTEL,
           CA.V_REG_ADDRESS,
           CA.N_CREATE_TIME AS N_CA_CT,
           CA.N_UPDATE_TIME AS N_CA_UT
      FROM T_CERT_INFO CERT
      LEFT JOIN T_CA_USER_INFO CA ON (CERT.V_CA_USER_ID = CA.ID);
/**
 * 电子摇号顺序
 */
CREATE TABLE EKB_T_ELECTRONICS (
	ID VARCHAR (96),
	N_NUMBER BIGINT (20),
	V_TPID     VARCHAR(100),
	V_BID_SECTION_ID VARCHAR (100),
	V_BID_SECTION_CODE VARCHAR (100),
	V_BID_SECTION_NAME VARCHAR (100),
	V_BID_SECTION_GROUP_CODE VARCHAR (100),
	V_BIDDER_NAME VARCHAR (200),
	V_BIDDER_NO VARCHAR (200),
	V_BIDDER_NAME_RELATION VARCHAR (200),
	V_BIDDER_NO_RELATION VARCHAR (200),
	V_TYPE VARCHAR (10) COMMENT '1、非关联企业。2、关联企业第一次匹配。3、关联企业第二次匹配.4、关联企业编码为空',
	V_CORRELATE_CODE VARCHAR (200),
	PRIMARY KEY(ID)
); 
ALTER TABLE EKB_T_ELECTRONICS COMMENT = '电子摇号结果'; 

/**
 * 开标室信息表
 */
CREATE TABLE EKB_T_VIDEO_INFO
(
   ID                   VARCHAR(32) NOT NULL,
   V_TPID               VARCHAR(32) COMMENT '招标项目ID',
   V_ROOM_AREA          VARCHAR(300) COMMENT '开标室地址',
   V_ROOM_NAME          VARCHAR(300) COMMENT '开标室名称',
   PRIMARY KEY (ID)
);
ALTER TABLE EKB_T_VIDEO_INFO COMMENT = '开标室信息';
