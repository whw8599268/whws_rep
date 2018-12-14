/**资源表**/
INSERT INTO T_SYS_RESOURCE_INFO (RESOURCE_ID, RESOURCE_NAME, RESOURCE_TYPE, RESOURCE_CODE, RESOURCE_URI, HTTP_METHOD, IS_AVAILABLE, RESOURCE_MEMO) VALUES ('1111d5ec4e634bb992f6d961c3c96352', '可视视图', 0, NULL, '/view/tsyj.html', 'SERVICE', 1, '可视视图');
INSERT INTO T_SYS_RESOURCE_INFO (RESOURCE_ID, RESOURCE_NAME, RESOURCE_TYPE, RESOURCE_CODE, RESOURCE_URI, HTTP_METHOD, IS_AVAILABLE, RESOURCE_MEMO) VALUES ('8888d5ec4e634bb992f6d961c3c35479', '运维人员登录页', 0, NULL, '/view/pm-login.html', 'SERVICE', 1, '运维人员登录页');

/**菜单表**/
TRUNCATE TABLE T_SYS_FUNC_INFO;
INSERT INTO T_SYS_FUNC_INFO VALUES ('3D5B3ABC1C2A4178A266E99227BBF532', '0001', '系统基础信息管理', '2', null, null, '10', '0', '1', null, '系统基础信息维护');
INSERT INTO T_SYS_FUNC_INFO VALUES ('5D5B36F91C2A4178A266E99227BBD4C4', '00010001', '用户基础信息管理', '2', 'view/manage/user/user.list.html', null, '1010', '1', '1', '3D5B3ABC1C2A4178A266E99227BBF532', '管理系统中的所有基本信息');
INSERT INTO T_SYS_FUNC_INFO VALUES ('912D235AEF0B44D7BD56B894E9D0DAC7', '00010002', '菜单管理', '2', 'view/manage/func/func.tree.list.html', null, '1020', '1', '1', '3D5B3ABC1C2A4178A266E99227BBF532', '管理系统中的所有菜单数据');
INSERT INTO T_SYS_FUNC_INFO VALUES ('4D5B36A93C2F4178A4S6E99227BCFDC9', '00010004', '系统资源管理', '2', 'view/manage/resource/resource.list.html', null, '1030', '1', '1', '3D5B3ABC1C2A4178A266E99227BBF532', '管理系统中的所有资源');
INSERT INTO T_SYS_FUNC_INFO VALUES ('6D5B36A91C2F4178A2SS6E99227BBDC9', '00010005', '角色管理', '2', 'view/manage/role/role.list.html', null, '1040', '1', '1', '3D5B3ABC1C2A4178A266E99227BBF532', '系统中的所有角色管理');
INSERT INTO T_SYS_FUNC_INFO VALUES ('dda7f6d9b21a4d9ea64d817167e49bdd', '00010006', '系统运行参数管理', '3', 'view/manage/param/param.list.html', null, '1050', '1', '1', '3D5B3ABC1C2A4178A266E99227BBF532', '系统运行参数的管理');
INSERT INTO T_SYS_FUNC_INFO VALUES ('d5166b59cc5d4dd1bfeb8b4fb5584dd5', '00010007', '数据字典管理', '2', 'view/manage/dict/dict.list.html', null, '1060', '1', '1', '3D5B3ABC1C2A4178A266E99227BBF532', '数据字典管理');
INSERT INTO T_SYS_FUNC_INFO VALUES ('26518a647ef14e078c0b2798b585ef35', '00010008', '当前机器信息', '3', 'view/manage/machine/machine.info.html', null, '10100', '1', '1', '3D5B3ABC1C2A4178A266E99227BBF532', '当前机器信息');
INSERT INTO T_SYS_FUNC_INFO VALUES ('12318a647ef19999990b2798b585e456', '00010010', '数据源监控', '3', 'view/manage/forward.html?t=0&f=/druid', null, '10120', '1', '1', '3D5B3ABC1C2A4178A266E99227BBF532', '数据源监控');

