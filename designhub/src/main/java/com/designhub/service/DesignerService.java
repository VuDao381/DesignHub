package com.designhub.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.designhub.entity.Designer;
import com.designhub.repository.DesignerRepository;

@Service
public class DesignerService {

    private final DesignerRepository designerRepository;

    public DesignerService(DesignerRepository designerRepository) {
        this.designerRepository = designerRepository;
    }

    public List<Designer> getAllDesigners() {
        return designerRepository.findAll();
    }

    public Optional<Designer> getDesignerById(Long id) {
        return designerRepository.findById(id);
    }

    public Designer createDesigner(Designer designer) {
        return designerRepository.save(designer);
    }

    public Optional<Designer> updateDesigner(Long id, Designer designer) {
        return designerRepository.findById(id)
                .map(existingDesigner -> {
                    existingDesigner.setBio(designer.getBio());
                    existingDesigner.setAvatar(designer.getAvatar());
                    existingDesigner.setUser(designer.getUser());

                    return designerRepository.save(existingDesigner);
                });
    }

    public boolean deleteDesigner(Long id) {
        if (!designerRepository.existsById(id)) {
            return false;
        }

        designerRepository.deleteById(id);
        return true;
    }
}
