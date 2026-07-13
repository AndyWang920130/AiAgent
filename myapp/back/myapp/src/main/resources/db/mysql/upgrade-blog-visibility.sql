SET NAMES utf8mb4;

SET @schema_name = DATABASE();

SET @add_visibility_column = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE `twsny_blog` ADD COLUMN `visibility` varchar(255) DEFAULT ''PUBLIC'' AFTER `status`',
    'SELECT ''twsny_blog.visibility already exists'' AS message'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name
    AND TABLE_NAME = 'twsny_blog'
    AND COLUMN_NAME = 'visibility'
);

PREPARE add_visibility_column_stmt FROM @add_visibility_column;
EXECUTE add_visibility_column_stmt;
DEALLOCATE PREPARE add_visibility_column_stmt;

UPDATE `twsny_blog`
SET `visibility` = 'PUBLIC'
WHERE `visibility` IS NULL
   OR `visibility` = ''
   OR `visibility` NOT IN ('PUBLIC', 'PRIVATE');

SET @add_visibility_index = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE `twsny_blog` ADD INDEX `idx_twsny_blog_visibility` (`visibility`)',
    'SELECT ''idx_twsny_blog_visibility already exists'' AS message'
  )
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = @schema_name
    AND TABLE_NAME = 'twsny_blog'
    AND INDEX_NAME = 'idx_twsny_blog_visibility'
);

PREPARE add_visibility_index_stmt FROM @add_visibility_index;
EXECUTE add_visibility_index_stmt;
DEALLOCATE PREPARE add_visibility_index_stmt;