INSERT INTO T_SYS_FUNC_INFO VALUES ('1430b15b822e47bdae4bc72da5e1e0a7', '0002', '系统日志管理', '2', null, null, '20', '0', '1', null, '主要针系统的日志管理');
INSERT INTO T_SYS_FUNC_INFO VALUES ('1D5B36B93C2F4178A4S6E99257BCFDC9', '00020001', '系统日志级别管理', '2', 'view/manage/logger/logger.list.html', null, '2010', '1', '1', '1430b15b822e47bdae4bc72da5e1e0a7', '系统日志级别管理，主要管理Logback.xml中定义的Logger');
INSERT INTO T_SYS_FUNC_INFO VALUES ('0D5B36367C2F4178A4S6E99227BCFDC4', '00020002', '系统运行日志管理', '2', 'view/manage/syslog/syslog.list.html', null, '2020', '1', '1', '1430b15b822e47bdae4bc72da5e1e0a7', '系统运行日志管理');

INSERT INTO T_SYS_FUNC_INFO VALUES ('1b06c07fa30a471ea3c554c86f0c814b', '0004', '运维管理', '2', null, null, '40', '0', '1', null, null);
INSERT INTO T_SYS_FUNC_INFO VALUES ('b80589d9cffd41928991bf5c97df515e', '00040001', '（将废除）投标文件解压情况管理', '2', 'view/monitor/unpack.record.list.html', null, '4050', '1', '1', '1b06c07fa30a471ea3c554c86f0c814b', null);
INSERT INTO T_SYS_FUNC_INFO VALUES ('e7f654f50c8c4d80b6b19916d043096c', '00040003', '已开招标项目信息管理', '2', 'view/eokb/bus/project_tool/project.list.html', null, '4030', '1', '1', '1b06c07fa30a471ea3c554c86f0c814b', '查看投标人');
INSERT INTO T_SYS_FUNC_INFO VALUES ('2ab2320652ee464eaa6c38b1d5d2f21a', '00040004', '招标项目数据同步管理', '2', 'view/job/bid.project.list.html', null, '4020', '1', '1', '1b06c07fa30a471ea3c554c86f0c814b', null);
INSERT INTO T_SYS_FUNC_INFO VALUES ('02bb0cf2a849446080d619fcf7529e1d', '00040005', '（最新版本）投标文件解密解压情况管理', '2', 'view/eokb/bus/decrypt/manage/decrypt.status.list.html', null, '4040', '1', '1', '1b06c07fa30a471ea3c554c86f0c814b', null);
INSERT INTO T_SYS_FUNC_INFO VALUES ('2be87d3ff9624f2cb7b9e9ed89ba840f', '00040006', '查看Tomcat日志文件', '2', '/view/pm/logs/log.file.list.html', null, '4050', '1', '1', '1b06c07fa30a471ea3c554c86f0c814b', null);
INSERT INTO T_SYS_FUNC_INFO VALUES ('96518a647ef19999990b2798b585ef39', '00040007', '用户信息管理', '2', 'view/eokb/bus/cert_user/cert.user.list.html', null, '4010', '1', '1', '1b06c07fa30a471ea3c554c86f0c814b', '证书用户信息');
INSERT INTO T_SYS_FUNC_INFO VALUES ('25499689eba044e5ac620aa9fd8cf0f8', '00040008', '开标会议室管理', '2', 'view/im/room.colse.list.html', null, '4070', '1', '1', '1b06c07fa30a471ea3c554c86f0c814b', null);
INSERT INTO T_SYS_FUNC_INFO VALUES ('d04a19f2896b497abd51ce1c60742bfe', '00040009', '开标操作日志查看', '2', 'view/eokb/bus/bidlog/bid.operation.log.html', null, '4090', '1', '1', '1b06c07fa30a471ea3c554c86f0c814b', '开标操作日志查看');

