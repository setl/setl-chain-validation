DROP PROCEDURE IF EXISTS `sp_main_updateBlockHistory`;

DELIMITER $$

CREATE DEFINER=`vn_owner`@`localhost` PROCEDURE `sp_main_updateBlockHistory`(
	IN thisChainId INT,
	IN thisBlockHistory INT
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
				'sp_main_updateBlockHistory',
				errorNumber,
				errorMessageText,
				errorSQLState
		);

		RESIGNAL;
	END;

	UPDATE `main`
	SET `blockHistory` = thisBlockHistory
	WHERE `chainId` = thisChainId;
END $$

DELIMITER ;
