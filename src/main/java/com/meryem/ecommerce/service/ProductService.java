package com.meryem.ecommerce.service;

import com.meryem.ecommerce.dto.ProductRequestDto;
import com.meryem.ecommerce.dto.ProductResponseDto;
import com.meryem.ecommerce.entity.Category;
import com.meryem.ecommerce.entity.Product;
import com.meryem.ecommerce.exception.ResourceNotFoundException;
import com.meryem.ecommerce.repository.CategoryRepository;
import com.meryem.ecommerce.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductService(
            ProductRepository productRepository,
            CategoryRepository categoryRepository
    ) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    public List<ProductResponseDto> getAllProducts() {

        List<Product> products = productRepository.findAll();

        List<ProductResponseDto> responseDtos = new ArrayList<>();

        for (Product product : products) {
            responseDtos.add(mapToResponseDto(product));
        }

        return responseDtos;
    }

    public ProductResponseDto getProductById(Long id) {

        Product product = productRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Product not found with id: " + id
                        )
                );

        return mapToResponseDto(product);
    }

    public ProductResponseDto createProduct(
            ProductRequestDto productRequestDto
    ) {

        Category category = categoryRepository
                .findById(productRequestDto.getCategoryId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Category not found with id: "
                                        + productRequestDto.getCategoryId()
                        )
                );

        Product product = new Product();

        product.setName(productRequestDto.getName());
        product.setDescription(productRequestDto.getDescription());
        product.setPrice(productRequestDto.getPrice());
        product.setStock(productRequestDto.getStock());
        product.setCategory(category);

        Product savedProduct = productRepository.save(product);

        return mapToResponseDto(savedProduct);
    }

    public ProductResponseDto updateProduct(
            Long id,
            ProductRequestDto productRequestDto
    ) {

        Product product = productRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Product not found with id: " + id
                        )
                );

        Category category = categoryRepository
                .findById(productRequestDto.getCategoryId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Category not found with id: "
                                        + productRequestDto.getCategoryId()
                        )
                );

        product.setName(productRequestDto.getName());
        product.setDescription(productRequestDto.getDescription());
        product.setPrice(productRequestDto.getPrice());
        product.setStock(productRequestDto.getStock());
        product.setCategory(category);

        Product updatedProduct = productRepository.save(product);

        return mapToResponseDto(updatedProduct);
    }

    public void deleteProduct(Long id) {

        Product product = productRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Product not found with id: " + id
                        )
                );

        productRepository.delete(product);
    }

    private ProductResponseDto mapToResponseDto(Product product) {

        ProductResponseDto responseDto = new ProductResponseDto();

        responseDto.setId(product.getId());
        responseDto.setName(product.getName());
        responseDto.setDescription(product.getDescription());
        responseDto.setPrice(product.getPrice());
        responseDto.setStock(product.getStock());

        if (product.getCategory() != null) {
            responseDto.setCategoryId(
                    product.getCategory().getId()
            );

            responseDto.setCategoryName(
                    product.getCategory().getName()
            );
        }

        return responseDto;
    }
}