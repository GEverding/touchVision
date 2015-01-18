-- ======================================================================
-- ===   Sql Script for Database : touchVision
-- ===
-- === Build : 14
-- ======================================================================

BEGIN WORK;

CREATE SCHEMA tv;

SET SEARCH_PATH=tv;
-- ======================================================================

CREATE TABLE patient
  (
    id          serial,
    name        varchar(32)   not null,
    created_on  timestamp     not null default current_timestamp,

    primary key(id)
  );

-- ======================================================================

CREATE TABLE recording
  (
    id          serial,
    created_on  timestamp   not null default current_timestamp,
    patient_id  int         not null,
    start_time  timestamp,
    stop_time   timestamp,

    primary key(id),

    foreign key(patient_id) references patient(id)
  );

-- ======================================================================

CREATE TABLE captured_data
  (
    id            serial   not null,
    recording_id  int      not null,
    pressure      float4   not null,
    x             float8   not null,
    y             float8   not null,
    z             float8   not null,
    timestamp     float8   not null,

    primary key(id),

    foreign key(recording_id) references recording(id) on delete CASCADE
  );

-- ======================================================================

COMMIT;
-- ======================================================================

