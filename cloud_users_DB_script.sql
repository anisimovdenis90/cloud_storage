DROP DATABASE IF EXISTS `cloud_users`;
CREATE DATABASE `cloud_users`;

DROP TABLE IF EXISTS `cloud_users`.`users`;
CREATE TABLE `cloud_users`.`users` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `nickname` VARCHAR(45) NOT NULL,
  `login` VARCHAR(45) NOT NULL,
  `password` VARCHAR(45) NOT NULL,
  `isLogin` BIT NOT NULL DEFAULT 0,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `nickname_UNIQUE` (`nickname` ASC) VISIBLE,
  UNIQUE INDEX `login_UNIQUE` (`login` ASC) VISIBLE);
  
INSERT INTO `cloud_users`.`users` (nickname, login, password) VALUES
('user1', 'user1', 'password1'),
('user2', 'user2', 'password2');