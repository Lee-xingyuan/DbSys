-- MySQL 8.0.30+；在 create.sql 建立全部业务表之后，由 root 执行。
-- 可用于首次初始化或修复已有账号；重复执行保留已有账号及密码。
-- 首次创建账号会返回随机密码，请保存到 IDEA 运行配置，不写入提交文件。
-- 本脚本只管理项目账号和授权，不修改业务数据。

CREATE ROLE IF NOT EXISTS 'cinema_admin'@'localhost';

CREATE USER IF NOT EXISTS 'cinema_app'@'localhost'
IDENTIFIED BY RANDOM PASSWORD;

-- 按具体表授予业务读写权限，避免数据库级授权中的下划线通配符匹配问题。
-- 这里的库名按普通标识符填写，不加反斜杠。新增表后需追加相应授权。
GRANT SELECT, INSERT, UPDATE, DELETE ON `cinema_design_demo`.`app_user` TO 'cinema_admin'@'localhost';
GRANT SELECT, INSERT, UPDATE, DELETE ON `cinema_design_demo`.`cinema` TO 'cinema_admin'@'localhost';
GRANT SELECT, INSERT, UPDATE, DELETE ON `cinema_design_demo`.`hall` TO 'cinema_admin'@'localhost';
GRANT SELECT, INSERT, UPDATE, DELETE ON `cinema_design_demo`.`seat` TO 'cinema_admin'@'localhost';
GRANT SELECT, INSERT, UPDATE, DELETE ON `cinema_design_demo`.`movie` TO 'cinema_admin'@'localhost';
GRANT SELECT, INSERT, UPDATE, DELETE ON `cinema_design_demo`.`screening` TO 'cinema_admin'@'localhost';
GRANT SELECT, INSERT, UPDATE, DELETE ON `cinema_design_demo`.`ticket_order` TO 'cinema_admin'@'localhost';
GRANT SELECT, INSERT, UPDATE, DELETE ON `cinema_design_demo`.`screening_seat` TO 'cinema_admin'@'localhost';
GRANT SELECT, INSERT, UPDATE, DELETE ON `cinema_design_demo`.`order_item` TO 'cinema_admin'@'localhost';
GRANT SELECT, INSERT, UPDATE, DELETE ON `cinema_design_demo`.`payment_attempt` TO 'cinema_admin'@'localhost';
GRANT SELECT, INSERT, UPDATE, DELETE ON `cinema_design_demo`.`refund_record` TO 'cinema_admin'@'localhost';
GRANT SELECT, INSERT, UPDATE, DELETE ON `cinema_design_demo`.`movie_review` TO 'cinema_admin'@'localhost';
GRANT SELECT, INSERT, UPDATE, DELETE ON `cinema_design_demo`.`audit_log` TO 'cinema_admin'@'localhost';

GRANT SELECT, INSERT, UPDATE, DELETE ON cinema_design_demo.login_guard TO 'cinema_admin'@'localhost';
GRANT SELECT, INSERT, UPDATE, DELETE ON cinema_design_demo.login_failure TO 'cinema_admin'@'localhost';


-- 收回旧版本在角色或应用账号上设置的数据库级授权。
-- 两种写法分别覆盖历史上的单次转义和重复转义；具体表权限已在上方授予。
-- IF EXISTS 让全新环境、已清理环境只产生提示，不因旧授权不存在而中断。
REVOKE IF EXISTS SELECT, INSERT, UPDATE, DELETE
ON `cinema\_design\_demo`.*
FROM 'cinema_admin'@'localhost', 'cinema_app'@'localhost';

REVOKE IF EXISTS SELECT, INSERT, UPDATE, DELETE
ON `cinema\\_design\\_demo`.*
FROM 'cinema_admin'@'localhost', 'cinema_app'@'localhost';

-- 将角色分配给应用账号，并在今后的每次连接中默认激活。
GRANT 'cinema_admin'@'localhost' TO 'cinema_app'@'localhost';
SET DEFAULT ROLE 'cinema_admin'@'localhost' TO 'cinema_app'@'localhost';


-- 查看角色与账号的授权情况；之后用 cinema_app 新连接验证 USE 和 SELECT。
SHOW GRANTS FOR 'cinema_admin'@'localhost';
SHOW GRANTS FOR 'cinema_app'@'localhost' USING 'cinema_admin'@'localhost';
