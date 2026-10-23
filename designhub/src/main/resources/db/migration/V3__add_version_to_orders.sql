-- V3__add_version_to_orders.sql
-- Thêm cột version để hỗ trợ Optimistic Locking ngăn ngừa race condition khi cập nhật đơn hàng

ALTER TABLE orders ADD COLUMN version BIGINT DEFAULT 0;
