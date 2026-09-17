Automatic type registration
KotlinTypesTests.testAutomaticTypeRegistration
Verifies that Hibernate automatically registers KotlinUuidJavaType and KotlinDurationJavaType. The shared tests run on both H2 and SQLite.

UUID generation with AUTO
KotlinTypesTests.testUuidAutoGeneration
Persists and reloads two entities using the AUTO strategy. Verifies that both UUID identifiers are generated and differ from each other.

UUID generation with UUID
KotlinTypesTests.testUuidExplicitGeneration
Persists and reloads two entities using the explicit UUID strategy. Verifies that both identifiers are generated and distinct.

UUID generation for CHAR columns
KotlinTypesTests.testUuidGenerationForCharColumn
Verifies UUID generation and retrieval through a CHAR(36) identifier column. Two entities must receive distinct identifiers.

UUID column mappings
KotlinTypesTests.testUuidColumnTypesWriteRead
Verifies UUID writes and reads through default, CHAR, VARCHAR and BINARY mappings, including null, all-zero bits, all-one bits and mixed values.

Finite Duration column mappings
KotlinTypesTests.testDurationColumnTypesWriteRead
Verifies zero, positive and negative durations through default, NUMERIC, DECIMAL, VARCHAR, BIGINT and DOUBLE mappings.

Null Duration columns
KotlinTypesTests.testDurationNullColumns
Persists an entity with null Duration values and verifies that all six mapped columns still return null after reloading.

Infinite Duration values
KotlinTypesTests.testDurationInfiniteWriteRead
Verifies that positive and negative infinity survive writes and reads through default numeric, NUMERIC, DECIMAL, VARCHAR and DOUBLE mappings.

DOUBLE infinity sentinel
KotlinTypesTests.testDurationDoubleInfiniteStorage
Verifies both infinities through DOUBLE using a sentinel that resolves to the same value at FLOAT and DOUBLE precision.

DOUBLE read overflow
KotlinTypesTests.testDurationDoubleReadOverflow
Inserts oversized DOUBLE nanosecond values of both signs using native SQL and verifies that reading them as Duration throws ArithmeticException.

Numeric string overflow
KotlinTypesTests.testDurationFromStringOverflow
Passes values just beyond the finite boundary and values too large for Long to JavaType.fromString. Both signs must throw ArithmeticException.

ISO string read overflow
KotlinTypesTests.testDurationVarcharReadOverflow
Inserts oversized ISO durations of both signs into VARCHAR. Reading them must throw ArithmeticException instead of silently producing infinity.

BIGINT rejects infinity
KotlinTypesTests.testDurationBigintRejectsInfinite
Verifies that writing either positive or negative infinity to BIGINT fails with IllegalArgumentException and a message explaining that the value cannot be stored.

BIGINT rejects overflow
KotlinTypesTests.testDurationBigintRejectsOverflow
Verifies that finite durations of either sign whose nanosecond counts exceed the Long range cannot be stored in BIGINT and produce IllegalArgumentException.

Numeric Duration values beyond Long
KotlinTypesTests.testDurationNumericBeyondLongRange
Verifies exact writes and reads of positive and negative 10^21 nanoseconds through default, NUMERIC and DECIMAL mappings on both databases.

Finite Duration limits in VARCHAR
KotlinTypesTests.testDurationVarcharFullFiniteRange
Verifies that the largest finite Duration and its negative counterpart survive writes and reads through VARCHAR without losing precision.

NUMERIC stores nanoseconds
KotlinTypesTests.testDurationNumericStoresNanoseconds
Reads the column using native SQL and verifies that -(12 seconds + 345 nanoseconds) is stored as the integer -12000000345.

Instant column mappings
KotlinTypesTests.testInstantColumnTypesWriteRead
Verifies instants before, at and after the epoch through default, TIMESTAMP and TIMESTAMP_UTC mappings, preserving nanoseconds on H2 and milliseconds on SQLite.

Null Instant columns
KotlinTypesTests.testInstantNullColumns
Verifies that the automatically applied Instant converter preserves null values through default, TIMESTAMP and TIMESTAMP_UTC mappings.

HQL parameters
KotlinTypesTests.testHqlParameters
Binds Kotlin Uuid, Duration and Instant values as HQL parameters and verifies that each query finds the corresponding persisted entity.

Duration dirty checking
KotlinTypesTests.testDurationDirtyChecking
Changes a managed Duration from one second to minus one nanosecond, flushes and clears the persistence context, then verifies that the database contains the updated value.

Finite Duration limits in H2 numeric columns
H2KotlinTypesTests.testDurationNumericFullFiniteRange
On H2 only, verifies exact writes and reads of the largest finite Duration and its negative counterpart through default, NUMERIC and DECIMAL mappings.

Numeric Duration read overflow
KotlinTypesTests.testDurationNumericReadOverflow
Inserts out-of-range nanosecond counts of both signs using native SQL. Reading default, NUMERIC and DECIMAL columns must throw ArithmeticException on H2 and SQLite.

Numeric Duration boundary overflow on H2
H2KotlinTypesTests.testDurationNumericReadBoundaryOverflow
Checks both signs just beyond the finite Duration limit and next to infinity sentinels. Numeric reads must throw ArithmeticException, even when millisecond truncation could hide overflow.

Infinity rejected by narrow numeric columns
H2KotlinTypesTests.testDurationInfinityRejectedByNarrowNumericColumn
On H2, verifies that default, NUMERIC and DECIMAL columns with precision 27 reject both 28-digit infinity sentinels with a database data exception.

Infinity allowed by narrow numeric columns
SqliteKotlinTypesTests.testDurationInfinityAllowedByNarrowNumericColumn
On SQLite, verifies that default, NUMERIC and DECIMAL columns declared with precision 27 still preserve both 28-digit infinity sentinels.
