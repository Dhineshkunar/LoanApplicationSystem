-- Reference DDL for PostgreSQL production deployment.
-- In real production this would be a Flyway/Liquibase migration
-- (e.g. V1__create_loan_applications_table.sql), NOT run by
-- hibernate.ddl-auto. Included here so the schema is explicit and
-- reviewable independent of the JPA mapping.

CREATE TABLE loan_applications (
    id               BIGSERIAL PRIMARY KEY,
    applicant_name   VARCHAR(100)     NOT NULL,
    mobile_number    VARCHAR(15)      NOT NULL,
    email            VARCHAR(150)     NOT NULL,
    loan_amount      NUMERIC(15, 2)   NOT NULL CHECK (loan_amount > 0),
    loan_type        VARCHAR(20)      NOT NULL,
    status           VARCHAR(20)      NOT NULL,
    interest_rate    NUMERIC(5, 2)    NOT NULL CHECK (interest_rate > 0),
    tenure_months    INTEGER          NOT NULL CHECK (tenure_months > 0),
    created_at       TIMESTAMP        NOT NULL DEFAULT now(),
    updated_at       TIMESTAMP        NOT NULL DEFAULT now()
);

CREATE INDEX idx_loan_status ON loan_applications (status);
CREATE INDEX idx_loan_type   ON loan_applications (loan_type);

COMMENT ON COLUMN loan_applications.status IS
    'One of: PENDING, UNDER_REVIEW, APPROVED, REJECTED, DISBURSED, CLOSED';
COMMENT ON COLUMN loan_applications.loan_type IS
    'One of: HOME, PERSONAL, VEHICLE, EDUCATION, BUSINESS';