INSERT INTO T_SYS_FUNC_INFO VALUES ('a2e300dcf06b433ab259eba71819a04b', '0005', '开标流程管理', '2', null, null, '30', '0', '1', null, '流程管理');
INSERT INTO T_SYS_FUNC_INFO VALUES ('ebad86ea41234374bff08f494f3cd566', '00050001', '开标流程管理', '2', 'view/flow/flow.list.html', null, '3010', '1', '1', 'a2e300dcf06b433ab259eba71819a04b', '开标流程管理');
INSERT INTO T_SYS_FUNC_INFO VALUES ('9e3c95643a8649d0aea0ee2915848697', '00050002', '修改流程状态', '2', 'view/eokb/bus/nodetool/node.tool.html', null, '3020', '1', '1', 'a2e300dcf06b433ab259eba71819a04b', '修改流程状态');

INSERT INTO T_SYS_FUNC_INFO VALUES ('9944d7cadd9a433ab84529bcbdd60239', '0003', '修改密码', '1', 'view/manage/user/modify.password.html', null, '50', '1', '1', null, '修改当前用户密码');

/**角色表**/
INSERT INTO T_SYS_ROLE_INFO VALUES ('62f1d3c2847243e8bb0dbe26a4006f1b', '运维管理员', '运维管理员', '3', '运维管理员');

/**角色菜单表**/
INSERT INTO T_SYS_ROLE_FUNC VALUES ('62f1d3c2847243e8bb0dbe26a4006f1b', '02bb0cf2a849446080d619fcf7529e1d');
INSERT INTO T_SYS_ROLE_FUNC VALUES ('62f1d3c2847243e8bb0dbe26a4006f1b', '0D5B36367C2F4178A4S6E99227BCFDC4');
INSERT INTO T_SYS_ROLE_FUNC VALUES ('62f1d3c2847243e8bb0dbe26a4006f1b', '1430b15b822e47bdae4bc72da5e1e0a7');
INSERT INTO T_SYS_ROLE_FUNC VALUES ('62f1d3c2847243e8bb0dbe26a4006f1b', '1b06c07fa30a471ea3c554c86f0c814b');
INSERT INTO T_SYS_ROLE_FUNC VALUES ('62f1d3c2847243e8bb0dbe26a4006f1b', '1D5B36B93C2F4178A4S6E99257BCFDC9');
INSERT INTO T_SYS_ROLE_FUNC VALUES ('62f1d3c2847243e8bb0dbe26a4006f1b', '25499689eba044e5ac620aa9fd8cf0f8');
INSERT INTO T_SYS_ROLE_FUNC VALUES ('62f1d3c2847243e8bb0dbe26a4006f1b', '2ab2320652ee464eaa6c38b1d5d2f21a');
INSERT INTO T_SYS_ROLE_FUNC VALUES ('62f1d3c2847243e8bb0dbe26a4006f1b', '2be87d3ff9624f2cb7b9e9ed89ba840f');
INSERT INTO T_SYS_ROLE_FUNC VALUES ('62f1d3c2847243e8bb0dbe26a4006f1b', '96518a647ef19999990b2798b585ef39');
INSERT INTO T_SYS_ROLE_FUNC VALUES ('62f1d3c2847243e8bb0dbe26a4006f1b', 'b80589d9cffd41928991bf5c97df515e');
INSERT INTO T_SYS_ROLE_FUNC VALUES ('62f1d3c2847243e8bb0dbe26a4006f1b', 'e7f654f50c8c4d80b6b19916d043096c');

/**系统运行参数表**/
INSERT INTO T_SYS_PARAM (PARAM_ID, PARAM_KEY, PARAM_VALUE, PARAM_RULE, PARAM_TIP, PARAM_DEMO) VALUES('0000d5ec4e634bb992f6d961c3c39999','aeolus.eokb.pm.admin.role.id','62f1d3c2847243e8bb0dbe26a4006f1b','.+','运维管理员角色ID,不允许为空!','运维管理员角色ID');


