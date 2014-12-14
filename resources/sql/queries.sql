-- name: new-recording<!
-- Creates a New Recording record
insert into recording (patient_id) values (:patient_id)

-- name: create-patient<!
-- Create new Patient Record
insert into patient (name) values (:name)

-- name: append<!
-- Append new captured data from capture device
insert into captured_data (recording_id, pressure, x, y, z, timestamp) values(:recording_id, :pressure, :x, :y, :z, :timestamp)
