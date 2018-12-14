DROP TABLE IF EXISTS EKB_T_IM_LOG;

DROP TABLE IF EXISTS EKB_T_IM_MSG;

DROP TABLE IF EXISTS EKB_T_IM_ROOM;

CREATE TABLE EKB_T_IM_LOG
(
   ID                   VARCHAR(32) NOT NULL COMMENT '主键',
   V_ROOM_ID            VARCHAR(32) NOT NULL COMMENT '聊天室ID',
   V_TITILE             VARCHAR(300) COMMENT '事件标题',
   V_RESULT             VARCHAR(300) COMMENT '事件备注',
   N_CRAET_TIME         BIGINT COMMENT '创建时间',
   V_CREATE_USER        VARCHAR(100) COMMENT '创建人',
   PRIMARY KEY (ID)
);

ALTER TABLE EKB_T_IM_LOG COMMENT '即时聊天日志表(开标特殊动作信息记录)';

CREATE TABLE EKB_T_IM_MSG
(
   ID                   VARCHAR(32) NOT NULL COMMENT '主键',
   V_ROOM_ID            VARCHAR(32) NOT NULL COMMENT '聊天室ID',
   V_SENDER_ID          VARCHAR(100) COMMENT '消息发送者ID,公告类型没有发送人ID',
   V_SENDER_NAME        VARCHAR(300) COMMENT '消息发送者姓名',
   V_SENDER_LOGO        VARCHAR(300) COMMENT '消息发送者LOGO',
   V_RECIPIENT_ID       VARCHAR(100) COMMENT '消息接受者ID,为空表示为所有人',
   V_RECIPIENT_NAME     VARCHAR(300) COMMENT '消息接受者姓名',
   V_MESSAGE_CONTENT    VARCHAR(4000) COMMENT '消息/命令内容',
   N_MESSAGE_TYPE       INT COMMENT '消息类型',
   N_SEND_TIME          BIGINT COMMENT '发送的时间戳',
   PRIMARY KEY (ID)
);

ALTER TABLE EKB_T_IM_MSG COMMENT '即时聊天信息表';

CREATE TABLE EKB_T_IM_ROOM
(
   ID                   VARCHAR(32) NOT NULL COMMENT '主键',
   V_TPID               VARCHAR(50) COMMENT '招标项目ID',
   V_ROOMER_ID          VARCHAR(100) COMMENT '室主ID(招标人ID)',
   V_OPEN_TIME          VARCHAR(20) COMMENT '开启时间',
   V_CLOSE_TIME         VARCHAR(20) COMMENT '关闭时间',
   N_CRAET_TIME         BIGINT COMMENT '创建时间',
   V_CREATE_USER        VARCHAR(100) COMMENT '创建人',
   PRIMARY KEY (ID)
);

ALTER TABLE EKB_T_IM_ROOM COMMENT '即时聊天室表';

INSERT INTO T_SYS_PARAM (PARAM_ID, PARAM_KEY, PARAM_VALUE, PARAM_RULE, PARAM_TIP, PARAM_DEMO)
VALUES ('11111111114fdftwjcdded2123412122', 'aeolus.flash.socket.security.policy.file.port', '843', '\\d*', 'flash socket 安全策略文件服务端口,不允许为空!', 'flash socket 安全策略文件服务端口');


