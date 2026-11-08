package com.designhub.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.designhub.entity.Category;
import com.designhub.entity.Design;
import com.designhub.entity.DesignRequest;
import com.designhub.entity.Designer;
import com.designhub.entity.Role;
import com.designhub.entity.User;
import com.designhub.exception.ResourceNotFoundException;
import com.designhub.repository.CategoryRepository;
import com.designhub.repository.DesignRepository;
import com.designhub.repository.DesignRequestRepository;
import com.designhub.repository.DesignerRepository;
import com.designhub.security.CurrentUserService;

@Service
@Transactional
public class DesignRequestService {

    private final DesignRequestRepository designRequestRepository;
    private final DesignerRepository designerRepository;
    private final CategoryRepository categoryRepository;
    private final DesignRepository designRepository;
    private final CurrentUserService currentUserService;

    public DesignRequestService(
            DesignRequestRepository designRequestRepository,
            DesignerRepository designerRepository,
            CategoryRepository categoryRepository,
            DesignRepository designRepository,
            CurrentUserService currentUserService) {
        this.designRequestRepository = designRequestRepository;
        this.designerRepository = designerRepository;
        this.categoryRepository = categoryRepository;
        this.designRepository = designRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public List<DesignRequest> getMyRequests() {
        User me = currentUserService.getCurrentUser();
        return designRequestRepository.findByCustomerIdOrderByCreatedAtDesc(me.getId());
    }

    @Transactional(readOnly = true)
    public List<DesignRequest> getDesignerRequests() {
        User me = currentUserService.getCurrentUser();
        if (currentUserService.isAdmin()) {
            return designRequestRepository.findAllByOrderByCreatedAtDesc();
        }
        Designer designer = designerRepository.findByUserId(me.getId())
                .orElseThrow(() -> new AccessDeniedException("Tài khoản chưa kích hoạt Kênh Designer"));
        return designRequestRepository.findOpenOrAssignedToDesigner(designer.getId());
    }

    public DesignRequest createRequest(DesignRequest req) {
        User me = currentUserService.getCurrentUser();
        req.setId(null);
        req.setCustomer(me);
        req.setStatus("OPEN");
        req.setCustomerUnread(false);

        if (req.getMockupType() == null || req.getMockupType().isBlank()) {
            req.setMockupType("STANDARD_2D");
        }

        Long catId = req.getCategoryId();
        if (catId != null) {
            Category cat = categoryRepository.findById(catId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục"));
            req.setCategory(cat);
        } else {
            req.setCategory(null);
        }

        Long desId = req.getDesignerId();
        if (desId != null) {
            Designer des = designerRepository.findById(desId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Designer"));
            req.setDesigner(des);
        } else {
            req.setDesigner(null);
        }

        String method = (req.getPaymentMethod() != null && !req.getPaymentMethod().isBlank())
                ? req.getPaymentMethod().trim().toUpperCase()
                : "VNPAY";
        req.setPaymentMethod(method);

        if ("UNPAID".equalsIgnoreCase(req.getPaymentStatus())) {
            req.setPaymentStatus("UNPAID");
            req.setStatus("PENDING_PAYMENT");
            req.setSystemNotification("Yêu cầu đã tạo, chờ thanh toán.");
        } else {
            String txCode = "DH-" + System.currentTimeMillis();
            req.setPaymentStatus("ESCROW_HELD");
            req.setTransactionCode(txCode);
            req.setPaidAt(LocalDateTime.now());
            req.setStatus("OPEN");
            req.setSystemNotification("Yêu cầu đã được gửi lên hệ thống, đang chờ Designer tiếp nhận.");
        }
        return designRequestRepository.save(req);
    }

    public DesignRequest payEscrow(Long id, Map<String, Object> payload) {
        DesignRequest req = designRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu thiết kế #" + id));
        currentUserService.checkOwnerOrAdmin(req.getCustomer().getId());

        String method = (payload != null && payload.get("paymentMethod") != null)
                ? String.valueOf(payload.get("paymentMethod")).trim().toUpperCase()
                : "VNPAY";
        String txCode = "DH-" + System.currentTimeMillis();

        req.setPaymentMethod(method);
        req.setPaymentStatus("ESCROW_HELD");
        req.setTransactionCode(txCode);
        req.setPaidAt(LocalDateTime.now());
        if ("PENDING_PAYMENT".equalsIgnoreCase(req.getStatus())) {
            req.setStatus("OPEN");
        }
        req.setCustomerUnread(false);
        req.setSystemNotification("Đã thanh toán thành công, đang chờ Designer tiếp nhận.");
        return designRequestRepository.save(req);
    }

    public DesignRequest acceptRequest(Long id) {
        User me = currentUserService.getCurrentUser();
        Designer designer = designerRepository.findByUserId(me.getId())
                .orElseGet(() -> {
                    if (me.getRole() == Role.ADMIN || me.getRole() == Role.DESIGNER) {
                        Designer auto = new Designer();
                        auto.setUser(me);
                        auto.setBio("Designer tại DesignHub");
                        return designerRepository.save(auto);
                    }
                    throw new AccessDeniedException("Chỉ Designer mới có quyền nhận yêu cầu thiết kế");
                });

        DesignRequest req = designRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu thiết kế #" + id));

        if (!"ESCROW_HELD".equalsIgnoreCase(req.getPaymentStatus())) {
            throw new IllegalArgumentException("Yêu cầu này chưa được thanh toán");
        }

        if (!"OPEN".equalsIgnoreCase(req.getStatus()) && (req.getDesigner() == null || !req.getDesigner().getId().equals(designer.getId()))) {
            throw new IllegalArgumentException("Yêu cầu này đã được nhận xử lý bởi Designer khác hoặc đã đóng");
        }

        String designerName = designer.getUser() != null
                ? (designer.getUser().getFullName() != null ? designer.getUser().getFullName() : designer.getUser().getUsername())
                : "Designer";

        req.setDesigner(designer);
        req.setStatus("IN_PROGRESS");
        req.setAcceptedAt(LocalDateTime.now());
        req.setCustomerUnread(true);
        req.setSystemNotification("Designer " + designerName + " đã nhận yêu cầu và đang thực hiện.");
        return designRequestRepository.save(req);
    }

    public DesignRequest submitPreview(Long id, Map<String, Object> payload) {
        User me = currentUserService.getCurrentUser();
        DesignRequest req = designRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu thiết kế #" + id));

        Designer activeDesigner = null;
        if (!currentUserService.isAdmin()) {
            activeDesigner = designerRepository.findByUserId(me.getId())
                    .orElseThrow(() -> new AccessDeniedException("Bạn không có quyền cập nhật yêu cầu này"));
            if (req.getDesigner() != null && !req.getDesigner().getId().equals(activeDesigner.getId())) {
                throw new AccessDeniedException("Yêu cầu này thuộc về Designer khác");
            }
            req.setDesigner(activeDesigner);
        } else {
            activeDesigner = req.getDesigner();
        }

        String previewImageUrl = payload.get("previewImageUrl") != null ? String.valueOf(payload.get("previewImageUrl")).trim() : null;
        String deliveryFileUrl = payload.get("deliveryFileUrl") != null ? String.valueOf(payload.get("deliveryFileUrl")).trim() : null;
        String deliveryFileName = payload.get("deliveryFileName") != null ? String.valueOf(payload.get("deliveryFileName")).trim() : null;
        String designerNote = payload.get("designerNote") != null ? String.valueOf(payload.get("designerNote")).trim() : null;
        String mockupType = payload.get("mockupType") != null ? String.valueOf(payload.get("mockupType")).trim() : null;

        if ((previewImageUrl == null || previewImageUrl.isEmpty()) && (deliveryFileUrl == null || deliveryFileUrl.isEmpty())) {
            throw new IllegalArgumentException("Vui lòng tải lên hình ảnh sản phẩm hoặc tệp thiết kế bàn giao để trả về hệ thống");
        }

        if (previewImageUrl != null && !previewImageUrl.isEmpty()) {
            req.setPreviewImageUrl(previewImageUrl);
        }
        if (deliveryFileUrl != null && !deliveryFileUrl.isEmpty()) {
            req.setDeliveryFileUrl(deliveryFileUrl);
        }
        if (deliveryFileName != null && !deliveryFileName.isEmpty()) {
            req.setDeliveryFileName(deliveryFileName);
        }
        if (designerNote != null) {
            req.setDesignerNote(designerNote);
        }
        if (mockupType != null && !mockupType.isEmpty()) {
            req.setMockupType(mockupType);
        }

        if (payload.get("resultDesignId") != null && !String.valueOf(payload.get("resultDesignId")).isBlank()) {
            Long designId = Long.valueOf(String.valueOf(payload.get("resultDesignId")));
            Design design = designRepository.findById(designId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thiết kế #" + designId));
            req.setResultDesign(design);
        }

        String designerName = (activeDesigner != null && activeDesigner.getUser() != null)
                ? (activeDesigner.getUser().getFullName() != null ? activeDesigner.getUser().getFullName() : activeDesigner.getUser().getUsername())
                : "Designer";

        if (req.getAcceptedAt() == null) {
            req.setAcceptedAt(LocalDateTime.now());
        }
        req.setDeliveredAt(LocalDateTime.now());
        req.setStatus("PREVIEW_READY");
        req.setCustomerUnread(true);
        req.setSystemNotification("Designer " + designerName + " đã bàn giao sản phẩm. Vui lòng kiểm tra và nghiệm thu.");
        return designRequestRepository.save(req);
    }

    public DesignRequest approveRequest(Long id) {
        DesignRequest req = designRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu thiết kế #" + id));

        currentUserService.checkOwnerOrAdmin(req.getCustomer().getId());

        if (!"PREVIEW_READY".equalsIgnoreCase(req.getStatus())) {
            throw new IllegalArgumentException("Yêu cầu chưa có sản phẩm bàn giao để nghiệm thu");
        }

        req.setStatus("APPROVED");
        req.setPaymentStatus("RELEASED");
        req.setReleasedAt(LocalDateTime.now());
        req.setCustomerUnread(false);
        req.setSystemNotification("Đã nghiệm thu sản phẩm và mở khóa file gốc.");
        return designRequestRepository.save(req);
    }

    public DesignRequest requestRevision(Long id, Map<String, Object> payload) {
        DesignRequest req = designRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu thiết kế #" + id));

        currentUserService.checkOwnerOrAdmin(req.getCustomer().getId());

        if (!"PREVIEW_READY".equalsIgnoreCase(req.getStatus())) {
            throw new IllegalArgumentException("Chỉ có thể yêu cầu chỉnh sửa khi Designer đã gửi sản phẩm bàn giao");
        }

        String feedback = (payload != null && payload.get("feedback") != null)
                ? String.valueOf(payload.get("feedback")).trim()
                : "";
        if (feedback.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập nội dung cần chỉnh sửa");
        }

        req.setCustomerFeedback(feedback);
        req.setRevisionCount(req.getRevisionCount() + 1);
        req.setStatus("REVISION_REQUESTED");
        req.setPaymentStatus("ESCROW_HELD");
        req.setCustomerUnread(false);
        req.setSystemNotification("Đã gửi yêu cầu chỉnh sửa cho Designer.");
        return designRequestRepository.save(req);
    }

    public DesignRequest rejectAndRefund(Long id, Map<String, Object> payload) {
        DesignRequest req = designRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu thiết kế #" + id));

        currentUserService.checkOwnerOrAdmin(req.getCustomer().getId());

        if (!"PREVIEW_READY".equalsIgnoreCase(req.getStatus()) && !"REVISION_REQUESTED".equalsIgnoreCase(req.getStatus())) {
            throw new IllegalArgumentException("Chỉ có thể hoàn tiền sau khi kiểm tra sản phẩm bàn giao");
        }

        String reason = (payload != null && payload.get("reason") != null)
                ? String.valueOf(payload.get("reason")).trim()
                : "";
        if (reason.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập lý do từ chối sản phẩm");
        }

        req.setCustomerFeedback(reason);
        req.setStatus("REFUNDED");
        req.setPaymentStatus("REFUNDED");
        req.setReleasedAt(LocalDateTime.now());
        req.setCustomerUnread(false);
        req.setSystemNotification("Đã từ chối sản phẩm và hoàn tiền.");
        return designRequestRepository.save(req);
    }

    public DesignRequest forceReleaseToDesigner(Long id, Map<String, Object> payload) {
        User me = currentUserService.getCurrentUser();
        DesignRequest req = designRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu thiết kế #" + id));

        if (!currentUserService.isAdmin()) {
            Designer designer = designerRepository.findByUserId(me.getId())
                    .orElseThrow(() -> new AccessDeniedException("Chỉ Designer phụ trách hoặc Admin mới có quyền thực hiện"));
            if (req.getDesigner() != null && !req.getDesigner().getId().equals(designer.getId())) {
                throw new AccessDeniedException("Bạn không phải Designer phụ trách yêu cầu này");
            }
        }

        if (!"PREVIEW_READY".equalsIgnoreCase(req.getStatus())) {
            throw new IllegalArgumentException("Chỉ có thể giải ngân khi đã bàn giao sản phẩm");
        }

        req.setStatus("APPROVED");
        req.setPaymentStatus("RELEASED");
        req.setReleasedAt(LocalDateTime.now());
        req.setCustomerUnread(true);
        req.setSystemNotification("Đã hoàn tất yêu cầu và mở khóa file sản phẩm.");
        return designRequestRepository.save(req);
    }

    public DesignRequest cancelRequest(Long id) {
        DesignRequest req = designRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu thiết kế #" + id));

        currentUserService.checkOwnerOrAdmin(req.getCustomer().getId());

        if ("APPROVED".equalsIgnoreCase(req.getStatus())) {
            throw new IllegalArgumentException("Không thể hủy yêu cầu đã hoàn tất");
        }

        if (!currentUserService.isAdmin() && ("IN_PROGRESS".equalsIgnoreCase(req.getStatus())
                || "PREVIEW_READY".equalsIgnoreCase(req.getStatus())
                || "REVISION_REQUESTED".equalsIgnoreCase(req.getStatus()))) {
            throw new IllegalArgumentException("Không thể hủy yêu cầu khi Designer đã nhận làm.");
        }

        req.setStatus("CANCELLED");
        if ("ESCROW_HELD".equalsIgnoreCase(req.getPaymentStatus())) {
            req.setPaymentStatus("REFUNDED");
            req.setReleasedAt(LocalDateTime.now());
            req.setSystemNotification("Đã hủy yêu cầu và hoàn tiền.");
        }
        req.setCustomerUnread(false);
        return designRequestRepository.save(req);
    }

    public void markAllMyNotificationsRead() {
        User me = currentUserService.getCurrentUser();
        List<DesignRequest> list = designRequestRepository.findByCustomerIdOrderByCreatedAtDesc(me.getId());
        for (DesignRequest r : list) {
            if (r.isCustomerUnread()) {
                r.setCustomerUnread(false);
                designRequestRepository.save(r);
            }
        }
    }
}
