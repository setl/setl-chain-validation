# MySQL DB Store
This module implements the [DBStore](../chain-dbstore/src/main/java/io/setl/bc/chain/dbstore/DBStore.java) interface and allows connectivity to a MySQL database, via JDBC.

## Schema creation
To initialize the blockchain correctly, a minimum of 3 schemas must be created, i.e. 1 for the validation node, 1 for the wallet node and 1 for the reporting node.

All SQL scripts are inside this sub-module [resources/db](src/main/resources/db) folder, and need to be run as DB user `root@localhost`. Within the folder are three sub-folders:

* [procedures](database/db/procedures)
* [schema](database/db/schema)
* [users](database/db/users)

Each containing the relevant scripts to create stored procedures, DB schema and DB users, respectively. All the scripts must be run in the order specified below, otherwise errors will occur. 

1. `cd database`
2. `mysql -u root -p < db/schema/createSchemas.sql`
3. `mysql -u root -p blockchain0 < db/schema/createTables.sql`
4. `mysql -u root -p blockchain1 < db/schema/createTables.sql`
5. `mysql -u root -p blockchain2 < db/schema/createTables.sql`
6. `mysql -u root -p blockchain3 < db/schema/createTables.sql`
7. `mysql -u root -p blockchain4 < db/schema/createTables.sql`
8. `mysql -u root -p < db/users/createVnOwnerUser.sql`
9. `mysql -u root -p < db/users/createWnOwnerUser.sql`
10. `flyway -configFiles=flyway.blockchain0.conf baseline; flyway -configFiles=flyway.blockchain0.conf migrate`
11. `flyway -configFiles=flyway.blockchain1.conf baseline; flyway -configFiles=flyway.blockchain1.conf migrate`
12. `flyway -configFiles=flyway.blockchain2.conf baseline; flyway -configFiles=flyway.blockchain2.conf migrate`
13. `flyway -configFiles=flyway.blockchain3.conf baseline; flyway -configFiles=flyway.blockchain3.conf migrate`
14. `flyway -configFiles=flyway.blockchain4.conf baseline; flyway -configFiles=flyway.blockchain4.conf migrate`
15. `mysql -u root -p < db/users/createVnClientUser.sql`
16. `mysql -u root -p < db/users/createWnClientUser.sql`

## Blockchain initialization
