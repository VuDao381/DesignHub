package com.designhub.service;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import com.designhub.entity.Category;
import com.designhub.repository.CategoryRepository;

@Service
@Transactional
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public Page<Category> getCategories(String keyword, Pageable pageable) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return categoryRepository.findAll(pageable);
        }

        return categoryRepository.findByNameContainingIgnoreCase(
                keyword.trim(),
                pageable
        );
    }

    @Transactional(readOnly = true)
    public Optional<Category> getCategoryById(Long id) {
        return categoryRepository.findById(id);
    }

    public Category createCategory(Category category) {
        if (categoryRepository.existsByNameIgnoreCase(category.getName().trim())) {
            throw new IllegalArgumentException("Tên danh mục đã tồn tại");
        }

        category.setId(null);
        category.setName(category.getName().trim());

        if (category.getDescription() != null) {
            category.setDescription(category.getDescription().trim());
        }

        return categoryRepository.save(category);
    }

    public Optional<Category> updateCategory(Long id, Category category) {
        return categoryRepository.findById(id)
                .map(existingCategory -> {
                    String newName = category.getName().trim();

                    if (!existingCategory.getName().equalsIgnoreCase(newName)
                            && categoryRepository.existsByNameIgnoreCase(newName)) {
                        throw new IllegalArgumentException("Tên danh mục đã tồn tại");
                    }

                    existingCategory.setName(newName);

                    if (category.getDescription() != null) {
                        existingCategory.setDescription(
                                category.getDescription().trim()
                        );
                    } else {
                        existingCategory.setDescription(null);
                    }

                    return categoryRepository.save(existingCategory);
                });
    }

    public boolean deleteCategory(Long id) {
        if (!categoryRepository.existsById(id)) {
            return false;
        }

        categoryRepository.deleteById(id);
        return true;
    }
}
