CREATE TABLE `users` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `login` VARCHAR(45) NOT NULL,
  `password` VARCHAR(100) NOT NULL,
  `isLogin` BIT NOT NULL DEFAULT 0,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `login_INDEX` USING BTREE (`login`),
  UNIQUE INDEX `login_UNIQUE` (`login`)
);

INSERT INTO `users` (login, password) VALUES
('user1', '$2a$10$QAAvr27w6iBfoKEWYQmNvuZBWaa8E4ltyMWrZxk95klp0hH52cYRC'),
('user2', '$2a$10$iUAzB.MAUJTMlCLvBNGfAevhjKEXrUitAFOJmQV0Qsw2pVefJLBpS');