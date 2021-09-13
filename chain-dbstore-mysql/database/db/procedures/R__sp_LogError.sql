DROP PROCEDURE IF EXISTS `sp_logError`;

DELIMITER $$

CREATE DEFINER=`vn_owner`@`localhost` PROCEDURE `sp_logError`(
	thisUserID integer,
	errorLocation varchar(100),
	errorNumber integer,
	errorMessageText  varchar(500),
	errorDetails  varchar(500)
)
BEGIN
	INSERT errorLog (
		`UserID`,
		`ErrorLocation`,
		`ErrorNumber`,
		`ErrorMessage`,
		`ErrorDetails`
  )
	VALUES (
		IFNULL(thisUserID, 0),
		IFNULL(errorLocation, ''),
		IFNULL(errorNumber, 0),
		IFNULL(errorMessageText, ''),
		IFNULL(errorDetails, '')
	);
END $$

DELIMITER ;
