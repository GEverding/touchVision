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
select id, pressure, x, y, z, is_new
from captured_data
where recording_id = :recording_id

-- name: find-all-recordings
-- Find all recording for a patient
select r.id as recording_id, p.name, r.created_on, r.start_time, r.stop_time
from recording as r, patient as p
where r.patient_id = p.id

-- name: find-active-recording
-- Find an active recording
select r.id, p.name, r.created_on, r.start_time
from recording as r, patient as p
where r.patient_id = p.id and
      r.start_time is not null and
      r.stop_time is null
limit 1

-- name: get-data-by-id
-- Find all recording for a patient
select id, pressure, x, y, z, timestamp, is_new
from captured_data
where recording_id=:id and
      timestamp > :start
order by timestamp asc
limit :limit

-- name: get-recording-data
select id, pressure, x, y, z, timestamp, is_new
from captured_data
where recording_id=:id

-- name: set-is-new-false!
update captured_data
set is_new=false
where id in (:ids)
