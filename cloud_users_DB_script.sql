CREATE TABLE `users` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `login` VARCHAR(45) NOT NULL,
  `password` VARCHAR(255) NOT NULL,
  `isLogin` BIT NOT NULL DEFAULT 0,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `login_UNIQUE` (`login`)
);

INSERT INTO `cloud_users`.`users` (login, password) VALUES
('user1', '$2a$10$QAAvr27w6iBfoKEWYQmNvuZBWaa8E4ltyMWrZxk95klp0hH52cYRC'),
('user2', '$2a$10$iUAzB.MAUJTMlCLvBNGfAevhjKEXrUitAFOJmQV0Qsw2pVefJLBpS');