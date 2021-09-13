DROP PROCEDURE IF EXISTS `sp_state_delete`;

DELIMITER $$

CREATE DEFINER=`vn_owner`@`localhost` PROCEDURE `sp_state_delete`(
	IN thisheight INT
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
				'sp_state_delete',
				errorNumber,
				errorMessageText,
				errorSQLState
		);

		RESIGNAL;
	END;

	DELETE FROM `state`
	WHERE `height` = thisHeight;
END $$

DELIMITER ;
