DROP TABLE IF EXISTS `blocks`;
CREATE TABLE `blocks` (
  `height` int(11) NOT NULL,
  `hash` varchar(255) DEFAULT NULL,
  `location` varchar(32) DEFAULT NULL,
  `dateEntered` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`height`)
);

DROP TABLE IF EXISTS `errorLog`;
CREATE TABLE `errorLog` (
  `rn` bigint(20) NOT NULL AUTO_INCREMENT,
  `userID` bigint(20) DEFAULT '0',
  `errorLocation` varchar(100) DEFAULT NULL,
  `errorNumber` int(11) DEFAULT '0',
  `errorMessage` varchar(500) DEFAULT NULL,
  `errorDetails` varchar(500) DEFAULT NULL,
  `dateEntered` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`rn`)
);

DROP TABLE IF EXISTS `main`;
CREATE TABLE `main` (
  `chainId` int(11) NOT NULL,
  `maxHeight` int(11) DEFAULT NULL,
  `blockHistory` int(11) DEFAULT NULL,
  `stateHistory` int(11) DEFAULT NULL,
  PRIMARY KEY (`chainId`)
);

DROP TABLE IF EXISTS `state`;
CREATE TABLE `state` (
  `height` int(11) NOT NULL,
  `hash` varchar(255) DEFAULT NULL,
  `location` varchar(32) DEFAULT NULL,
  `dateEntered` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`height`)
);
