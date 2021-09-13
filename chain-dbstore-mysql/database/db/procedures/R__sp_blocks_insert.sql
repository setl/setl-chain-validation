DROP PROCEDURE IF EXISTS `sp_blocks_insert`;

DELIMITER $$

CREATE DEFINER=`vn_owner`@`localhost` PROCEDURE `sp_blocks_insert`(
	IN thisheight INT,
	IN thisHash VARCHAR(255),
	IN thisDateEntered DATETIME
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
				'sp_blocks_insert',
				errorNumber,
				errorMessageText,
				errorSQLState
		);

		RESIGNAL;
	END;

	INSERT INTO `blocks` (
		`height`,
		`hash`,
		`dateEntered`
	)
	VALUES (
		thisHeight,
		thisHash,
		thisDateEntered
	);
END $$

DELIMITER ;
