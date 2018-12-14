drop VIEW if exists EKB_V_TENDER_PROJECT_FLOW_INFO;
drop table if exists EKB_T_FLOW_INFO;
drop table if exists EKB_T_FLOW_NODE_INFO;
drop table if exists EKB_T_TENDER_PROJECT_FLOW_NODE_INFO;
drop table if exists EKB_T_TPFN_DATA_INFO;

/*==============================================================*/
/* Table: EKB_T_FLOW_INFO                                       */
/*==============================================================*/
create table EKB_T_FLOW_INFO
(
   ID                   varchar(32) not null comment 'ID',
   V_FLOW_CODE          varchar(100) not null comment '流程编号',
   V_FLOW_NAME          varchar(300) comment '流程名称',
   V_BEM_CODE           varchar(100) comment '评标办法编号',
   V_VERSION            varchar(50) comment '版本号',
   N_IS_SECTION_GROUP   tinyint comment '是否有标段组,0没有,1有',
   N_CREATE_TIME        bigint comment '创建时间',
   V_CREATE_USER        varchar(32) comment '创建人',
   primary key (ID)
);

alter table EKB_T_FLOW_INFO comment '开标流程信息表';

/*==============================================================*/
/* Table: EKB_T_FLOW_NODE_INFO                                  */
/*==============================================================*/
create table EKB_T_FLOW_NODE_INFO
(
   ID                   varchar(32) not null,
   V_FLOW_ID            varchar(32) not null comment '所属流程ID',
   V_NODE_NAME          varchar(300) comment '节点名称',
   V_NODE_ICON          varchar(255) comment '节点图标',
   N_INDEX              int comment '节点序号',
   N_DEPTH              int comment '节点深度',
   V_PID                varchar(32) comment '父节点ID,顶层节点为NULL',
   V_TENDERER_PAGE_URL  varchar(255) comment '招标人页面URL',
   N_TENDERER_IS_SHOW   tinyint comment '招标人是否显示节点,0不显示,1显示',
   V_BIDDER_PAGE_URL    varchar(255) comment '投标人页面URL',
   N_BIDDER_IS_SHOW     tinyint comment '投标人是否显示节点,0不显示,1显示',
   N_IS_START_UP_NODE   int comment '是否为启动节点,0不是,1是',
   N_CREATE_TIME        bigint comment '创建时间',
   V_CREATE_USER        varchar(32) comment '创建人',
   primary key (ID)
);

alter table EKB_T_FLOW_NODE_INFO comment '开标流程节点';

/*==============================================================*/
/* Table: EKB_T_TENDER_PROJECT_FLOW_NODE_INFO                   */
/*==============================================================*/
create table EKB_T_TENDER_PROJECT_FLOW_NODE_INFO
(
   ID                   varchar(32) not null comment 'ID',
   V_FLOW_NODE_ID       varchar(32) not null comment '流程节点ID',
   V_TPID               varchar(32) not null comment '招标项目ID',
   N_STATUS             int comment '节点状态',
   N_CREATE_TIME        bigint comment '创建时间',
   V_CREATE_USER        varchar(32) comment '创建人',
   primary key (ID)
);

alter table EKB_T_TENDER_PROJECT_FLOW_NODE_INFO comment '招标项目开标流程节点信息表';


create table EKB_T_TPFN_STATUS_TIME
(
   ID                   varchar(32) not null comment 'ID',
   N_STATUS             int comment '节点状态',
   N_START_TIME         bigint comment '开始时间',
   primary key (ID,N_STATUS)
);

alter table EKB_T_TPFN_STATUS_TIME comment '招标项目开标流程节点状态时间表';

/*==============================================================*/
/* Table: EKB_T_TPFN_DATA_INFO                                  */
/*==============================================================*/
create table EKB_T_TPFN_DATA_INFO
(
   ID                   varchar(32) not null comment 'ID',
   V_TPID               varchar(32) comment '招标项目ID',
   V_TPFN_ID            varchar(32) comment '招标项目流程节点ID',
   V_BUS_ID             varchar(32) comment '业务关联ID',
   V_BUS_FLAG_TYPE      varchar(100) comment '业务类型标记',
   V_JSON_OBJ           longtext comment 'JSON字符串',
   N_CREATE_TIME        bigint comment '创建时间',
   V_CREATE_USER        varchar(32) comment '创建人',
   primary key (ID)
);

alter table EKB_T_TPFN_DATA_INFO comment '招标项目流程节点数据表';

/*==============================================================*/
/* View: EKB_V_TENDER_PROJECT_FLOW_INFO                         */
/*==============================================================*/
create VIEW  EKB_V_TENDER_PROJECT_FLOW_INFO
    as
    
    SELECT FLOW.ID AS V_FLOW_ID,
           FLOW.V_FLOW_CODE,
           FLOW.V_FLOW_NAME,
           FLOW.V_BEM_CODE,
           FLOW.N_IS_SECTION_GROUP,
           NODE.ID AS V_NODE_ID,
           NODE.V_NODE_NAME,
           NODE.V_NODE_ICON,
           NODE.N_INDEX,
           NODE.N_DEPTH,
           NODE.V_PID,
           NODE.V_TENDERER_PAGE_URL,
           NODE.N_TENDERER_IS_SHOW,
           NODE.V_BIDDER_PAGE_URL,
           NODE.N_BIDDER_IS_SHOW,
           NODE.N_IS_START_UP_NODE,
           TP_NODE.ID,
           TP_NODE.V_TPID,
           TP_NODE.N_STATUS,
           TP_NODE.N_CREATE_TIME,
           TP_NODE.V_CREATE_USER
      FROM EKB_T_FLOW_INFO FLOW
      JOIN EKB_T_FLOW_NODE_INFO AS NODE ON (FLOW.ID = NODE.V_FLOW_ID)
      LEFT JOIN EKB_T_TENDER_PROJECT_FLOW_NODE_INFO TP_NODE ON (NODE.ID =
                                                               TP_NODE.V_FLOW_NODE_ID);

