-- name: new-recording<!
-- Creates a New Recording record
insert into recording (patient_id) values (:patient_id)

-- name: start-recording!
-- Start Recording
update recording set start_time = CURRENT_TIMESTAMP where id = :id

-- name: stop-recording!
-- Stop Recording
update recording set stop_time = CURRENT_TIMESTAMP where id = :id

-- name: create-patient<!
-- Create new Patient Record
insert into patient (name) values (:name)

-- name: append<!
-- Append new captured data from capture device
insert into captured_data (recording_id, pressure, x, y, z, timestamp) values(:recording_id, :pressure, :x, :y, :z, :timestamp)

-- name: fetch-data-by-record-id
-- Fetch All rows of a recording
select id, pressure, x, y, z
from captured_data
where recording_id = :recording_id
