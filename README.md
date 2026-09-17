# 🎨 DesignHub

**DesignHub** là nền tảng thương mại điện tử chuyên **kinh doanh và phân phối các bản thiết kế số**. Website cho phép các Designer đăng tải, quản lý và bán sản phẩm thiết kế, đồng thời cung cấp cho khách hàng khả năng tìm kiếm, xem thông tin, mua và tải xuống các bản thiết kế sau khi thanh toán.

Dự án được xây dựng nhằm mô phỏng một **sàn thương mại điện tử dành cho sản phẩm thiết kế số**, với các chức năng quản lý người dùng, sản phẩm, giỏ hàng, đơn hàng, thanh toán, đánh giá và thống kê doanh thu.

---

## 📌 1. Giới thiệu chung

Trong thời đại số, nhu cầu sử dụng các sản phẩm thiết kế như Template, Poster, UI/UX, CV, Presentation, Social Media Design,... ngày càng phổ biến.

DesignHub được xây dựng để kết nối:

* 🎨 **Designer** – Người tạo và kinh doanh bản thiết kế.
* 👤 **Customer** – Người tìm kiếm và mua bản thiết kế.
* 👨‍💼 **Admin** – Người quản trị và kiểm soát toàn bộ hệ thống.

Website hướng đến việc tạo ra một môi trường thuận tiện để **đăng bán – tìm kiếm – mua – tải – đánh giá** các sản phẩm thiết kế số.

---

## 🎯 2. Mục tiêu của hệ thống

DesignHub hướng đến các mục tiêu:

* Xây dựng website kinh doanh bản thiết kế số.
* Quản lý thông tin Designer và Customer.
* Quản lý danh mục và sản phẩm thiết kế.
* Hỗ trợ tìm kiếm và lọc sản phẩm.
* Quản lý giỏ hàng và đơn hàng.
* Mô phỏng quy trình thanh toán trực tuyến.
* Cung cấp quyền tải sản phẩm sau khi mua.
* Cho phép khách hàng đánh giá sản phẩm.
* Hỗ trợ Designer theo dõi sản phẩm và doanh thu.
* Hỗ trợ Admin quản lý và kiểm duyệt hệ thống.

---

# ⚙️ 3. Chức năng hệ thống

## 👤 3.1. Chức năng dành cho Customer

### 🔐 Quản lý tài khoản

* Đăng ký tài khoản.
* Đăng nhập.
* Đăng xuất.
* Cập nhật thông tin cá nhân.
* Thay đổi mật khẩu.

### 🔎 Tìm kiếm và xem sản phẩm

* Xem danh sách bản thiết kế.
* Xem chi tiết sản phẩm.
* Tìm kiếm theo tên sản phẩm.
* Lọc theo danh mục.
* Lọc theo khoảng giá.
* Xem hình ảnh Preview.
* Xem thông tin Designer.
* Xem đánh giá của những người đã mua.

### 🛒 Giỏ hàng

* Thêm sản phẩm vào giỏ hàng.
* Xóa sản phẩm khỏi giỏ hàng.
* Thay đổi sản phẩm trong giỏ.
* Xem tổng tiền.
* Tiến hành đặt hàng.

### 💳 Đặt hàng và thanh toán

Quy trình mua hàng:

```text
Xem sản phẩm
      ↓
Thêm vào giỏ hàng
      ↓
Kiểm tra đơn hàng
      ↓
Đặt hàng
      ↓
Thanh toán
      ↓
Thanh toán thành công
      ↓
Cấp quyền tải
      ↓
Tải bản thiết kế
```

### 📥 Quản lý bản thiết kế đã mua

* Xem lịch sử mua hàng.
* Xem các sản phẩm đã mua.
* Tải xuống bản thiết kế.
* Xem lịch sử tải xuống.

### ❤️ Yêu thích

* Thêm sản phẩm vào danh sách yêu thích.
* Xóa sản phẩm khỏi danh sách yêu thích.
* Xem lại các sản phẩm đã lưu.

### ⭐ Đánh giá

Sau khi mua sản phẩm, Customer có thể:

* Đánh giá số sao.
* Viết nhận xét.
* Xem đánh giá của những khách hàng khác.

---

# 🎨 3.2. Chức năng dành cho Designer

Designer là người cung cấp và kinh doanh các bản thiết kế trên hệ thống.

### 👨‍🎨 Quản lý hồ sơ

* Đăng ký trở thành Designer.
* Cập nhật thông tin cá nhân.
* Quản lý thông tin cửa hàng/hồ sơ Designer.

