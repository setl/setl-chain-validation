CREATE USER IF NOT EXISTS 'vn_owner'@'localhost' IDENTIFIED BY 'm68/^Ryt!Hw45vS[';

GRANT ALL ON blockchain0.* TO `vn_owner`@`localhost`;
GRANT ALL ON blockchain1.* TO `vn_owner`@`localhost`;
GRANT ALL ON blockchain2.* TO `vn_owner`@`localhost`;
GRANT ALL ON blockchain3.* TO `vn_owner`@`localhost`;
GRANT ALL ON blockchain4.* TO `vn_owner`@`localhost`;

REVOKE SHUTDOWN ON *.* FROM `vn_owner`@`localhost`;
