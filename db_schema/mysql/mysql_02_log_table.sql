/**创建logback所需表**/
DROP TABLE IF EXISTS LOGGING_EVENT_PROPERTY;
DROP TABLE IF EXISTS LOGGING_EVENT_EXCEPTION;
DROP TABLE IF EXISTS LOGGING_EVENT;

CREATE TABLE LOGGING_EVENT 
(
    TIMESTMP                  BIGINT                                NOT NULL             COMMENT '日志写入时间',            
    FORMATTED_MESSAGE         TEXT                                  NOT NULL             COMMENT '日志详细信息',
    LOGGER_NAME               VARCHAR(254)                          NOT NULL             COMMENT 'logger名称',
    LEVEL_STRING              VARCHAR(254)                          NOT NULL             COMMENT '日志级别',
    THREAD_NAME               VARCHAR(254)                                               COMMENT '线程名称',
    REFERENCE_FLAG            SMALLINT                                                   COMMENT '引用标记(1:正常,2:异常码,3:代表异常)',
    ARG0                      VARCHAR(254)                                               COMMENT '参数一',
    ARG1                      VARCHAR(254)                                               COMMENT '参数二',
    ARG2                      VARCHAR(254)                                               COMMENT '参数三',
    ARG3                      VARCHAR(254)                                               COMMENT '参数四',
    CALLER_FILENAME           VARCHAR(254)                          NOT NULL             COMMENT '文件名称',
    CALLER_CLASS              VARCHAR(254)                          NOT NULL             COMMENT '类名',
    CALLER_METHOD             VARCHAR(254)                          NOT NULL             COMMENT '方法名称',
    CALLER_LINE               CHAR(4)                               NOT NULL             COMMENT '代码行数',
    EVENT_ID                  BIGINT                                NOT NULL             AUTO_INCREMENT        PRIMARY KEY     COMMENT '日志ID'
)ENGINE=MyISAM;
/**增加表注释**/    
ALTER TABLE LOGGING_EVENT COMMENT = 'Logback日志信息表'; 

CREATE TABLE LOGGING_EVENT_PROPERTY
(
    EVENT_ID	                   BIGINT              NOT NULL           COMMENT '日志ID',
    MAPPED_KEY                     VARCHAR(254)        NOT NULL           COMMENT '日志ID',
    MAPPED_VALUE                   TEXT                                   COMMENT '日志ID',
    PRIMARY KEY(EVENT_ID, MAPPED_KEY)
)ENGINE=MyISAM;
ALTER TABLE LOGGING_EVENT_PROPERTY COMMENT = 'Logback日志异常堆栈信息表'; 

CREATE TABLE LOGGING_EVENT_EXCEPTION
(
    EVENT_ID                       BIGINT              NOT NULL           COMMENT '日志ID',
    I                              SMALLINT            NOT NULL           COMMENT '异常堆栈行数',
    TRACE_LINE                     VARCHAR(254)        NOT NULL           COMMENT '当前行堆栈信息',
    PRIMARY KEY(EVENT_ID, I)
)ENGINE=MyISAM;
/**增加表注释**/    
ALTER TABLE LOGGING_EVENT_EXCEPTION COMMENT = 'Logback日志异常堆栈信息表'; 



