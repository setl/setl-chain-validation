DROP PROCEDURE IF EXISTS `sp_main_updateMaxHeight`;

DELIMITER $$

CREATE DEFINER=`vn_owner`@`localhost` PROCEDURE `sp_main_updateMaxHeight`(
	IN thisChainId INT,
	IN thisMaxHeight INT
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
				'sp_main_updateMaxHeight',
				errorNumber,
				errorMessageText,
				errorSQLState
		);

		RESIGNAL;
	END;

	UPDATE `main`
	SET `maxHeight` = thisMaxHeight
	WHERE `chainId` = thisChainId;
END $$

DELIMITER ;
