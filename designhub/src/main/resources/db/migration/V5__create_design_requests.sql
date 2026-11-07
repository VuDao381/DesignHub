-- V5__create_design_requests.sql
-- Bảng lưu trữ yêu cầu đặt thiết kế riêng theo sản phẩm của khách hàng & nghiệm thu 3D

CREATE TABLE IF NOT EXISTS design_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    product_description TEXT NOT NULL,
    reference_image_url VARCHAR(500),
    budget DECIMAL(12, 2) NOT NULL,
    deadline VARCHAR(100),
    mockup_type VARCHAR(50) DEFAULT 'BOX_3D',
    status VARCHAR(50) NOT NULL DEFAULT 'OPEN',
    preview_image_url VARCHAR(500),
    designer_note VARCHAR(1000),
    customer_id BIGINT NOT NULL,
    designer_id BIGINT,
    category_id BIGINT,
    result_design_id BIGINT,
    created_at DATETIME,
    updated_at DATETIME,
    CONSTRAINT fk_design_requests_customer FOREIGN KEY (customer_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_design_requests_designer FOREIGN KEY (designer_id) REFERENCES designers(id) ON DELETE SET NULL,
    CONSTRAINT fk_design_requests_category FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE SET NULL,
    CONSTRAINT fk_design_requests_result_design FOREIGN KEY (result_design_id) REFERENCES designs(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

