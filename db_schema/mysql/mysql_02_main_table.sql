/**创建平台所需表**/
DROP TABLE IF EXISTS  T_SYS_USER_BASE;
DROP TABLE IF EXISTS  T_SYS_FUNC_INFO;
DROP TABLE IF EXISTS  T_SYS_RESOURCE_INFO;
DROP TABLE IF EXISTS  T_SYS_ROLE_INFO;
DROP TABLE IF EXISTS  T_SYS_ROLE_FUNC;
DROP TABLE IF EXISTS  T_SYS_ROLE_RESOURCE;
DROP TABLE IF EXISTS  T_SYS_USER_FUNC;
DROP TABLE IF EXISTS  T_SYS_USER_RESOURCE;
DROP TABLE IF EXISTS  T_SYS_USER_ROLE;
DROP TABLE IF EXISTS  T_SYS_PARAM;
DROP TABLE IF EXISTS  T_SYS_DICT;

/*==============================================================*/
/* TABLE: T_SYS_PARAM                                           */
/*==============================================================*/
CREATE TABLE T_SYS_PARAM
(
   PARAM_ID             VARCHAR(32) NOT NULL COMMENT 'ID',
   PARAM_KEY            VARCHAR(100) NOT NULL COMMENT '参数KEY',
   PARAM_VALUE          VARCHAR(2000) COMMENT '参数VALUE',
   PARAM_RULE           VARCHAR(2000) COMMENT '参数校验规则,JAVA正则表达式',
   PARAM_TIP            VARCHAR(300)  COMMENT '验证失败时的提示信息',
   PARAM_DEMO           VARCHAR(4000) COMMENT '备注',
   PRIMARY KEY (PARAM_ID),
   UNIQUE KEY UK_PARAM_KEY (PARAM_KEY)
);

ALTER TABLE T_SYS_PARAM COMMENT '系统运行参数';

/*==============================================================*/
/* TABLE: T_SYS_DICT                                            */
/*==============================================================*/
CREATE TABLE T_SYS_DICT
(
   DICT_ID              VARCHAR(32) NOT NULL COMMENT 'ID',
   DICT_TYPE            INT NOT NULL COMMENT '数据字典类型',
   DICT_TEXT            VARCHAR(300) COMMENT '字典显示文本',
   LEVEL_CODE           VARCHAR(1000) NOT NULL COMMENT '数据字典隐藏层级编码,8位表示一层,例如:000000010000000100000001',
   PID                  VARCHAR(32) COMMENT '父ID,顶层为NULL',
   IS_READ_ONLY         TINYINT COMMENT '是否只读,0否,1是',
   ORDER_KEY            INT COMMENT '排序关键字',
   DICT_VALUE           VARCHAR(100) COMMENT '字典值',
   DICT_VALUE1          VARCHAR(100) COMMENT '字典值1',
   DICT_VALUE2          VARCHAR(100) COMMENT '字典值2',
   DICT_VALUE3          VARCHAR(100) COMMENT '字典值3',
   DICT_VALUE4          VARCHAR(100) COMMENT '字典值4',
   DICT_VALUE5          VARCHAR(100) COMMENT '字典值5',
   DICT_VALUE6          VARCHAR(100) COMMENT '字典值6',
   DICT_VALUE7          VARCHAR(100) COMMENT '字典值7',
   DICT_VALUE8          VARCHAR(100) COMMENT '字典值8',
   DICT_VALUE9          VARCHAR(100) COMMENT '字典值9',
   REMARK               VARCHAR(1000) COMMENT '备注',
   PRIMARY KEY (DICT_ID)
);

ALTER TABLE T_SYS_DICT COMMENT '数据字典表';

/**创建用户信息表**/
CREATE TABLE T_SYS_USER_BASE
(
   USER_ID                       VARCHAR(32)                NOT NULL           COMMENT '用户ID',
   USER_ACCOUNT         		 VARCHAR(100)				NOT NULL		   COMMENT '用户登录名',
   USER_NAME                     VARCHAR(200)                                  COMMENT '用户姓名',
   PASSWORD                      VARCHAR(50)                                   COMMENT '密码',
   IS_ADMIN                      TINYINT		            NOT NULL           COMMENT '是否为管理员,1是,0不是',
   IS_LOCK                       TINYINT		            NOT NULL           COMMENT '是否被冻结,1是,0不是',
   PRIMARY KEY (USER_ID),
   UNIQUE KEY UK_USER_ACCOUNT (USER_ACCOUNT) 
);
/**增加表注释**/    
ALTER TABLE T_SYS_USER_BASE COMMENT = '用户信息表';

