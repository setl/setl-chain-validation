DROP PROCEDURE IF EXISTS sp_main_getStateHistory;

DELIMITER $$

CREATE DEFINER=`vn_owner`@`localhost` PROCEDURE sp_main_getStateHistory(
	IN thisChainId INT,
	OUT thisStateHistory INT
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
				'sp_main_getStateHistory',
				errorNumber,
				errorMessageText,
				errorSQLState
		);

		RESIGNAL;
	END;

	SELECT `stateHistory`
	INTO thisStateHistory
	FROM `main`
	WHERE  `chainId` = thisChainId;
END $$

DELIMITER ;
