SET NAMES utf8mb4;

SET @schema_name = DATABASE();

SET @add_role_column = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE `twsny_user` ADD COLUMN `role` varchar(255) DEFAULT NULL AFTER `user_type`',
    'SELECT ''twsny_user.role already exists'' AS message'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name
    AND TABLE_NAME = 'twsny_user'
    AND COLUMN_NAME = 'role'
);

PREPARE add_role_column_stmt FROM @add_role_column;
EXECUTE add_role_column_stmt;
DEALLOCATE PREPARE add_role_column_stmt;

UPDATE `twsny_user`
SET `role` = 'ADMIN'
WHERE `login` = 'admin'
  AND (`role` IS NULL OR `role` = '');

UPDATE `twsny_user`
SET `role` = 'USER'
WHERE `role` IS NULL
   OR `role` = '';
