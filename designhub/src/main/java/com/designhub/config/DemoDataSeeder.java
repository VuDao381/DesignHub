package com.designhub.config;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.designhub.entity.Category;
import com.designhub.entity.Design;
import com.designhub.entity.DesignFile;
import com.designhub.entity.DesignImage;
import com.designhub.entity.Designer;
import com.designhub.entity.DownloadHistory;
import com.designhub.entity.Order;
import com.designhub.entity.OrderDetail;
import com.designhub.entity.Payment;
import com.designhub.entity.PaymentStatus;
import com.designhub.entity.Review;
import com.designhub.entity.Role;
import com.designhub.entity.User;
import com.designhub.repository.CategoryRepository;
import com.designhub.repository.DesignFileRepository;
import com.designhub.repository.DesignImageRepository;
import com.designhub.repository.DesignRepository;
import com.designhub.repository.DesignerRepository;
import com.designhub.repository.DownloadHistoryRepository;
import com.designhub.repository.OrderDetailRepository;
import com.designhub.repository.OrderRepository;
import com.designhub.repository.PaymentRepository;
import com.designhub.repository.ReviewRepository;
import com.designhub.repository.UserRepository;

@Component
@org.springframework.core.annotation.Order(10)
public class DemoDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final UserRepository userRepository;
    private final DesignerRepository designerRepository;
    private final CategoryRepository categoryRepository;
    private final DesignRepository designRepository;
    private final DesignImageRepository designImageRepository;
    private final DesignFileRepository designFileRepository;
    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final PaymentRepository paymentRepository;
    private final ReviewRepository reviewRepository;
    private final DownloadHistoryRepository downloadHistoryRepository;
    private final PasswordEncoder passwordEncoder;

    public DemoDataSeeder(
            UserRepository userRepository,
            DesignerRepository designerRepository,
            CategoryRepository categoryRepository,
            DesignRepository designRepository,
            DesignImageRepository designImageRepository,
            DesignFileRepository designFileRepository,
            OrderRepository orderRepository,
            OrderDetailRepository orderDetailRepository,
            PaymentRepository paymentRepository,
            ReviewRepository reviewRepository,
            DownloadHistoryRepository downloadHistoryRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.designerRepository = designerRepository;
        this.categoryRepository = categoryRepository;
        this.designRepository = designRepository;
        this.designImageRepository = designImageRepository;
        this.designFileRepository = designFileRepository;
        this.orderRepository = orderRepository;
        this.orderDetailRepository = orderDetailRepository;
        this.paymentRepository = paymentRepository;
        this.reviewRepository = reviewRepository;
        this.downloadHistoryRepository = downloadHistoryRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (categoryRepository.count() > 0 && designRepository.count() > 0) {
            log.info("Dữ liệu mẫu (Demo Data) đã tồn tại. Bỏ qua bước nạp tự động.");
            return;
        }

        log.info(">>> Đang bắt đầu nạp dữ liệu mẫu (Seeding Demo Data) cho DesignHub...");

        // 1. Tạo Danh mục (Categories)
        Category catUiUx = createCategory("UI/UX & Mobile Apps", "Giao diện Web App, Mobile UI Kit, Dashboard SaaS tối ưu trải nghiệm người dùng.");
        Category catPoster = createCategory("Poster & Quảng Cáo", "Mẫu Poster sự kiện, Banner marketing, Cover mạng xã hội độ phân giải cao.");
        Category catLogo = createCategory("Logo & Brand Identity", "Hệ thống nhận diện thương hiệu, Logo vector, Brand guideline chuyên nghiệp.");
        Category catTemplate = createCategory("Template & Slide", "Slide thuyết trình PowerPoint/Keynote hiện đại, mẫu CV/Resume chuẩn quốc tế.");
        Category cat3d = createCategory("Vector & 3D Artwork", "Gói icon 3D sống động, minh họa vector Isometric, texture thiết kế.");

        // 2. Tạo Tài khoản Người dùng (Users & Designers)
        User alexUser = createUserIfNotExist("vu", "vu@designhub.com", "123456", "Vũ Đào", "0901234567", Role.DESIGNER);
        Designer alexDesigner = createDesignerIfNotExist(alexUser, 
                "Senior UI/UX & Brand Identity Designer với 5+ năm kinh nghiệm. Tác giả của nhiều bộ Design System và Mobile Kits chất lượng cao.",
                "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=200");

        User mayaUser = createUserIfNotExist("hoang", "hoang@designhub.com", "123456", "Hoàng Lê", "0908765432", Role.DESIGNER);
        Designer mayaDesigner = createDesignerIfNotExist(mayaUser,
                "Digital Artist & Visual Designer đam mê phong cách Cyberpunk, Typography nghệ thuật và Poster sự kiện âm nhạc.",
                "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=200");

        User johnUser = createUserIfNotExist("quan", "quan@gmail.com", "123456", "Quân Nguyễn", "0912334455", Role.CUSTOMER);
        User lisaUser = createUserIfNotExist("tuan", "tuan@gmail.com", "123456", "Tuấn Vũ", "0933445566", Role.CUSTOMER);

        // 3. Tạo Bản Thiết Kế (Designs)
        // Design 1: APPROVED - SaaS Dashboard
        Design d1 = createDesign(
                "SaaS Analytics Dashboard UI Kit",
                "Bộ giao diện Dashboard chuyên sâu dành cho nền tảng SaaS & Fintech với hơn 100+ components, hỗ trợ Auto-Layout 5.0, đầy đủ biến thể Dark/Light mode và thiết kế chuẩn Responsive.",
                new BigDecimal("250000"),
                "APPROVED",
                null,
                catUiUx,
                alexDesigner);
        createDesignImage(d1, "https://images.unsplash.com/photo-1551288049-bebda4e38f71?w=1200&auto=format&fit=crop", true);
        createDesignImage(d1, "https://images.unsplash.com/photo-1460925895917-afdab827c52f?w=1200&auto=format&fit=crop", false);
        createDesignFile(d1, "saas-analytics-dashboard-v2.fig", "/uploads/files/saas-analytics-dashboard-v2.fig", 25165824L, "FIG");

        // Design 2: APPROVED - Cyberpunk Poster
        Design d2 = createDesign(
                "Cyberpunk Neon Music Fest Poster",
                "Mẫu Poster sự kiện EDM / Cyberpunk với hiệu ứng ánh sáng Neon rực rỡ, layer Photoshop được tổ chức chuyên nghiệp, chuẩn in ấn CMYK 300 DPI kích thước A2/A3.",
                new BigDecimal("95000"),
                "APPROVED",
                null,
                catPoster,
                mayaDesigner);
        createDesignImage(d2, "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=1200&auto=format&fit=crop", true);
        createDesignFile(d2, "cyberpunk-neon-poster-cmyk.psd", "/uploads/files/cyberpunk-neon-poster-cmyk.psd", 88080384L, "PSD");

        // Design 3: APPROVED - Coffee Brand Identity
        Design d3 = createDesign(
                "Minimalist Artisan Coffee Brand Guidelines",
                "Trọn bộ nhận diện thương hiệu quán cà phê phong cách Tối giản Hiện đại: Logo vector tùy chỉnh, bảng màu Panton, mockup bao bì cốc/túi giấy và hướng dẫn sử dụng font.",
                new BigDecimal("350000"),
                "APPROVED",
                null,
                catLogo,
                alexDesigner);
        createDesignImage(d3, "https://images.unsplash.com/photo-1501339847302-ac426a4a7cbb?w=1200&auto=format&fit=crop", true);
        createDesignFile(d3, "coffee-brand-identity-pack.ai", "/uploads/files/coffee-brand-identity-pack.ai", 33554432L, "AI");

        // Design 4: APPROVED - Fintech Mobile Banking App
        Design d4 = createDesign(
                "Fintech Banking Mobile App UI Concept",
                "Hệ thống 45+ màn hình ứng dụng ngân hàng số & ví điện tử: luồng chuyển tiền, quét mã QR, thẻ ảo, biểu đồ chi tiêu và quản lý tài chính cá nhân.",
                new BigDecimal("180000"),
                "APPROVED",
                null,
                catUiUx,
                alexDesigner);
        createDesignImage(d4, "https://images.unsplash.com/photo-1563986768609-322da13575f3?w=1200&auto=format&fit=crop", true);
        createDesignFile(d4, "fintech-mobile-banking.fig", "/uploads/files/fintech-mobile-banking.fig", 18874368L, "FIG");

        // Design 5: PENDING - Mẫu chờ Admin duyệt
        Design d5 = createDesign(
                "Eco Life Organic Farm Brand Identity",
                "Bộ nhận diện nông sản hữu cơ thân thiện môi trường, gồm logo cách điệu hình mầm cây và bộ icon bao bì tự hủy sinh học.",
                new BigDecimal("290000"),
                "PENDING",
                null,
                catLogo,
                mayaDesigner);
        createDesignImage(d5, "https://images.unsplash.com/photo-1542601906990-b4d3fb778b09?w=1200&auto=format&fit=crop", true);
        createDesignFile(d5, "eco-life-farm-branding.ai", "/uploads/files/eco-life-farm-branding.ai", 23068672L, "AI");

        // Design 6: REJECTED - Mẫu bị từ chối kèm lý do
        Design d6 = createDesign(
                "Retro Vintage Typography Badge Pack",
                "Bộ huy hiệu phong cách cổ điển thập niên 80 gồm 20 mẫu vector tùy biến chữ tự do, phù hợp in áo thun, nhãn mác sản phẩm và mũ nón.",
                new BigDecimal("120000"),
                "REJECTED",
                "Ảnh xem trước kích thước nhỏ và bị mờ. Vui lòng tải lại ảnh mockup độ phân giải tối thiểu 1200x800px và kiểm tra lại font chữ đính kèm.",
                cat3d,
                mayaDesigner);
        createDesignImage(d6, "https://images.unsplash.com/photo-1579783900882-c0d3dad7b119?w=1200&auto=format&fit=crop", true);
        createDesignFile(d6, "retro-typography-badges.zip", "/uploads/files/retro-typography-badges.zip", 15728640L, "ZIP");

        // 4. Tạo Đơn hàng đã mua (Order) cho John Đặng để test "Kho của tôi" và Tải file
        createCompletedOrder(johnUser, d1);

        // 5. Tạo Đánh giá mẫu (Reviews)
        createReview(johnUser, d1, 5, "Sản phẩm cực kỳ chất lượng, các component tổ chức mạch lạc, chuẩn auto-layout, tiết kiệm được rất nhiều thời gian!");
        createReview(lisaUser, d2, 5, "Poster in ra rất nét và màu sắc chuẩn Neon, layer Photoshop phân chia gọn gàng dễ chỉnh sửa chữ.");

        log.info(">>> Nạp dữ liệu mẫu hoàn tất thành công! 5 danh mục, 6 thiết kế (APPROVED, PENDING, REJECTED), 1 đơn hàng đã mua và 2 đánh giá.");
    }

    private Category createCategory(String name, String desc) {
        return categoryRepository.findByNameContainingIgnoreCase(name, org.springframework.data.domain.Pageable.unpaged())
                .stream().findFirst()
                .orElseGet(() -> {
                    Category cat = new Category();
                    cat.setName(name);
                    cat.setDescription(desc);
                    return categoryRepository.save(cat);
                });
    }

    private User createUserIfNotExist(String username, String email, String rawPassword, String fullName, String phone, Role role) {
        return userRepository.findByEmail(email).orElseGet(() -> {
            User u = new User();
            u.setUsername(username);
            u.setEmail(email);
            u.setPassword(passwordEncoder.encode(rawPassword));
            u.setFullName(fullName);
            u.setPhone(phone);
            u.setRole(role);
            return userRepository.save(u);
        });
    }

    private Designer createDesignerIfNotExist(User user, String bio, String avatar) {
        return designerRepository.findByUserId(user.getId()).orElseGet(() -> {
            Designer d = new Designer();
            d.setUser(user);
            d.setBio(bio);
            d.setAvatar(avatar);
            return designerRepository.save(d);
        });
    }

    private Design createDesign(String name, String desc, BigDecimal price, String status, String rejectReason, Category cat, Designer designer) {
        Design d = new Design();
        d.setName(name);
        d.setDescription(desc);
        d.setPrice(price);
        d.setStatus(status);
        d.setRejectReason(rejectReason);
        d.setCategory(cat);
        d.setDesigner(designer);
        return designRepository.save(d);
    }

    private void createDesignImage(Design design, String url, boolean isPrimary) {
        DesignImage img = new DesignImage();
        img.setDesign(design);
        img.setImageUrl(url);
        img.setPrimary(isPrimary);
        designImageRepository.save(img);
    }

    private void createDesignFile(Design design, String fileName, String fileUrl, long size, String type) {
        DesignFile file = new DesignFile();
        file.setDesign(design);
        file.setFileName(fileName);
        file.setFileUrl(fileUrl);
        file.setFileSize(size);
        file.setFileType(type);
        designFileRepository.save(file);
    }

    private void createCompletedOrder(User user, Design design) {
        if (orderRepository.existsByUserId(user.getId())) {
            return;
        }

        Order order = new Order();
        order.setUser(user);
        order.setTotalAmount(design.getPrice());
        order.setStatus("PAID");
        Order savedOrder = orderRepository.save(order);

        OrderDetail detail = new OrderDetail();
        detail.setOrder(savedOrder);
        detail.setDesign(design);
        detail.setQuantity(1);
        detail.setPrice(design.getPrice());
        orderDetailRepository.save(detail);

        Payment payment = new Payment();
        payment.setOrder(savedOrder);
        payment.setAmount(design.getPrice());
        payment.setPaymentMethod("VNPAY");
        payment.setTransactionId("TXN_DEMO_" + System.currentTimeMillis());
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setPaymentDate(LocalDateTime.now());
        paymentRepository.save(payment);

        DownloadHistory history = new DownloadHistory();
        history.setUser(user);
        history.setDesign(design);
        history.setDownloadedAt(LocalDateTime.now());
        downloadHistoryRepository.save(history);
    }

    private void createReview(User user, Design design, int rating, String comment) {
        if (!reviewRepository.existsByUserIdAndDesignId(user.getId(), design.getId())) {
            Review review = new Review();
            review.setUser(user);
            review.setDesign(design);
            review.setRating(rating);
            review.setComment(comment);
            reviewRepository.save(review);
        }
    }
}
