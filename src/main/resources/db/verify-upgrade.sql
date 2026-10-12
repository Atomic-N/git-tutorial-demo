USE ai_tef;

DELIMITER $$

DROP PROCEDURE IF EXISTS emit_check $$
CREATE PROCEDURE emit_check(IN p_check_name VARCHAR(64), IN p_sql TEXT, IN p_fallback TEXT)
BEGIN
  IF p_sql IS NULL OR p_sql = '' THEN
    SELECT p_check_name AS check_name, p_fallback AS result;
  ELSE
    SET @verify_sql = CONCAT(
      'SELECT ',
      QUOTE(p_check_name),
      ' AS check_name, CAST((',
      p_sql,
      ') AS CHAR) AS result'
    );
    PREPARE stmt FROM @verify_sql;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
  END IF;
END $$

DELIMITER ;

SELECT
  'missing_tables' AS check_name,
  COALESCE(GROUP_CONCAT(required.table_name ORDER BY required.table_name SEPARATOR ', '), 'OK') AS result
FROM (
  SELECT 'roles' AS table_name
  UNION ALL SELECT 'user_roles'
  UNION ALL SELECT 'category_moderators'
  UNION ALL SELECT 'tag_groups'
  UNION ALL SELECT 'user_preferences'
  UNION ALL SELECT 'badges'
  UNION ALL SELECT 'user_badges'
  UNION ALL SELECT 'uploaded_files'
  UNION ALL SELECT 'chat_channels'
  UNION ALL SELECT 'chat_messages'
  UNION ALL SELECT 'user_relationships'
  UNION ALL SELECT 'direct_message_threads'
  UNION ALL SELECT 'direct_message_participants'
  UNION ALL SELECT 'direct_messages'
  UNION ALL SELECT 'site_settings'
  UNION ALL SELECT 'audit_logs'
) required
LEFT JOIN information_schema.TABLES actual
  ON actual.TABLE_SCHEMA = DATABASE()
 AND actual.TABLE_NAME = required.table_name
WHERE actual.TABLE_NAME IS NULL;

SELECT
  'missing_columns' AS check_name,
  COALESCE(GROUP_CONCAT(required.column_ref ORDER BY required.column_ref SEPARATOR ', '), 'OK') AS result
FROM (
  SELECT 'tags.tag_group_id' AS column_ref, 'tags' AS table_name, 'tag_group_id' AS column_name
) required
LEFT JOIN information_schema.COLUMNS actual
  ON actual.TABLE_SCHEMA = DATABASE()
 AND actual.TABLE_NAME = required.table_name
 AND actual.COLUMN_NAME = required.column_name
WHERE actual.COLUMN_NAME IS NULL;

SELECT
  'missing_foreign_keys' AS check_name,
  COALESCE(GROUP_CONCAT(required.constraint_name ORDER BY required.constraint_name SEPARATOR ', '), 'OK') AS result
FROM (
  SELECT 'fk_tags_group' AS constraint_name, 'tags' AS table_name
) required
LEFT JOIN information_schema.TABLE_CONSTRAINTS actual
  ON actual.TABLE_SCHEMA = DATABASE()
 AND actual.TABLE_NAME = required.table_name
 AND actual.CONSTRAINT_NAME = required.constraint_name
 AND actual.CONSTRAINT_TYPE = 'FOREIGN KEY'
WHERE actual.CONSTRAINT_NAME IS NULL;

CALL emit_check(
  'invalid_tag_group_links',
  IF(
    EXISTS (
      SELECT 1 FROM information_schema.TABLES
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tag_groups'
    ),
    'SELECT COUNT(*) FROM tags t LEFT JOIN tag_groups tg ON tg.id = t.tag_group_id WHERE t.tag_group_id IS NOT NULL AND tg.id IS NULL',
    NULL
  ),
  'MISSING_TABLE: tag_groups'
);

CALL emit_check(
  'preference_rows',
  IF(
    EXISTS (
      SELECT 1 FROM information_schema.TABLES
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_preferences'
    ),
    'SELECT CONCAT((SELECT COUNT(*) FROM user_preferences), '' / '', (SELECT COUNT(*) FROM users))',
    NULL
  ),
  'MISSING_TABLE: user_preferences'
);

CALL emit_check(
  'site_settings',
  IF(
    EXISTS (
      SELECT 1 FROM information_schema.TABLES
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'site_settings'
    ),
    'SELECT COALESCE(GROUP_CONCAT(CONCAT(setting_key, ''='', setting_value) ORDER BY setting_key SEPARATOR ''; ''), ''EMPTY'') FROM site_settings',
    NULL
  ),
  'MISSING_TABLE: site_settings'
);

CALL emit_check(
  'latest_migration',
  IF(
    EXISTS (
      SELECT 1 FROM information_schema.TABLES
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'schema_migrations'
    ),
    'SELECT COALESCE(MAX(CONCAT(version, '' @ '', DATE_FORMAT(applied_at, ''%Y-%m-%d %H:%i:%s''))), ''EMPTY'') FROM schema_migrations',
    NULL
  ),
  'MISSING_TABLE: schema_migrations'
);

DROP PROCEDURE IF EXISTS emit_check;
