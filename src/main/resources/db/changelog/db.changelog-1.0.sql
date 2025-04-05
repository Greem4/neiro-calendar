--liquibase formatted sql

--changeset Greem4:1
CREATE TABLE attendance_records
(
    id          BIGSERIAL PRIMARY KEY,
    person_name VARCHAR(255) NOT NULL,
    visit_date  DATE         NOT NULL,
    attended    BOOLEAN DEFAULT FALSE
);