### 📦 Quản lý sản phẩm

Designer có thể:

* Thêm bản thiết kế mới.
* Upload file thiết kế.
* Upload hình ảnh Preview.
* Nhập tên sản phẩm.
* Nhập mô tả.
* Chọn danh mục.
* Thiết lập giá bán.
* Chỉnh sửa sản phẩm.
* Xóa sản phẩm.
* Ẩn/hiển thị sản phẩm.

Một sản phẩm có thể chứa:

```text
Tên sản phẩm
Mô tả
Giá
Danh mục
Designer
Ảnh Preview
File thiết kế
Ngày đăng
Trạng thái
Số lượt bán
```

### 📊 Theo dõi doanh thu

Designer có thể xem:

* Số lượng sản phẩm.
* Số lượt bán.
* Doanh thu.
* Sản phẩm bán chạy.
* Đánh giá của khách hàng.

---

# 👨‍💼 3.3. Chức năng dành cho Admin

Admin chịu trách nhiệm quản lý và kiểm soát toàn bộ hệ thống.

### 👥 Quản lý người dùng

* Xem danh sách người dùng.
* Tìm kiếm người dùng.
* Xem thông tin tài khoản.
* Khóa/mở khóa tài khoản.
* Quản lý quyền người dùng.

### 🎨 Quản lý Designer

* Xem danh sách Designer.
* Xem thông tin Designer.
* Kiểm tra hoạt động của Designer.
* Quản lý trạng thái Designer.

### 📦 Quản lý sản phẩm

* Xem tất cả bản thiết kế.
* Thêm/sửa/xóa sản phẩm.
* Quản lý danh mục.
* Kiểm duyệt bản thiết kế.
* Ẩn sản phẩm vi phạm.

### 🛍️ Quản lý đơn hàng

* Xem danh sách đơn hàng.
* Xem chi tiết đơn hàng.
* Theo dõi trạng thái thanh toán.
* Theo dõi sản phẩm đã mua.

### 💰 Quản lý giao dịch

* Theo dõi giao dịch.
* Kiểm tra trạng thái thanh toán.
* Thống kê doanh thu.

### 📊 Dashboard

Admin có thể theo dõi:

* Tổng số người dùng.
* Tổng số Designer.
* Tổng số sản phẩm.
* Tổng số đơn hàng.
* Tổng doanh thu.
* Sản phẩm được mua nhiều.
* Doanh thu theo thời gian.

---

# 🏗️ 4. Hệ thống phân quyền

DesignHub sử dụng mô hình phân quyền theo vai trò:

| Vai trò     | Quyền chính                  |
| ----------- | ---------------------------- |
| 👤 Customer | Tìm kiếm, mua, tải, đánh giá |
| 🎨 Designer | Đăng bán và quản lý thiết kế |
| 👨‍💼 Admin | Quản lý toàn bộ hệ thống     |

Mô hình:

```text
                    DESIGNHUB
                        │
          ┌─────────────┼─────────────┐
          ↓             ↓             ↓
      CUSTOMER       DESIGNER        ADMIN
          │             │             │
          ↓             ↓             ↓
       Mua hàng      Bán thiết kế   Quản trị
       Tải file      Quản lý SP     Kiểm duyệt
       Đánh giá      Doanh thu      Thống kê
```

---

# 🗄️ 5. Cơ sở dữ liệu

Hệ thống dự kiến sử dụng các bảng chính:

```text
USER
 │
 ├── CUSTOMER
 │
 └── DESIGNER
        │
        ↓
      DESIGN
        │
        ├── CATEGORY
        ├── DESIGN_IMAGE
        └── DESIGN_FILE

CUSTOMER
   │
   ├── CART
   │      └── CART_ITEM
   │
   ├── ORDER
   │      └── ORDER_DETAIL
   │
   ├── PAYMENT
   │
   ├── REVIEW
   │
   ├── FAVORITE
   │
   └── DOWNLOAD_HISTORY
```

### Các bảng chính

| Bảng               | Chức năng              |
| ------------------ | ---------------------- |
| `users`            | Thông tin tài khoản    |
| `customers`        | Thông tin khách hàng   |
| `designers`        | Thông tin Designer     |
| `categories`       | Danh mục thiết kế      |
| `designs`          | Thông tin bản thiết kế |
| `design_images`    | Hình ảnh Preview       |
| `design_files`     | File thiết kế          |
| `carts`            | Giỏ hàng               |
| `cart_items`       | Chi tiết giỏ hàng      |
| `orders`           | Đơn hàng               |
| `order_details`    | Chi tiết đơn hàng      |
| `payments`         | Thông tin thanh toán   |
| `reviews`          | Đánh giá               |
| `favorites`        | Danh sách yêu thích    |
| `download_history` | Lịch sử tải xuống      |

