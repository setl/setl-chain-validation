DROP PROCEDURE IF EXISTS `sp_main_init`;

DELIMITER $$

CREATE DEFINER=`vn_owner`@`localhost` PROCEDURE `sp_main_init`(
	IN thisChainId INT
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
				'sp_main_init',
				errorNumber,
				errorMessageText,
				errorSQLState
		);

		RESIGNAL;
	END;

	INSERT INTO `main` (
		`chainId`,
    `blockHistory`,
    `maxHeight`,
    `stateHistory`
	)
	VALUES (
		thisChainId,
		0,
		0,
		0
	);
END $$

DELIMITER ;
