DROP TABLE IF EXISTS  EKB_T_COMPANY_CREDIT_SCORE_INFO;
DROP TABLE IF EXISTS  EKB_T_INDUSTRY_AVG_CREDIT_SCORE_INFO;

CREATE TABLE EKB_T_COMPANY_CREDIT_SCORE_INFO
(
        ID              VARCHAR(32) NOT NULL COMMENT 'ID',
        V_COMPANY_NAME  VARCHAR(1000)        COMMENT '企业名称',
        V_ORG_CODE      VARCHAR(20)          COMMENT '组织机构代码',
        V_TYPE          VARCHAR(300)          COMMENT '分值类别',
        V_YEAR          VARCHAR(10)           COMMENT '年份',
        V_QUARTER       VARCHAR(100)          COMMENT '季度',
        N_RANKING        FLOAT                 COMMENT '综合排名',
        N_HTLY           FLOAT                 COMMENT '合同履约',
        N_ZLAQWM         FLOAT                 COMMENT '质量安全文明',
        N_TOTAL_SCORE    FLOAT                 COMMENT '总分',
        N_TCXW           FLOAT                 COMMENT '通常行为',
        N_CREATE_TIME        bigint comment '创建时间',
        PRIMARY KEY (ID)
);

ALTER TABLE EKB_T_COMPANY_CREDIT_SCORE_INFO COMMENT '企业季度信用分表';


CREATE TABLE EKB_T_INDUSTRY_AVG_CREDIT_SCORE_INFO
(
        ID              VARCHAR(32) NOT NULL COMMENT 'ID',
        V_TYPE          VARCHAR(300)          COMMENT '分值类别',
        V_YEAR          VARCHAR(10)           COMMENT '年份',
        V_QUARTER       VARCHAR(100)          COMMENT '季度',
        N_HTLY           FLOAT                 COMMENT '合同履约',
        N_ZLAQWM         FLOAT                 COMMENT '质量安全文明',
        N_TOTAL_SCORE    FLOAT                 COMMENT '总分',
        N_TCXW           FLOAT                 COMMENT '通常行为',
        N_CREATE_TIME        bigint comment '创建时间',
        PRIMARY KEY (ID)
);

ALTER TABLE EKB_T_INDUSTRY_AVG_CREDIT_SCORE_INFO COMMENT '行业季度平均分数表';