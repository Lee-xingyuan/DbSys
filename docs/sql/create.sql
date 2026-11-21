-- MySQL 8.0.16+；应用开发用建库建表脚本。
-- 与原建表与验证脚本使用同一表结构和库名，不包含演示交易。
-- 仅在不存在 cinema_design_demo 时执行一次；出现错误时停止并核对。
-- 本脚本不删除或重建已有数据库。

CREATE DATABASE cinema_design_demo CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

USE cinema_design_demo;

SET NAMES utf8mb4;

SET time_zone = '+08:00';

CREATE TABLE app_user (
  `user_id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `username` VARCHAR(32) NOT NULL,
  `password_hash` VARCHAR(255) NOT NULL,
  `role` ENUM('CUSTOMER','ADMIN') NOT NULL,
  `is_active` BOOLEAN NOT NULL,
  `registered_at` DATETIME NOT NULL,
  UNIQUE (username),
  CHECK (CHAR_LENGTH(username) BETWEEN 3 AND 32),
  CHECK (is_active IN (0,1))
) ENGINE=InnoDB;

CREATE TABLE cinema (
  `cinema_id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `cinema_name` VARCHAR(100) NOT NULL,
  `address` VARCHAR(255) NOT NULL,
  `contact_phone` VARCHAR(32) NOT NULL,
  `is_active` BOOLEAN NOT NULL,
  CHECK (is_active IN (0,1))
) ENGINE=InnoDB;

CREATE TABLE hall (
  `hall_id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `cinema_id` BIGINT NOT NULL,
  `hall_name` VARCHAR(64) NOT NULL,
  `is_active` BOOLEAN NOT NULL,
  UNIQUE (cinema_id,hall_name),
  FOREIGN KEY (cinema_id) REFERENCES cinema(cinema_id),
  CHECK (is_active IN (0,1))
) ENGINE=InnoDB;

CREATE TABLE seat (
  `seat_id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `hall_id` BIGINT NOT NULL,
  `row_number` SMALLINT NOT NULL,
  `seat_number` SMALLINT NOT NULL,
  `is_active` BOOLEAN NOT NULL,
  UNIQUE (hall_id,`row_number`,seat_number),
  CHECK (`row_number`>0 AND seat_number>0),
  FOREIGN KEY (hall_id) REFERENCES hall(hall_id),
  CHECK (is_active IN (0,1))
) ENGINE=InnoDB;

CREATE TABLE movie (
  `movie_id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `title` VARCHAR(120) NOT NULL,
  `synopsis` TEXT NOT NULL,
  `genre` VARCHAR(40) NOT NULL,
  `duration_minutes` SMALLINT NOT NULL,
  `poster_url` VARCHAR(500) NULL,
  `listing_status` ENUM('DRAFT','LISTED','UNLISTED') NOT NULL,
  CHECK (duration_minutes>0),
  CHECK (listing_status <> 'LISTED' OR (CHAR_LENGTH(TRIM(title))>0 AND CHAR_LENGTH(TRIM(synopsis))>0 AND CHAR_LENGTH(TRIM(genre))>0))
) ENGINE=InnoDB;

CREATE TABLE screening (
  `screening_id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `movie_id` BIGINT NOT NULL,
  `hall_id` BIGINT NOT NULL,
  `starts_at` DATETIME NOT NULL,
  `ends_at` DATETIME NOT NULL,
  `ticket_price` DECIMAL(10,2) NOT NULL,
  `screening_status` ENUM('SCHEDULED','CANCELLED') NOT NULL,
  `revision` INT NOT NULL,
  INDEX ix_hall_start (hall_id,starts_at),
  INDEX ix_movie_start (movie_id,starts_at),
  CHECK (ends_at>starts_at AND ticket_price BETWEEN 0.01 AND 9999.99 AND revision>0),
  FOREIGN KEY (movie_id) REFERENCES movie(movie_id),
  FOREIGN KEY (hall_id) REFERENCES hall(hall_id)
) ENGINE=InnoDB;

CREATE TABLE ticket_order (
  `order_id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `user_id` BIGINT NOT NULL,
  `screening_id` BIGINT NOT NULL,
  `request_key` CHAR(36) NOT NULL,
  `request_digest` CHAR(64) NOT NULL,
  `order_status` ENUM('PENDING','PAID','CANCELLED','EXPIRED','REFUNDED') NOT NULL,
  `total_amount` DECIMAL(10,2) NOT NULL,
  `created_at` DATETIME NOT NULL,
  `expires_at` DATETIME NOT NULL,
  `paid_at` DATETIME NULL,
  `closed_at` DATETIME NULL,
  `movie_title_snapshot` VARCHAR(120) NOT NULL,
  `cinema_name_snapshot` VARCHAR(100) NOT NULL,
  `hall_name_snapshot` VARCHAR(64) NOT NULL,
  `start_time_snapshot` DATETIME NOT NULL,
  `end_time_snapshot` DATETIME NOT NULL,
  UNIQUE (user_id,request_key),
  INDEX ix_order_user (user_id,created_at),
  INDEX ix_expiry (order_status,expires_at),
  CHECK (total_amount>0 AND expires_at>created_at),
  CHECK ((order_status IN ('PAID','REFUNDED'))=(paid_at IS NOT NULL)),
  CHECK ((order_status IN ('CANCELLED','EXPIRED'))=(closed_at IS NOT NULL)),
  FOREIGN KEY (user_id) REFERENCES app_user(user_id),
  FOREIGN KEY (screening_id) REFERENCES screening(screening_id)
) ENGINE=InnoDB;

CREATE TABLE screening_seat (
  `screening_seat_id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `screening_id` BIGINT NOT NULL,
  `seat_id` BIGINT NOT NULL,
  `inventory_status` ENUM('AVAILABLE','HELD','SOLD','DISABLED') NOT NULL,
  `current_order_id` BIGINT NULL,
  UNIQUE (screening_id,seat_id),
  CHECK ((inventory_status IN ('HELD','SOLD'))=(current_order_id IS NOT NULL)),
  FOREIGN KEY (screening_id) REFERENCES screening(screening_id),
  FOREIGN KEY (seat_id) REFERENCES seat(seat_id),
  FOREIGN KEY (current_order_id) REFERENCES ticket_order(order_id)
) ENGINE=InnoDB;

