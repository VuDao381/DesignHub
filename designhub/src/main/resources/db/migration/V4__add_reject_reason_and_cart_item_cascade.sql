-- V4__add_reject_reason_and_cart_item_cascade.sql
-- Thêm cột reject_reason cho bảng designs để lưu lý do khi admin từ chối thiết kế
-- Idempotent: chỉ thêm cột nếu chưa tồn tại (DB cũ có thể đã được thêm tay khi Flyway chưa chạy)

SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'designs' AND COLUMN_NAME = 'reject_reason'
);
SET @ddl := IF(@col_exists = 0,
    'ALTER TABLE designs ADD COLUMN reject_reason VARCHAR(500) NULL AFTER status',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
