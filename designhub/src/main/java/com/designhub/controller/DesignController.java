package com.designhub.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.designhub.entity.Design;
import com.designhub.service.DesignService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/designs")
public class DesignController {

    private final DesignService designService;

    public DesignController(DesignService designService) {
        this.designService = designService;
    }

    @GetMapping
    public ResponseEntity<Page<Design>> getDesigns(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);

        return ResponseEntity.ok(
                designService.getDesigns(keyword, pageable)
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<Design> getDesignById(
            @PathVariable Long id) {

        return designService.getDesignById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Design> createDesign(
            @Valid @RequestBody Design design) {

        return ResponseEntity.ok(
                designService.createDesign(design)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<Design> updateDesign(
            @PathVariable Long id,
            @Valid @RequestBody Design design) {

        return designService.updateDesign(id, design)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDesign(
            @PathVariable Long id) {

        if (!designService.deleteDesign(id)) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.noContent().build();
    }
}
