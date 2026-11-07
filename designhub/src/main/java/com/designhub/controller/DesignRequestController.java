package com.designhub.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.designhub.entity.DesignRequest;
import com.designhub.service.DesignRequestService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/design-requests")
public class DesignRequestController {

    private final DesignRequestService designRequestService;

    public DesignRequestController(DesignRequestService designRequestService) {
        this.designRequestService = designRequestService;
    }

    @GetMapping
    public ResponseEntity<List<DesignRequest>> getAllRequests() {
        return ResponseEntity.ok(designRequestService.getDesignerRequests());
    }

    @GetMapping("/my")
    public ResponseEntity<List<DesignRequest>> getMyRequests() {
        return ResponseEntity.ok(designRequestService.getMyRequests());
    }

    @GetMapping("/designer")
    public ResponseEntity<List<DesignRequest>> getDesignerRequests() {
        return ResponseEntity.ok(designRequestService.getDesignerRequests());
    }

    @PostMapping
    public ResponseEntity<DesignRequest> createRequest(@Valid @RequestBody DesignRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(designRequestService.createRequest(request));
    }

    @RequestMapping(value = "/{id}/accept", method = {RequestMethod.POST, RequestMethod.PATCH})
    public ResponseEntity<DesignRequest> acceptRequest(@PathVariable Long id) {
        return ResponseEntity.ok(designRequestService.acceptRequest(id));
    }

    @RequestMapping(value = "/{id}/submit-preview", method = {RequestMethod.POST, RequestMethod.PATCH})
    public ResponseEntity<DesignRequest> submitPreview(
            @PathVariable Long id,
            @RequestBody Map<String, Object> payload) {
        return ResponseEntity.ok(designRequestService.submitPreview(id, payload));
    }

    @RequestMapping(value = "/{id}/approve", method = {RequestMethod.POST, RequestMethod.PATCH})
    public ResponseEntity<DesignRequest> approveRequest(@PathVariable Long id) {
        return ResponseEntity.ok(designRequestService.approveRequest(id));
    }

    @RequestMapping(value = "/{id}/pay-escrow", method = {RequestMethod.POST, RequestMethod.PATCH})
    public ResponseEntity<DesignRequest> payEscrow(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> payload) {
        return ResponseEntity.ok(designRequestService.payEscrow(id, payload));
    }

    @RequestMapping(value = "/{id}/request-revision", method = {RequestMethod.POST, RequestMethod.PATCH})
    public ResponseEntity<DesignRequest> requestRevision(
            @PathVariable Long id,
            @RequestBody Map<String, Object> payload) {
        return ResponseEntity.ok(designRequestService.requestRevision(id, payload));
    }

    @RequestMapping(value = "/{id}/reject-refund", method = {RequestMethod.POST, RequestMethod.PATCH})
    public ResponseEntity<DesignRequest> rejectAndRefund(
            @PathVariable Long id,
            @RequestBody Map<String, Object> payload) {
        return ResponseEntity.ok(designRequestService.rejectAndRefund(id, payload));
    }

    @RequestMapping(value = "/{id}/force-release", method = {RequestMethod.POST, RequestMethod.PATCH})
    public ResponseEntity<DesignRequest> forceReleaseToDesigner(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> payload) {
        return ResponseEntity.ok(designRequestService.forceReleaseToDesigner(id, payload));
    }

    @RequestMapping(value = "/{id}/cancel", method = {RequestMethod.POST, RequestMethod.PATCH})
    public ResponseEntity<DesignRequest> cancelRequest(@PathVariable Long id) {
        return ResponseEntity.ok(designRequestService.cancelRequest(id));
    }

    @RequestMapping(value = "/mark-read", method = {RequestMethod.POST, RequestMethod.PATCH})
    public ResponseEntity<Map<String, Object>> markNotificationsRead() {
        designRequestService.markAllMyNotificationsRead();
        return ResponseEntity.ok(Map.of("success", true));
    }
}
