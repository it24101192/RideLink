-- Run against ridelink_account_db only for databases created with BINARY(36) UUIDs.
-- Preserve the first 16 UUID bytes and refuse to discard any nonzero suffix.
DROP PROCEDURE IF EXISTS ridelink_fix_account_uuid_storage;
DELIMITER //
CREATE PROCEDURE ridelink_fix_account_uuid_storage()
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'account'
          AND column_name = 'user_id' AND column_type IN ('binary(36)', 'varbinary(36)')
    ) THEN
        IF EXISTS (
            SELECT 1 FROM account WHERE user_id IS NOT NULL
              AND (OCTET_LENGTH(user_id) < 16 OR HEX(SUBSTRING(user_id, 17)) REGEXP '[1-9A-F]')
        ) THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'UUID column contains unexpected bytes; migration stopped without changing account data';
        END IF;
        ALTER TABLE account MODIFY COLUMN user_id VARBINARY(36);
        UPDATE account SET user_id = LEFT(user_id, 16) WHERE user_id IS NOT NULL;
        ALTER TABLE account MODIFY COLUMN user_id BINARY(16);
    END IF;
END//
DELIMITER ;
CALL ridelink_fix_account_uuid_storage();
DROP PROCEDURE ridelink_fix_account_uuid_storage;
