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

import com.designhub.entity.Designer;
import com.designhub.service.DesignerService;

@RestController
@RequestMapping("/api/designers")
public class DesignerController {

    private final DesignerService designerService;

    public DesignerController(DesignerService designerService) {
        this.designerService = designerService;
    }

    @GetMapping
    public ResponseEntity<List<Designer>> getAllDesigners() {
        return ResponseEntity.ok(
                designerService.getAllDesigners()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<Designer> getDesignerById(
            @PathVariable Long id) {

        return designerService.getDesignerById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Designer> createDesigner(
            @RequestBody Designer designer) {

        return ResponseEntity.ok(
                designerService.createDesigner(designer)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<Designer> updateDesigner(
            @PathVariable Long id,
            @RequestBody Designer designer) {

        return designerService.updateDesigner(id, designer)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDesigner(
            @PathVariable Long id) {

        if (!designerService.deleteDesigner(id)) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.noContent().build();
    }
}
