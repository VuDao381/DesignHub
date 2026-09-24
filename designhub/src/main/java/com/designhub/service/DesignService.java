package com.designhub.service;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.designhub.entity.Design;
import com.designhub.repository.DesignRepository;

@Service
public class DesignService {

    private final DesignRepository designRepository;

    public DesignService(DesignRepository designRepository) {
        this.designRepository = designRepository;
    }

    public Page<Design> getDesigns(String keyword, Pageable pageable) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return designRepository.findAll(pageable);
        }

        return designRepository.findByNameContainingIgnoreCase(
                keyword.trim(),
                pageable
        );
    }

    public Optional<Design> getDesignById(Long id) {
        return designRepository.findById(id);
    }

    public Design createDesign(Design design) {
        return designRepository.save(design);
    }

    public Optional<Design> updateDesign(Long id, Design design) {
        return designRepository.findById(id)
                .map(existingDesign -> {
                    existingDesign.setName(design.getName());
                    existingDesign.setDescription(design.getDescription());
                    existingDesign.setPrice(design.getPrice());
                    existingDesign.setStatus(design.getStatus());
                    existingDesign.setCategory(design.getCategory());
                    existingDesign.setDesigner(design.getDesigner());

                    return designRepository.save(existingDesign);
                });
    }

    public boolean deleteDesign(Long id) {
        if (!designRepository.existsById(id)) {
            return false;
        }

        designRepository.deleteById(id);
        return true;
    }
}