/**创建菜单表**/
CREATE TABLE T_SYS_FUNC_INFO(
      FUNC_ID                  VARCHAR(32)          NOT NULL           COMMENT '菜单ID',
      FUNC_CODE                VARCHAR(100)         NOT NULL           COMMENT '菜单层级编码,4位表示一层,例如:000100010001',
      FUNC_NAME                VARCHAR(200)         NOT NULL           COMMENT '菜单名称',
      FUNC_TYPE                TINYINT              NOT NULL           COMMENT '菜单类型,0:任意访问(包括游客),1:表示登录可访问,2:授权可访问,3:超级管理员可访问',
      HTTP_URL                 VARCHAR(255)                            COMMENT '菜单HTTP URL',
      FUNC_ICON                VARCHAR(255)                            COMMENT '菜单图片',
      ORDER_KEY                INT                                     COMMENT '排序关键字',
      IS_LEAF                  TINYINT              NOT NULL           COMMENT '是否为叶子节点,1为是,0为不是',
      IS_AVAILABLE             TINYINT              NOT NULL           COMMENT '菜单是否可用,1为可用,0为不可用',
      PARENT_ID                VARCHAR(32)                             COMMENT '父菜单ID,顶层菜单为NULL',
      FUNC_MEMO 			   VARCHAR(1000)		          		   COMMENT '菜单备注',
      PRIMARY KEY (FUNC_ID),
      UNIQUE KEY UK_FUNC_CODE (FUNC_CODE) 
);
/**增加表注释**/    
ALTER TABLE T_SYS_FUNC_INFO COMMENT = '菜单信息表';  

/**创建资源表**/
CREATE TABLE T_SYS_RESOURCE_INFO(
      RESOURCE_ID              VARCHAR(32)          NOT NULL           COMMENT '资源ID',
      RESOURCE_NAME            VARCHAR(200)         NOT NULL           COMMENT '资源名称',
      RESOURCE_TYPE            TINYINT              NOT NULL           COMMENT '资源类型,0:任意访问(包括游客),1:表示登录可访问,2:授权可访问,3:超级管理员可访问',
      RESOURCE_CODE            VARCHAR(100)                            COMMENT '资源编码',
      RESOURCE_URI             VARCHAR(255)         NOT NULL           COMMENT '资源REST URI',
      HTTP_METHOD              VARCHAR(20)          NOT NULL           COMMENT 'HTTP请求方式,GET/PUT/DELETE/POST/HEAD/OPTIONS/SERVICE',
      IS_AVAILABLE             TINYINT              NOT NULL           COMMENT '资源是否可用,1为可用,0为不可用',
      RESOURCE_MEMO 		   VARCHAR(1000)		          		   COMMENT '资源备注',
      PRIMARY KEY (RESOURCE_ID)
);
/**增加表注释**/    
ALTER TABLE T_SYS_RESOURCE_INFO COMMENT = '资源信息表';  

/**创建角色信息表**/
CREATE TABLE T_SYS_ROLE_INFO(
      ROLE_ID                  VARCHAR(32)          NOT NULL           COMMENT '角色ID',
      ROLE_NAME                VARCHAR(200)         NOT NULL           COMMENT '角色名称',
      ALIAS_NAME               VARCHAR(200)                            COMMENT '角色别名',
      ORDER_KEY                INT                                     COMMENT '排序关键字',
      ROLE_MEMO                VARCHAR(1000)                           COMMENT '角色备注',
      PRIMARY KEY (ROLE_ID)
);
/**增加表注释**/    
ALTER TABLE T_SYS_ROLE_INFO COMMENT = '角色信息表'; 

/**创建角色菜单表**/
CREATE TABLE T_SYS_ROLE_FUNC(
      ROLE_ID                  VARCHAR(32)          NOT NULL           COMMENT '角色ID',
      FUNC_ID                  VARCHAR(32)          NOT NULL           COMMENT '菜单ID',
      PRIMARY KEY (ROLE_ID,FUNC_ID)
);
/**增加表注释**/    
ALTER TABLE T_SYS_ROLE_FUNC COMMENT = '角色菜单表'; 

/**创建角色资源表**/
CREATE TABLE T_SYS_ROLE_RESOURCE(
      ROLE_ID                  VARCHAR(32)          NOT NULL           COMMENT '角色ID',
      RESOURCE_ID              VARCHAR(32)          NOT NULL           COMMENT '资源ID',
      PRIMARY KEY (ROLE_ID,RESOURCE_ID)
);
/**增加表注释**/    
ALTER TABLE T_SYS_ROLE_RESOURCE COMMENT = '角色资源表'; 

/**创建用户菜单**/
CREATE TABLE T_SYS_USER_FUNC(
      USER_ID                  VARCHAR(32)          NOT NULL           COMMENT '用户ID',
      FUNC_ID                  VARCHAR(32)          NOT NULL           COMMENT '菜单ID',
      PRIMARY KEY (USER_ID,FUNC_ID)
);
/**增加表注释**/    
ALTER TABLE T_SYS_USER_FUNC COMMENT = '用户菜单表'; 

/**创建用户资源表**/
CREATE TABLE T_SYS_USER_RESOURCE(
      USER_ID                  VARCHAR(32)          NOT NULL           COMMENT '用户ID',
      RESOURCE_ID              VARCHAR(32)          NOT NULL           COMMENT '资源ID',
      PRIMARY KEY (USER_ID,RESOURCE_ID)
);
/**增加表注释**/    
ALTER TABLE T_SYS_USER_RESOURCE COMMENT = '用户资源表'; 

/**创建用户角色表**/
CREATE TABLE T_SYS_USER_ROLE(
      USER_ID                  VARCHAR(32)          NOT NULL           COMMENT '用户ID',
      ROLE_ID                  VARCHAR(32)          NOT NULL           COMMENT '角色ID',
      PRIMARY KEY (USER_ID,ROLE_ID)
);
/**增加表注释**/    
ALTER TABLE T_SYS_USER_ROLE COMMENT = '用户角色表'; 