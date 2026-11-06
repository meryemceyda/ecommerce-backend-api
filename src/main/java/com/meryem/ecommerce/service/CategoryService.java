package com.meryem.ecommerce.service;

import com.meryem.ecommerce.dto.CategoryRequestDto;
import com.meryem.ecommerce.dto.CategoryResponseDto;
import com.meryem.ecommerce.entity.Category;
import com.meryem.ecommerce.exception.ResourceNotFoundException;
import com.meryem.ecommerce.repository.CategoryRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public List<CategoryResponseDto> getAllCategories() {

        List<Category> categories = categoryRepository.findAll();
        List<CategoryResponseDto> responseDtos = new ArrayList<>();

        for (Category category : categories) {
            responseDtos.add(mapToResponseDto(category));
        }

        return responseDtos;
    }

    public CategoryResponseDto getCategoryById(Long id) {

        Category category = categoryRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Category not found with id: " + id
                        )
                );

        return mapToResponseDto(category);
    }

    public CategoryResponseDto createCategory(CategoryRequestDto dto) {

        Category category = new Category();

        category.setName(dto.getName());
        category.setDescription(dto.getDescription());

        Category savedCategory = categoryRepository.save(category);

        return mapToResponseDto(savedCategory);
    }

    public CategoryResponseDto updateCategory(
            Long id,
            CategoryRequestDto dto
    ) {

        Category category = categoryRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Category not found with id: " + id
                        )
                );

        category.setName(dto.getName());
        category.setDescription(dto.getDescription());

        Category updatedCategory = categoryRepository.save(category);

        return mapToResponseDto(updatedCategory);
    }

    public void deleteCategory(Long id) {

        Category category = categoryRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Category not found with id: " + id
                        )
                );

        categoryRepository.delete(category);
    }

    private CategoryResponseDto mapToResponseDto(Category category) {

        CategoryResponseDto responseDto = new CategoryResponseDto();

        responseDto.setId(category.getId());
        responseDto.setName(category.getName());
        responseDto.setDescription(category.getDescription());

        return responseDto;
    }
}