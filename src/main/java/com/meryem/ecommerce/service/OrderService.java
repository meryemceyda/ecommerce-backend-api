package com.meryem.ecommerce.service;

import com.meryem.ecommerce.dto.*;
import com.meryem.ecommerce.entity.Order;
import com.meryem.ecommerce.entity.OrderItem;
import com.meryem.ecommerce.entity.Product;
import com.meryem.ecommerce.entity.user;
import com.meryem.ecommerce.enums.OrderStatus;
import com.meryem.ecommerce.exception.InsufficientStockException;
import com.meryem.ecommerce.exception.InvalidOrderStatusException;
import com.meryem.ecommerce.exception.ResourceNotFoundException;
import com.meryem.ecommerce.repository.OrderRepository;
import com.meryem.ecommerce.repository.ProductRepository;
import com.meryem.ecommerce.repository.UserRepository;

import jakarta.transaction.Transactional;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    public OrderService(
            OrderRepository orderRepository,
            UserRepository userRepository,
            ProductRepository productRepository
    ) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public OrderResponseDto createOrder(
            OrderRequestDto dto,
            Long currentUserId
    ) {

        user currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found"
                ));

        Order order = new Order();

        order.setUser(currentUser);
        order.setStatus(OrderStatus.CREATED);
        order.setCreatedAt(LocalDateTime.now());

        BigDecimal totalPrice = BigDecimal.ZERO;

        for (OrderItemRequestDto itemDto : dto.getItems()) {

            Product product = productRepository
                    .findById(itemDto.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Product not found with id: "
                                    + itemDto.getProductId()
                    ));

            if (product.getStock() < itemDto.getQuantity()) {
                throw new InsufficientStockException(
                        "Insufficient stock for product id: "
                                + product.getId()
                );
            }

            OrderItem orderItem = new OrderItem();

            orderItem.setProduct(product);
            orderItem.setQuantity(itemDto.getQuantity());
            orderItem.setUnitPrice(product.getPrice());

            order.addItem(orderItem);

            product.setStock(
                    product.getStock() - itemDto.getQuantity()
            );

            BigDecimal lineTotal = product.getPrice()
                    .multiply(
                            BigDecimal.valueOf(itemDto.getQuantity())
                    );

            totalPrice = totalPrice.add(lineTotal);
        }

        order.setTotalPrice(totalPrice);

        Order savedOrder = orderRepository.save(order);

        return mapToResponseDto(savedOrder);
    }

    @Transactional
    public OrderResponseDto getOrderById(
            Long orderId,
            Long currentUserId,
            boolean isAdmin
    ) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order not found"
                ));

        if (!isAdmin &&
                !order.getUser().getId().equals(currentUserId)) {

            throw new ResourceNotFoundException(
                    "Order not found"
            );
        }

        return mapToResponseDto(order);
    }

    @Transactional
    public List<OrderResponseDto> getAllOrders() {

        return mapToResponseDtoList(
                orderRepository.findAll()
        );
    }

    @Transactional
    public List<OrderResponseDto> getOrdersByUserId(Long userId) {

        userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found"
                ));

        return mapToResponseDtoList(
                orderRepository.findByUserId(userId)
        );
    }

    @Transactional
    public OrderResponseDto updateOrderStatus(
            Long id,
            OrderStatusUpdateRequestDto dto
    ) {

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order not found with id: " + id
                ));

        validateStatusTransition(
                order.getStatus(),
                dto.getStatus()
        );

        if (dto.getStatus() == OrderStatus.CANCELLED) {

            for (OrderItem item : order.getItems()) {

                Product product = item.getProduct();

                product.setStock(
                        product.getStock() + item.getQuantity()
                );
            }
        }

        order.setStatus(dto.getStatus());

        return mapToResponseDto(
                orderRepository.save(order)
        );
    }

    private void validateStatusTransition(
            OrderStatus currentStatus,
            OrderStatus newStatus
    ) {

        boolean validTransition = switch (currentStatus) {

            case CREATED ->
                    newStatus == OrderStatus.CONFIRMED
                            || newStatus == OrderStatus.CANCELLED;

            case CONFIRMED ->
                    newStatus == OrderStatus.SHIPPED
                            || newStatus == OrderStatus.CANCELLED;

            case SHIPPED ->
                    newStatus == OrderStatus.DELIVERED
                            || newStatus == OrderStatus.CANCELLED;

            case DELIVERED, CANCELLED -> false;
        };

        if (!validTransition) {
            throw new InvalidOrderStatusException(
                    "Invalid order status transition: "
                            + currentStatus
                            + " -> "
                            + newStatus
            );
        }
    }

    private OrderResponseDto mapToResponseDto(Order order) {

        OrderResponseDto responseDto = new OrderResponseDto();

        responseDto.setId(order.getId());
        responseDto.setUserId(order.getUser().getId());
        responseDto.setStatus(order.getStatus());
        responseDto.setTotalPrice(order.getTotalPrice());
        responseDto.setCreatedAt(order.getCreatedAt());

        List<OrderItemResponseDto> itemDtos = new ArrayList<>();

        for (OrderItem item : order.getItems()) {

            OrderItemResponseDto itemResponseDto =
                    new OrderItemResponseDto();

            itemResponseDto.setProductId(
                    item.getProduct().getId()
            );

            itemResponseDto.setProductName(
                    item.getProduct().getName()
            );

            itemResponseDto.setQuantity(
                    item.getQuantity()
            );

            itemResponseDto.setUnitPrice(
                    item.getUnitPrice()
            );

            BigDecimal lineTotal = item.getUnitPrice()
                    .multiply(
                            BigDecimal.valueOf(item.getQuantity())
                    );

            itemResponseDto.setLineTotal(lineTotal);

            itemDtos.add(itemResponseDto);
        }

        responseDto.setItems(itemDtos);

        return responseDto;
    }

    private List<OrderResponseDto> mapToResponseDtoList(
            List<Order> orders
    ) {

        List<OrderResponseDto> responseDtos = new ArrayList<>();

        for (Order order : orders) {
            responseDtos.add(mapToResponseDto(order));
        }

        return responseDtos;
    }
}