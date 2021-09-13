DROP PROCEDURE IF EXISTS `sp_blocks_getHash`;

DELIMITER $$

CREATE DEFINER=`vn_owner`@`localhost` PROCEDURE `sp_blocks_getHash`(
	IN thisheight INT,
	OUT thisHash VARCHAR(255)
)
BEGIN
	DECLARE errorNumber INT;
	DECLARE errorMessageText TEXT;
	DECLARE errorSQLState CHAR(5) DEFAULT '00000';

	DECLARE EXIT HANDLER FOR SQLEXCEPTION
	BEGIN
		GET DIAGNOSTICS CONDITION 1
		errorNumber = MYSQL_ERRNO,
		errorSQLState = RETURNED_SQLSTATE,
		errorMessageText = MESSAGE_TEXT;

		CALL sp_logError(
				0,
				'sp_blocks_getHash',
				errorNumber,
				errorMessageText,
				errorSQLState
		);

		RESIGNAL;
	END;

	SELECT `hash`
	INTO thisHash
	FROM `blocks`
	WHERE `height` = thisHeight;
END $$

DELIMITER ;