CREATE TABLE order_item (
  `order_item_id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `order_id` BIGINT NOT NULL,
  `screening_seat_id` BIGINT NOT NULL,
  `seat_label_snapshot` VARCHAR(40) NOT NULL,
  `ticket_price` DECIMAL(10,2) NOT NULL,
  UNIQUE (order_id,screening_seat_id),
  CHECK (ticket_price>0),
  FOREIGN KEY (order_id) REFERENCES ticket_order(order_id),
  FOREIGN KEY (screening_seat_id) REFERENCES screening_seat(screening_seat_id)
) ENGINE=InnoDB;

CREATE TABLE payment_attempt (
  `payment_attempt_id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `order_id` BIGINT NOT NULL,
  `request_key` CHAR(36) NOT NULL,
  `attempted_at` DATETIME NOT NULL,
  `attempt_amount` DECIMAL(10,2) NOT NULL,
  `payment_result` ENUM('SUCCESS','FAILURE') NOT NULL,
  UNIQUE (order_id,request_key),
  UNIQUE (payment_attempt_id,order_id),
  UNIQUE INDEX uq_one_success ((CASE WHEN payment_result='SUCCESS' THEN order_id ELSE NULL END)),
  INDEX ix_attempt_time (attempted_at,order_id),
  CHECK (attempt_amount>0),
  FOREIGN KEY (order_id) REFERENCES ticket_order(order_id)
) ENGINE=InnoDB;

CREATE TABLE refund_record (
  `refund_id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `payment_attempt_id` BIGINT NOT NULL,
  `order_id` BIGINT NOT NULL,
  `refund_amount` DECIMAL(10,2) NOT NULL,
  `refunded_at` DATETIME NOT NULL,
  UNIQUE (order_id),
  UNIQUE (payment_attempt_id),
  FOREIGN KEY (payment_attempt_id,order_id) REFERENCES payment_attempt(payment_attempt_id,order_id),
  INDEX ix_refund_time (refunded_at,order_id),
  CHECK (refund_amount>0),
  FOREIGN KEY (payment_attempt_id) REFERENCES payment_attempt(payment_attempt_id),
  FOREIGN KEY (order_id) REFERENCES ticket_order(order_id)
) ENGINE=InnoDB;

CREATE TABLE movie_review (
  `review_id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `user_id` BIGINT NOT NULL,
  `movie_id` BIGINT NOT NULL,
  `qualifying_order_id` BIGINT NOT NULL,
  `rating` TINYINT NOT NULL,
  `body` VARCHAR(500) NOT NULL,
  `published_at` DATETIME NOT NULL,
  `updated_at` DATETIME NOT NULL,
  `visibility_status` ENUM('PUBLIC','HIDDEN') NOT NULL,
  UNIQUE (user_id,movie_id),
  CHECK (rating BETWEEN 1 AND 5 AND CHAR_LENGTH(TRIM(body)) BETWEEN 1 AND 500),
  INDEX ix_review_public (movie_id,visibility_status,published_at),
  FOREIGN KEY (user_id) REFERENCES app_user(user_id),
  FOREIGN KEY (movie_id) REFERENCES movie(movie_id),
  FOREIGN KEY (qualifying_order_id) REFERENCES ticket_order(order_id)
) ENGINE=InnoDB;

CREATE TABLE audit_log (
  `audit_log_id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `actor_user_id` BIGINT NULL,
  `action_type` VARCHAR(48) NOT NULL,
  `target_type` VARCHAR(40) NOT NULL,
  `target_id` BIGINT NOT NULL,
  `occurred_at` DATETIME NOT NULL,
  `request_id` VARCHAR(64) NOT NULL,
  `change_summary` VARCHAR(500) NOT NULL,
  `action_reason` VARCHAR(255) NULL,
  INDEX ix_audit_actor (actor_user_id,occurred_at),
  INDEX ix_audit_action (action_type,occurred_at),
  FOREIGN KEY (actor_user_id) REFERENCES app_user(user_id)
) ENGINE=InnoDB;

