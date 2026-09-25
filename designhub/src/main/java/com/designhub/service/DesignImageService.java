package com.designhub.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.designhub.entity.DesignImage;
import com.designhub.repository.DesignImageRepository;

@Service
public class DesignImageService {

    private final DesignImageRepository designImageRepository;

    public DesignImageService(DesignImageRepository designImageRepository) {
        this.designImageRepository = designImageRepository;
    }

    public List<DesignImage> getAllImages() {
        return designImageRepository.findAll();
    }

    public List<DesignImage> getImagesByDesignId(Long designId) {
        return designImageRepository.findByDesignId(designId);
    }

    public Optional<DesignImage> getImageById(Long id) {
        return designImageRepository.findById(id);
    }

    public DesignImage createImage(DesignImage designImage) {
        return designImageRepository.save(designImage);
    }

    public Optional<DesignImage> updateImage(
            Long id,
            DesignImage designImage) {

        return designImageRepository.findById(id)
                .map(existingImage -> {
                    existingImage.setImageUrl(designImage.getImageUrl());
                    existingImage.setPrimary(designImage.isPrimary());
                    existingImage.setDesign(designImage.getDesign());

                    return designImageRepository.save(existingImage);
                });
    }

    public boolean deleteImage(Long id) {
        if (!designImageRepository.existsById(id)) {
            return false;
        }

        designImageRepository.deleteById(id);
        return true;
    }
}
