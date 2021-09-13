DROP PROCEDURE IF EXISTS `sp_main_getChainId`;
DROP PROCEDURE IF EXISTS `sp_state_archived`;
DROP PROCEDURE IF EXISTS `sp_state_getExpired`;
DROP PROCEDURE IF EXISTS `sp_blocks_getExpired`;

DELIMITER $$

CREATE DEFINER=`vn_owner`@`localhost` PROCEDURE `sp_main_getChainId`(
	OUT thisChainId INT
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
					'sp_main_getChainId',
					errorNumber,
					errorMessageText,
					errorSQLState
			);

			RESIGNAL;
		END;

		SELECT `chainId`
		INTO thisChainId
		FROM `main`
		LIMIT 1;
	END $$

CREATE DEFINER=`vn_owner`@`localhost` PROCEDURE `sp_state_archived`(
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
					'sp_state_archived',
					errorNumber,
					errorMessageText,
					errorSQLState
			);

			RESIGNAL;
		END;

		UPDATE state
		SET location = 'ARCHIVED'
		WHERE k = thisheight;
	END $$

CREATE DEFINER=`vn_owner`@`localhost` PROCEDURE `sp_state_getExpired`(
	IN adate DATETIME,
	OUT thisHeight INT(11),
	OUT thisHash VARCHAR(255),
	OUT thisDateEntered DATETIME
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
					'sp_state_getExpired',
					errorNumber,
					errorMessageText,
					errorSQLState
			);

			RESIGNAL;
		END;

		SELECT height, hash, dateEntered
		INTO thisHeight, thisHash, thisDateEntered
		FROM state
		WHERE dateEntered < adate;
	END $$

CREATE DEFINER=`vn_owner`@`localhost` PROCEDURE `sp_blocks_getExpired`(
	IN adate DATETIME,
	OUT thisHeight INT(11),
	OUT thisHash VARCHAR(255),
	OUT thisDateEntered DATETIME
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
					'sp_block_getExpired',
					errorNumber,
					errorMessageText,
					errorSQLState
			);

			RESIGNAL;
		END;

		SELECT height, hash, dateEntered
		INTO thisHeight, thisHash, thisDateEntered
		FROM blocks
		WHERE dateEntered < adate;
	END $$

DELIMITER ;
