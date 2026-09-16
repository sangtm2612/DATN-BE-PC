--liquibase formatted sql

--changeset kinhduanpc:13__order_history runOnChange:false splitStatements:true endDelimiter:;
--comment Audit trail: lưu lịch sử thay đổi trạng thái đơn hàng (ai làm gì, lúc nào)

-- ============================================================
-- LỊCH SỬ ĐƠN HÀNG — audit trail đầy đủ
-- Mỗi lần đổi trạng thái, hủy, ghi chú đều tạo 1 bản ghi
-- Trả lời được: ai xác nhận? ai ship? nhân viên nào xử lý?
-- ============================================================

CREATE TABLE IF NOT EXISTS order_history (
    id                  SERIAL          PRIMARY KEY,
    order_id            INT             NOT NULL REFERENCES orders(id) ON DELETE CASCADE,

    -- Trạng thái trước và sau khi đổi
    from_status         VARCHAR(30),                        -- NULL nếu là bản ghi khởi tạo
    to_status           VARCHAR(30)     NOT NULL,

    -- Ai thực hiện thao tác
    performed_by        INT             REFERENCES users(id) ON DELETE SET NULL,
    performed_by_name   VARCHAR(150),                       -- Snapshot tên tại thời điểm (không đổi nếu user đổi tên sau)
    performed_by_role   VARCHAR(30),                        -- customer | staff | technician | admin | system

    -- Ghi chú nội bộ của nhân viên khi thực hiện
    note                TEXT,

    -- Actor thực hiện
    actor_type          VARCHAR(20)     NOT NULL DEFAULT 'system',  -- customer | staff | system

    created_at          TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_order_history_order ON order_history(order_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_order_history_performer ON order_history(performed_by);
