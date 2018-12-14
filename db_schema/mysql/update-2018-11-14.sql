/* 新增证书信息表 OPEN ID*/
ALTER TABLE T_CERT_INFO ADD COLUMN V_OPEN_ID VARCHAR(32) DEFAULT NULL COMMENT '开放式授权返回的全局唯一标识' AFTER ID;
/* 新增证书用户信息表OPEN ID*/
ALTER TABLE T_CA_USER_INFO ADD COLUMN V_OPEN_ID VARCHAR(32) DEFAULT NULL COMMENT '开放式授权返回的全局唯一标识' AFTER ID;

/*==============================================================*/
/* VIEW: V_CERT_USER_INFO                                       */
/*==============================================================*/
CREATE OR REPLACE VIEW V_CERT_USER_INFO
    AS
    SELECT CERT.ID AS V_CERT_ID,
           CERT.V_OPEN_ID AS V_OPEN_CERT_ID,
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
           CA.V_OPEN_ID AS V_OPEN_USER_ID,
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