---

# 🔒 6. Quyền tải bản thiết kế

Một trong những nghiệp vụ chính của hệ thống là **kiểm soát quyền tải file**.

File thiết kế gốc sẽ không được cung cấp trực tiếp cho người chưa mua sản phẩm.

```text
Customer
    ↓
Mua Design
    ↓
Thanh toán thành công
    ↓
Order = PAID
    ↓
Xác nhận quyền sở hữu
    ↓
Cho phép Download
```

---

# 🖥️ 7. Các giao diện chính

Website dự kiến bao gồm:

### Trang khách hàng

* 🏠 Trang chủ
* 🔎 Trang tìm kiếm
* 🎨 Danh sách thiết kế
* 📄 Chi tiết thiết kế
* 🛒 Giỏ hàng
* 💳 Thanh toán
* 📦 Lịch sử đơn hàng
* 📥 Sản phẩm đã mua
* ❤️ Danh sách yêu thích
* 👤 Hồ sơ cá nhân

### Trang Designer

* 📊 Dashboard
* 📦 Quản lý sản phẩm
* ➕ Thêm sản phẩm
* ✏️ Chỉnh sửa sản phẩm
* 💰 Doanh thu
* ⭐ Đánh giá

### Trang Admin

* 📊 Dashboard
* 👥 Quản lý người dùng
* 🎨 Quản lý Designer
* 📦 Quản lý thiết kế
* 🏷️ Quản lý danh mục
* 🛍️ Quản lý đơn hàng
* 💳 Quản lý giao dịch
* 📈 Thống kê

---

# 🛠️ 8. Công nghệ sử dụng

## Frontend

* HTML5
* CSS
* JavaScript
* Bootstrap

## Backend

* Java
* Spring Boot
* RESTful API

## Database

* MySQL

## Công cụ phát triển

* Visual Studio Code / IntelliJ IDEA
* MySQL Workbench
* Git
* GitHub
* Postman

---

# 🧩 9. Kiến trúc hệ thống

Hệ thống được xây dựng theo mô hình:

```text
┌─────────────────────────────┐
│          FRONTEND           │
│       React / HTML / CSS    │
└──────────────┬──────────────┘
               │
               │ REST API
               ↓
┌─────────────────────────────┐
│           BACKEND           │
│        Spring Boot          │
│                             │
│  Controller → Service       │
│             → Repository    │
└──────────────┬──────────────┘
               │
               ↓
┌─────────────────────────────┐
│          DATABASE           │
│            MySQL            │
└─────────────────────────────┘
```

---

# 🔐 10. Bảo mật

Hệ thống dự kiến triển khai các cơ chế:

* Xác thực người dùng.
* Phân quyền theo Role.
* Mã hóa mật khẩu.
* JWT Authentication.
* Kiểm tra quyền truy cập API.
* Kiểm tra dữ liệu đầu vào.
* Kiểm soát quyền tải file.
* Ngăn người dùng truy cập trực tiếp file chưa được mua.

---

# 🧪 11. Quy trình hoạt động mẫu

### Customer mua sản phẩm

```text
1. Đăng nhập
      ↓
2. Tìm kiếm "CV Template"
      ↓
3. Xem chi tiết
      ↓
4. Thêm vào giỏ hàng
      ↓
5. Đặt hàng
      ↓
6. Thanh toán
      ↓
7. Hệ thống xác nhận thanh toán
      ↓
8. Cấp quyền tải
      ↓
9. Download file
      ↓
10. Đánh giá sản phẩm
```

### Designer đăng bán sản phẩm

```text
1. Đăng nhập Designer
      ↓
2. Chọn "Thêm sản phẩm"
      ↓
3. Nhập thông tin
      ↓
4. Upload Preview
      ↓
5. Upload file thiết kế
      ↓
6. Thiết lập giá
      ↓
7. Gửi sản phẩm
      ↓
8. Admin kiểm duyệt
      ↓
9. Sản phẩm được hiển thị
      ↓
10. Khách hàng mua
```

---


## ⭐ DesignHub

> **Create. Share. Sell.**

**Một nền tảng kết nối Designer và khách hàng thông qua việc kinh doanh các sản phẩm thiết kế số.**
