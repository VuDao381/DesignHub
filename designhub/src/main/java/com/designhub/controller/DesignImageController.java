package com.designhub.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.designhub.entity.DesignImage;
import com.designhub.service.DesignImageService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/design-images")
public class DesignImageController {

    private final DesignImageService designImageService;

    public DesignImageController(DesignImageService designImageService) {
        this.designImageService = designImageService;
    }

    @GetMapping
    public ResponseEntity<List<DesignImage>> getAllImages() {
        return ResponseEntity.ok(
                designImageService.getAllImages()
        );
    }

    @GetMapping("/design/{designId}")
    public ResponseEntity<List<DesignImage>> getImagesByDesignId(
            @PathVariable Long designId) {

        return ResponseEntity.ok(
                designImageService.getImagesByDesignId(designId)
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<DesignImage> getImageById(
            @PathVariable Long id) {

        return designImageService.getImageById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<DesignImage> createImage(
            @Valid @RequestBody DesignImage designImage) {

        return ResponseEntity.ok(
                designImageService.createImage(designImage)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<DesignImage> updateImage(
            @PathVariable Long id,
            @Valid @RequestBody DesignImage designImage) {

        return designImageService.updateImage(id, designImage)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteImage(
            @PathVariable Long id) {

        if (!designImageService.deleteImage(id)) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.noContent().build();
    }
}
