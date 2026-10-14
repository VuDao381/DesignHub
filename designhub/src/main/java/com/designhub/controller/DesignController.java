package com.designhub.controller;

import java.math.BigDecimal;

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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/designs")
@Tag(name = "Designs", description = "API quản lý và tìm kiếm thiết kế")
public class DesignController {

    private final DesignService designService;

    public DesignController(DesignService designService) {
        this.designService = designService;
    }

    @GetMapping
    @Operation(summary = "Tìm kiếm & lọc danh sách thiết kế",
               description = "Tìm kiếm theo từ khóa (tên, mô tả), lọc theo danh mục, designer, khoảng giá và sắp xếp.")
    public ResponseEntity<Page<Design>> getDesigns(
            @Parameter(description = "Từ khóa tìm kiếm trong tên hoặc mô tả")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "ID danh mục")
            @RequestParam(required = false) Long categoryId,
            @Parameter(description = "ID Designer")
            @RequestParam(required = false) Long designerId,
            @Parameter(description = "Giá tối thiểu")
            @RequestParam(required = false) BigDecimal minPrice,
            @Parameter(description = "Giá tối đa")
            @RequestParam(required = false) BigDecimal maxPrice,
            @Parameter(description = "Tiêu chí sắp xếp: createdAt_desc (mới nhất), createdAt_asc (cũ nhất), price_asc (giá tăng), price_desc (giá giảm), name_asc (A-Z)")
            @RequestParam(required = false, defaultValue = "createdAt_desc") String sortBy,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        int validPage = Math.max(page, 0);
        int validSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(validPage, validSize);

        return ResponseEntity.ok(
                designService.searchAndFilterDesigns(
                        keyword,
                        categoryId,
                        designerId,
                        minPrice,
                        maxPrice,
                        sortBy,
                        pageable)
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
