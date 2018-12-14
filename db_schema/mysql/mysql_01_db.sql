/**创建主数据库**/  
CREATE DATABASE IF NOT EXISTS DB_EBID_EOKB;  
USE DB_EBID_EOKB;  
/**--修改默认字符集**/  
ALTER DATABASE DEFAULT CHARSET=UTF8;  

/**创建用户并赋予用户DB_EBID_ONLINE_KB数据库所有权限**/
GRANT ALL PRIVILEGES ON DB_EBID_EOKB.* TO DB_EBID_EOKB@"%" IDENTIFIED BY "DB_EBID_EOKB";
GRANT ALL PRIVILEGES ON DB_EBID_EOKB.* TO DB_EBID_EOKB@"localhost" IDENTIFIED BY "DB_EBID_EOKB";
GRANT ALL PRIVILEGES ON DB_EBID_EOKB.* TO DB_EBID_EOKB@"127.0.0.1" IDENTIFIED BY "DB_EBID_EOKB";
FLUSH PRIVILEGES;

/**创建日志数据库**/  
CREATE DATABASE IF NOT EXISTS DB_EBID_EOKB_LOG;  
USE DB_EBID_EOKB_LOG;  
/**--修改默认字符集**/  
ALTER DATABASE DEFAULT CHARSET=UTF8;  

/**创建用户并赋予用户DB_AEOLUS_OA_LOG数据库所有权限**/
GRANT ALL PRIVILEGES ON DB_EBID_EOKB_LOG.* TO DB_EBID_EOKB_LOG@"%" IDENTIFIED BY "DB_EBID_EOKB_LOG";
GRANT ALL PRIVILEGES ON DB_EBID_EOKB_LOG.* TO DB_EBID_EOKB_LOG@"localhost" IDENTIFIED BY "DB_EBID_EOKB_LOG";
GRANT ALL PRIVILEGES ON DB_EBID_EOKB_LOG.* TO DB_EBID_EOKB_LOG@"127.0.0.1" IDENTIFIED BY "DB_EBID_EOKB_LOG";
FLUSH PRIVILEGES;


/**创建用户并赋予用户DB_EBID_KP数据库所有权限**/
GRANT ALL PRIVILEGES ON DB_EBID_EOPB.* TO DB_EBID_EOKB@"%" IDENTIFIED BY "DB_EBID_EOKB";
GRANT ALL PRIVILEGES ON DB_EBID_EOPB.* TO DB_EBID_EOKB@"localhost" IDENTIFIED BY "DB_EBID_EOKB";
GRANT ALL PRIVILEGES ON DB_EBID_EOPB.* TO DB_EBID_EOKB@"127.0.0.1" IDENTIFIED BY "DB_EBID_EOKB";
FLUSH PRIVILEGES;
