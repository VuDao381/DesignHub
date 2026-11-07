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
            req.setSystemNotification("Yêu cầu đã được khởi tạo. Vui lòng hoàn tất Thanh toán tạm giữ (Escrow) để hệ thống kích hoạt đẩy yêu cầu tới các Designer.");
        } else {
            String txCode = "ESCROW-DH-" + System.currentTimeMillis();
            req.setPaymentStatus("ESCROW_HELD");
            req.setTransactionCode(txCode);
            req.setPaidAt(LocalDateTime.now());
            req.setStatus("OPEN");
            req.setSystemNotification("Hệ thống đã nhận & tạm giữ 100% khoản thanh toán bảo đảm (Escrow " + method + " - Mã GD: " + txCode + "). Yêu cầu của bạn đã được đẩy lên hệ thống để Designer phù hợp nhận làm.");
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
        String txCode = "ESCROW-DH-" + System.currentTimeMillis();

        req.setPaymentMethod(method);
        req.setPaymentStatus("ESCROW_HELD");
        req.setTransactionCode(txCode);
        req.setPaidAt(LocalDateTime.now());
        if ("PENDING_PAYMENT".equalsIgnoreCase(req.getStatus())) {
            req.setStatus("OPEN");
        }
        req.setCustomerUnread(false);
        req.setSystemNotification("Thanh toán tạm giữ thành công (Mã GD: " + txCode + "). Hệ thống đang giữ tiền bảo đảm và đã mở yêu cầu cho Designer nhận làm.");
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
            throw new IllegalArgumentException("Yêu cầu này khách hàng chưa thanh toán tạm giữ (Escrow) nên chưa thể nhận làm");
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
        req.setSystemNotification("Hệ thống thông báo: Yêu cầu #" + req.getId() + " (\"" + req.getTitle() + "\") của bạn đã được Designer " + designerName + " tiếp nhận! Khoản tiền tạm giữ (Escrow) đang được hệ thống bảo đảm (khách hàng không thể tự ý hủy ngang khi Designer đang thực hiện).");
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
        req.setSystemNotification("Hệ thống thông báo: Hệ thống đã nhận được sản phẩm hoàn thiện từ Designer " + designerName + " cho yêu cầu #" + req.getId() + " (\"" + req.getTitle() + "\") và chuyển về cho bạn kiểm tra! Nếu sản phẩm đúng yêu cầu, hãy bấm [Nghiệm thu & Giải ngân] để tải File gốc; nếu chưa đúng yêu cầu, bạn có thể [Yêu cầu chỉnh sửa] hoặc [Báo sai yêu cầu & Hoàn tiền 100%].");
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
        req.setSystemNotification("Hoàn tất nghiệm thu! Hệ thống đã mở khóa toàn bộ File thiết kế gốc cho bạn tải về và giải ngân 100% tiền tạm giữ (Escrow) cho Designer.");
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
            throw new IllegalArgumentException("Vui lòng nhập nội dung chưa đúng yêu cầu cần Designer chỉnh sửa lại");
        }

        req.setCustomerFeedback(feedback);
        req.setRevisionCount(req.getRevisionCount() + 1);
        req.setStatus("REVISION_REQUESTED");
        req.setPaymentStatus("ESCROW_HELD");
        req.setCustomerUnread(false);
        req.setSystemNotification("Bạn đã gửi yêu cầu chỉnh sửa lần #" + req.getRevisionCount() + " (\"" + feedback + "\"). Hệ thống vẫn đang tạm giữ an toàn khoản tiền ký quỹ của bạn và đã thông báo cho Designer chỉnh sửa lại.");
        return designRequestRepository.save(req);
    }

    public DesignRequest rejectAndRefund(Long id, Map<String, Object> payload) {
        DesignRequest req = designRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu thiết kế #" + id));

        currentUserService.checkOwnerOrAdmin(req.getCustomer().getId());

        if (!"PREVIEW_READY".equalsIgnoreCase(req.getStatus()) && !"REVISION_REQUESTED".equalsIgnoreCase(req.getStatus())) {
            throw new IllegalArgumentException("Chỉ có thể báo sai yêu cầu & hoàn tiền sau khi kiểm tra sản phẩm bàn giao");
        }

        String reason = (payload != null && payload.get("reason") != null)
                ? String.valueOf(payload.get("reason")).trim()
                : "";
        if (reason.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập lý do sản phẩm không đúng với yêu cầu ban đầu để hệ thống hoàn tiền");
        }

        req.setCustomerFeedback(reason);
        req.setStatus("REFUNDED");
        req.setPaymentStatus("REFUNDED");
        req.setReleasedAt(LocalDateTime.now());
        req.setCustomerUnread(false);
        req.setSystemNotification("Bảo vệ Khách hàng: Do sản phẩm không đúng với yêu cầu ban đầu (\"" + reason + "\"), hệ thống đã thu hồi quyền tải bản vẽ và HOÀN TRẢ 100% số tiền ký quỹ về tài khoản của bạn!");
        return designRequestRepository.save(req);
    }

    public DesignRequest forceReleaseToDesigner(Long id, Map<String, Object> payload) {
        User me = currentUserService.getCurrentUser();
        DesignRequest req = designRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu thiết kế #" + id));

        if (!currentUserService.isAdmin()) {
            Designer designer = designerRepository.findByUserId(me.getId())
                    .orElseThrow(() -> new AccessDeniedException("Chỉ Designer phụ trách hoặc Admin mới có quyền thực hiện"));
            if (req.getDesigner() == null || !req.getDesigner().getId().equals(designer.getId())) {
                throw new AccessDeniedException("Bạn không phải Designer phụ trách yêu cầu này");
            }
        }

        if (!"PREVIEW_READY".equalsIgnoreCase(req.getStatus())) {
            throw new IllegalArgumentException("Chỉ có thể giải ngân bảo vệ Designer khi đã trả đầy đủ sản phẩm về hệ thống");
        }

        String note = (payload != null && payload.get("note") != null && !String.valueOf(payload.get("note")).isBlank())
                ? String.valueOf(payload.get("note")).trim()
                : "Sản phẩm đã được hoàn thiện đúng theo mô tả yêu cầu ban đầu nhưng khách hàng không bấm nhận.";

        req.setStatus("APPROVED");
        req.setPaymentStatus("RELEASED");
        req.setReleasedAt(LocalDateTime.now());
        req.setCustomerUnread(true);
        req.setSystemNotification("Bảo vệ quyền lợi Designer: Hệ thống xác nhận sản phẩm đã làm đúng yêu cầu cam kết (\"" + note + "\"). Hệ thống đã tự động giải ngân tiền tạm giữ (Escrow) cho Designer và mở khóa file sản phẩm cho bạn.");
        return designRequestRepository.save(req);
    }

    public DesignRequest cancelRequest(Long id) {
        DesignRequest req = designRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu thiết kế #" + id));

        currentUserService.checkOwnerOrAdmin(req.getCustomer().getId());

        if ("APPROVED".equalsIgnoreCase(req.getStatus())) {
            throw new IllegalArgumentException("Không thể hủy yêu cầu đã được nghiệm thu hoàn tất");
        }

        if (!currentUserService.isAdmin() && ("IN_PROGRESS".equalsIgnoreCase(req.getStatus())
                || "PREVIEW_READY".equalsIgnoreCase(req.getStatus())
                || "REVISION_REQUESTED".equalsIgnoreCase(req.getStatus()))) {
            throw new IllegalArgumentException("Không thể tự ý hủy ngang khi Designer đã nhận làm để bảo vệ công sức Designer! Nếu sản phẩm bàn giao không đúng yêu cầu, bạn có quyền bấm 'Yêu cầu sửa lại' hoặc 'Báo sai yêu cầu & Hoàn tiền 100%' ở bước nghiệm thu.");
        }

        req.setStatus("CANCELLED");
        if ("ESCROW_HELD".equalsIgnoreCase(req.getPaymentStatus())) {
            req.setPaymentStatus("REFUNDED");
            req.setReleasedAt(LocalDateTime.now());
            req.setSystemNotification("Bạn đã hủy yêu cầu trước khi có Designer nhận làm. Hệ thống đã hoàn trả 100% số tiền ký quỹ về tài khoản của bạn.");
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
