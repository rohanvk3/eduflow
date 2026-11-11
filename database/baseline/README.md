# Baseline adoption workspace

`schema.sql` is a 48-table schema-only snapshot generated from isolated MySQL 8.0.46, exercised by the backend integration and real-browser tests. It contains no application rows, CREATE DATABASE/USE statements or DROP TABLE directives. It has been imported into a fresh isolated schema for production-profile validation; see the latest readiness report for the outcome.

SHA-256: `51899B4EECA48BE955FFB91B4E576BD5FA32AF03C5828461D76AA84EAE807A6D`.

Use only after operator review, against a **new empty** MySQL 8 database. Existing populated databases require read-only comparison and individual reviewed forward migrations using [DATABASE.md](../../docs/DATABASE.md). Never apply the full baseline over existing tables. No migration engine or automatic baseline-on-startup was enabled. Historical step SQL is not a replacement for this complete baseline. `scripts/Verify-IsolatedBaseline.ps1` reproduces the empty-schema import and starts the production JAR for validation on loopback.
