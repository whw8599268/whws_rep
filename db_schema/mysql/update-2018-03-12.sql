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

