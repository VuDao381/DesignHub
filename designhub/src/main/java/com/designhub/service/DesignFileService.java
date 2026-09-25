package com.designhub.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.designhub.entity.DesignFile;
import com.designhub.repository.DesignFileRepository;

@Service
public class DesignFileService {

    private final DesignFileRepository designFileRepository;

    public DesignFileService(DesignFileRepository designFileRepository) {
        this.designFileRepository = designFileRepository;
    }

    public List<DesignFile> getAllFiles() {
        return designFileRepository.findAll();
    }

    public List<DesignFile> getFilesByDesignId(Long designId) {
        return designFileRepository.findByDesignId(designId);
    }

    public Optional<DesignFile> getFileById(Long id) {
        return designFileRepository.findById(id);
    }

    public DesignFile createFile(DesignFile designFile) {
        return designFileRepository.save(designFile);
    }

    public Optional<DesignFile> updateFile(
            Long id,
            DesignFile designFile) {

        return designFileRepository.findById(id)
                .map(existingFile -> {
                    existingFile.setFileName(designFile.getFileName());
                    existingFile.setFileUrl(designFile.getFileUrl());
                    existingFile.setFileSize(designFile.getFileSize());
                    existingFile.setFileType(designFile.getFileType());
                    existingFile.setDesign(designFile.getDesign());

                    return designFileRepository.save(existingFile);
                });
    }

    public boolean deleteFile(Long id) {
        if (!designFileRepository.existsById(id)) {
            return false;
        }

        designFileRepository.deleteById(id);
        return true;
    }
}
