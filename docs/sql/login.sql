USE cinema_design_demo;

CREATE TABLE login_guard (
    username VARCHAR(32) NOT NULL PRIMARY KEY,
    locked_until DATETIME NULL
) ENGINE=InnoDB;

CREATE TABLE login_failure (
    failure_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(32) NOT NULL,
    occurred_at DATETIME NOT NULL,
    INDEX ix_login_failure_user_time(username, occurred_at)
) ENGINE=InnoDB;