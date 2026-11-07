package com.designhub.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "design_requests")
public class DesignRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Tiêu đề yêu cầu thiết kế không được để trống")
    @Size(max = 255)
    @Column(nullable = false, length = 255)
    private String title;

    @NotBlank(message = "Mô tả yêu cầu thiết kế không được để trống")
    @Column(name = "product_description", nullable = false, columnDefinition = "TEXT")
    private String productDescription;

    @Size(max = 500)
    @Column(name = "reference_image_url", length = 500)
    private String referenceImageUrl;

    @NotNull(message = "Ngân sách dự kiến không được để trống")
    @DecimalMin(value = "0.0", inclusive = true, message = "Ngân sách không hợp lệ")
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal budget;

    @Size(max = 100)
    @Column(length = 100)
    private String deadline;

    @Size(max = 50)
    @Column(name = "mockup_type", length = 50)
    private String mockupType = "STANDARD_2D";

    @Size(max = 50)
    @Column(nullable = false, length = 50)
    private String status = "OPEN";

    @Size(max = 500)
    @Column(name = "preview_image_url", length = 500)
    private String previewImageUrl;

    @Size(max = 500)
    @Column(name = "delivery_file_url", length = 500)
    private String deliveryFileUrl;

    @Size(max = 255)
    @Column(name = "delivery_file_name", length = 255)
    private String deliveryFileName;

    @Size(max = 1000)
    @Column(name = "designer_note", length = 1000)
    private String designerNote;

    @Size(max = 1000)
    @Column(name = "system_notification", length = 1000)
    private String systemNotification;

    @Column(name = "customer_unread", nullable = false)
    private boolean customerUnread = false;

    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Size(max = 50)
    @Column(name = "payment_status", nullable = false, length = 50)
    private String paymentStatus = "ESCROW_HELD";

    @Size(max = 50)
    @Column(name = "payment_method", length = 50)
    private String paymentMethod = "VNPAY";

    @Size(max = 100)
    @Column(name = "transaction_code", length = 100)
    private String transactionCode;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "released_at")
    private LocalDateTime releasedAt;

    @Size(max = 1000)
    @Column(name = "customer_feedback", length = 1000)
    private String customerFeedback;

    @Column(name = "revision_count", nullable = false)
    private int revisionCount = 0;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "designer_id")
    private Designer designer;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "result_design_id")
    private Design resultDesign;

    @Transient
    private Long categoryId;

    @Transient
    private Long designerId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public DesignRequest() {
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null || this.status.isBlank()) {
            this.status = "OPEN";
        }
        if (this.mockupType == null || this.mockupType.isBlank()) {
            this.mockupType = "STANDARD_2D";
        }
        if (this.paymentStatus == null || this.paymentStatus.isBlank()) {
            this.paymentStatus = "ESCROW_HELD";
        }
        if (this.paymentMethod == null || this.paymentMethod.isBlank()) {
            this.paymentMethod = "VNPAY";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getProductDescription() {
        return productDescription;
    }

    public void setProductDescription(String productDescription) {
        this.productDescription = productDescription;
    }

    public String getReferenceImageUrl() {
        return referenceImageUrl;
    }

    public void setReferenceImageUrl(String referenceImageUrl) {
        this.referenceImageUrl = referenceImageUrl;
    }

    public BigDecimal getBudget() {
        return budget;
    }

    public void setBudget(BigDecimal budget) {
        this.budget = budget;
    }

    public String getDeadline() {
        return deadline;
    }

    public void setDeadline(String deadline) {
        this.deadline = deadline;
    }

    public String getMockupType() {
        return mockupType;
    }

    public void setMockupType(String mockupType) {
        this.mockupType = mockupType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPreviewImageUrl() {
        return previewImageUrl;
    }

    public void setPreviewImageUrl(String previewImageUrl) {
        this.previewImageUrl = previewImageUrl;
    }

    public String getDeliveryFileUrl() {
        return deliveryFileUrl;
    }

    public void setDeliveryFileUrl(String deliveryFileUrl) {
        this.deliveryFileUrl = deliveryFileUrl;
    }

    public String getDeliveryFileName() {
        return deliveryFileName;
    }

    public void setDeliveryFileName(String deliveryFileName) {
        this.deliveryFileName = deliveryFileName;
    }

    public String getDesignerNote() {
        return designerNote;
    }

    public void setDesignerNote(String designerNote) {
        this.designerNote = designerNote;
    }

    public String getSystemNotification() {
        return systemNotification;
    }

    public void setSystemNotification(String systemNotification) {
        this.systemNotification = systemNotification;
    }

    public boolean isCustomerUnread() {
        return customerUnread;
    }

    public void setCustomerUnread(boolean customerUnread) {
        this.customerUnread = customerUnread;
    }

    public LocalDateTime getAcceptedAt() {
        return acceptedAt;
    }

    public void setAcceptedAt(LocalDateTime acceptedAt) {
        this.acceptedAt = acceptedAt;
    }

    public LocalDateTime getDeliveredAt() {
        return deliveredAt;
    }

    public void setDeliveredAt(LocalDateTime deliveredAt) {
        this.deliveredAt = deliveredAt;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getTransactionCode() {
        return transactionCode;
    }

    public void setTransactionCode(String transactionCode) {
        this.transactionCode = transactionCode;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(LocalDateTime paidAt) {
        this.paidAt = paidAt;
    }

    public LocalDateTime getReleasedAt() {
        return releasedAt;
    }

    public void setReleasedAt(LocalDateTime releasedAt) {
        this.releasedAt = releasedAt;
    }

    public String getCustomerFeedback() {
        return customerFeedback;
    }

    public void setCustomerFeedback(String customerFeedback) {
        this.customerFeedback = customerFeedback;
    }

    public int getRevisionCount() {
        return revisionCount;
    }

    public void setRevisionCount(int revisionCount) {
        this.revisionCount = revisionCount;
    }

    public User getCustomer() {
        return customer;
    }

    public void setCustomer(User customer) {
        this.customer = customer;
    }

    public Designer getDesigner() {
        return designer;
    }

    public void setDesigner(Designer designer) {
        this.designer = designer;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public Design getResultDesign() {
        return resultDesign;
    }

    public void setResultDesign(Design resultDesign) {
        this.resultDesign = resultDesign;
    }

    public Long getCategoryId() {
        return categoryId != null ? categoryId : (category != null ? category.getId() : null);
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public Long getDesignerId() {
        return designerId != null ? designerId : (designer != null ? designer.getId() : null);
    }

    public void setDesignerId(Long designerId) {
        this.designerId = designerId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
