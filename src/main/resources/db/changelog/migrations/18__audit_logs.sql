--liquibase formatted sql

--changeset kinhduanpc:18__audit_logs runOnChange:false splitStatements:true endDelimiter:;
--comment Audit trail chung cho ReturnRequest, ServiceRequest, Warranty, VoucherPolicy

CREATE TABLE IF NOT EXISTS audit_logs (
    id                  SERIAL          PRIMARY KEY,

    -- Đối tượng được audit
    entity_type         VARCHAR(40)     NOT NULL,   -- RETURN_REQUEST | SERVICE_REQUEST | WARRANTY | VOUCHER_POLICY
    entity_id           BIGINT          NOT NULL,

    -- Hành động
    action              VARCHAR(50)     NOT NULL,   -- STATUS_CHANGED | NOTE_ADDED | COST_QUOTED | DIAGNOSIS_SET | ...
    from_value          TEXT,                       -- Giá trị trước (trạng thái cũ, giá trị cũ...)
    to_value            TEXT,                       -- Giá trị sau
    note                TEXT,                       -- Ghi chú thêm của nhân viên

    -- Ai thực hiện
    performed_by        INT             REFERENCES users(id) ON DELETE SET NULL,
    performed_by_name   VARCHAR(150),
    performed_by_role   VARCHAR(30),

    created_at          TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_audit_logs_entity ON audit_logs(entity_type, entity_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_logs_performer ON audit_logs(performed_by);
