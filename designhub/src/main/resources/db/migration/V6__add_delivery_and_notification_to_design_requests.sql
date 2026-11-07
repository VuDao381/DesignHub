-- V6__add_delivery_and_notification_to_design_requests.sql
-- Bổ sung trường lưu file sản phẩm bàn giao và thông báo tự động từ hệ thống tới khách hàng

ALTER TABLE design_requests
    ADD COLUMN delivery_file_url VARCHAR(500) NULL,
    ADD COLUMN delivery_file_name VARCHAR(255) NULL,
    ADD COLUMN system_notification VARCHAR(1000) NULL,
    ADD COLUMN customer_unread BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN accepted_at DATETIME NULL,
    ADD COLUMN delivered_at DATETIME NULL;

