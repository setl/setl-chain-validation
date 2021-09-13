CREATE USER IF NOT EXISTS 'wn_owner'@'localhost' IDENTIFIED BY 'T{01@0$Xw23!DkXn';

GRANT ALL ON blockchain0.* TO `wn_owner`@`localhost`;
GRANT ALL ON blockchain1.* TO `wn_owner`@`localhost`;
GRANT ALL ON blockchain2.* TO `wn_owner`@`localhost`;
GRANT ALL ON blockchain3.* TO `wn_owner`@`localhost`;
GRANT ALL ON blockchain4.* TO `wn_owner`@`localhost`;

REVOKE SHUTDOWN ON *.* FROM `wn_owner`@`localhost`;
