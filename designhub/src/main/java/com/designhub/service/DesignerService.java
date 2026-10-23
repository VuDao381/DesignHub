package com.designhub.service;

import java.util.List;
import java.util.Optional;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.designhub.dto.RegisterDesignerRequest;
import com.designhub.entity.Designer;
import com.designhub.entity.Role;
import com.designhub.entity.User;
import com.designhub.exception.ResourceNotFoundException;
import com.designhub.repository.DesignRepository;
import com.designhub.repository.DesignerRepository;
import com.designhub.repository.UserRepository;
import com.designhub.security.CurrentUserService;

@Service
@Transactional
public class DesignerService {

    private final DesignerRepository designerRepository;
    private final DesignRepository designRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;

    public DesignerService(
            DesignerRepository designerRepository,
            DesignRepository designRepository,
            UserRepository userRepository,
            CurrentUserService currentUserService) {
        this.designerRepository = designerRepository;
        this.designRepository = designRepository;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public List<Designer> getAllDesigners() {
        return designerRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Designer> getDesignerById(Long id) {
        return designerRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Designer> getDesignerByUserId(Long userId) {
        return designerRepository.findByUserId(userId);
    }

    public Designer createDesigner(Designer designer) {
        if (!currentUserService.isAdmin()) {
            throw new AccessDeniedException("Chỉ ADMIN mới có quyền tạo hồ sơ Designer trực tiếp");
        }
        if (designer.getUser() == null || designer.getUser().getId() == null) {
            throw new IllegalArgumentException("Hồ sơ Designer phải liên kết với User hợp lệ");
        }
        User user = userRepository.findById(designer.getUser().getId())
                .orElseThrow(() -> new ResourceNotFoundException("User không tồn tại"));

        if (designerRepository.findByUserId(user.getId()).isPresent()) {
            throw new IllegalArgumentException("User này đã có hồ sơ Designer");
        }

        designer.setId(null);
        designer.setUser(user);
        if (user.getRole() != Role.DESIGNER && user.getRole() != Role.ADMIN) {
            user.setRole(Role.DESIGNER);
            userRepository.save(user);
        }
        return designerRepository.save(designer);
    }

    public Designer registerAsDesigner(RegisterDesignerRequest request) {
        User me = currentUserService.getCurrentUser();
        if (designerRepository.findByUserId(me.getId()).isPresent()) {
            throw new IllegalArgumentException("Bạn đã có hồ sơ Designer rồi");
        }

        Designer designer = new Designer();
        designer.setId(null);
        designer.setUser(me);
        if (request != null) {
            designer.setBio(request.bio());
            designer.setAvatar(request.avatar());
        }

        if (me.getRole() != Role.DESIGNER && me.getRole() != Role.ADMIN) {
            me.setRole(Role.DESIGNER);
            userRepository.save(me);
        }

        return designerRepository.save(designer);
    }

    public Optional<Designer> updateDesigner(Long id, Designer designer) {
        return designerRepository.findById(id)
                .map(existingDesigner -> {
                    currentUserService.checkOwnerOrAdmin(existingDesigner.getUser().getId());

                    existingDesigner.setBio(designer.getBio());
                    existingDesigner.setAvatar(designer.getAvatar());

                    return designerRepository.save(existingDesigner);
                });
    }

    public boolean deleteDesigner(Long id) {
        if (!currentUserService.isAdmin()) {
            throw new AccessDeniedException("Chỉ ADMIN mới có quyền xoá hồ sơ Designer");
        }

        Optional<Designer> designerOpt = designerRepository.findById(id);
        if (designerOpt.isEmpty()) {
            return false;
        }

        if (designRepository.existsByDesignerId(id)) {
            throw new IllegalStateException("Không thể xóa Designer khi vẫn còn các thiết kế liên kết trong hệ thống. Vui lòng chuyển giao hoặc xóa các thiết kế trước.");
        }

        Designer designer = designerOpt.get();
        if (designer.getUser() != null && designer.getUser().getRole() == Role.DESIGNER) {
            designer.getUser().setRole(Role.CUSTOMER);
            userRepository.save(designer.getUser());
        }

        designerRepository.delete(designer);
        return true;
    }
}
