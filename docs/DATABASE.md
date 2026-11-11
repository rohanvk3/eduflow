# Database integrity and migration adoption

Production uses MySQL 8 and `spring.jpa.hibernate.ddl-auto=validate`. Local development retains its original `update` behavior. Tests use in-memory H2, with explicit MySQL verification opt-in. No Flyway or Liquibase is currently installed. A [48-table schema-only baseline](../database/baseline/README.md) is now generated from and tested against isolated MySQL 8.0.46. SQL in `database/step*-schema.sql` remains historical incremental DDL; never concatenate and execute it against a populated database.

Entities define foreign keys, unique identifiers, payment idempotency keys, attempt uniqueness, notification deduplication, optimistic versions and financial decimal columns. Services retain enrollment/payment history and use transactions and locks. H2 cannot certify MySQL locking, collation or index behavior.

## Safe baseline procedure (manual release gate)

1. Inventory the actual database version, table/column definitions, indexes, foreign keys, row counts and invalid/orphaned relationships read-only. Record `SHOW CREATE TABLE` output for every mapped table. Protect this inventory as confidential infrastructure metadata.
2. Take a consistent database and private-file backup. Restore into a new restricted staging database. Never use the live database to experiment with DDL.
3. Compare the clone to JPA mappings and historical SQL. Identify missing columns/indexes, nulls, duplicate values and invalid references. Check all money precision and scale before converting any column. Never auto-delete offending rows.
4. Compare the supplied `database/baseline/schema.sql` against the reconciled clone, with no application rows, users, passwords or DEFINER clauses. Review differences and record checksums. The supplied baseline has isolated test evidence, but that does not establish compatibility with an unseen populated installation.
5. For a new empty database only, apply the reviewed baseline using a separate migration account. For an existing populated database, apply individually reviewed forward migrations to the clone, preserve history, and rehearse rollback/restore first. Record script checksums and application versions in a release ledger.
6. Start the JAR with `prod`; Hibernate validation must pass. Run MySQL workflows and compare fee ledger, payment allocations, void exclusions and analytics totals before approval. Restrict the runtime account to required DML, without DROP/ALTER privileges.
7. Adopt Flyway/Liquibase only through a reviewed subsequent change: baseline existing schema after comparison, never enable automatic baseline-on-migrate or use a fresh-schema script on populated tables.

The supplied baseline and paired database/upload restoration now have isolated execution evidence; see [FINAL_READINESS_REPORT.md](FINAL_READINESS_REPORT.md). Comparison against any existing installation remains an operator release prerequisite. No migrations have been applied to the user's development database by this milestone.
