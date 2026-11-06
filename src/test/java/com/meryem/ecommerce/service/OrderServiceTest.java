package com.meryem.ecommerce.service;

import com.meryem.ecommerce.dto.OrderItemRequestDto;
import com.meryem.ecommerce.dto.OrderRequestDto;
import com.meryem.ecommerce.dto.OrderResponseDto;
import com.meryem.ecommerce.dto.OrderStatusUpdateRequestDto;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private OrderService orderService;

    @Test
    void createOrder_shouldCreateOrder_whenRequestIsValid() {

        // ARRANGE

        user testUser = new user();
        testUser.setId(1L);

        Product product = createProduct(
                10L,
                "Laptop",
                new BigDecimal("100.00"),
                5
        );

        OrderItemRequestDto itemDto = new OrderItemRequestDto();
        itemDto.setProductId(10L);
        itemDto.setQuantity(2);

        OrderRequestDto requestDto = new OrderRequestDto();
        requestDto.setUserId(1L);
        requestDto.setItems(List.of(itemDto));

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(testUser));

        when(productRepository.findById(10L))
                .thenReturn(Optional.of(product));

        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));


        // ACT

        OrderResponseDto result = orderService.createOrder(requestDto);


        // ASSERT

        assertNotNull(result);

        assertEquals(OrderStatus.CREATED, result.getStatus());

        assertEquals(
                0,
                new BigDecimal("200.00")
                        .compareTo(result.getTotalPrice())
        );

        assertEquals(3, product.getStock());

        assertEquals(1, result.getItems().size());

        assertEquals(10L, result.getItems().get(0).getProductId());

        assertEquals(2, result.getItems().get(0).getQuantity());

        assertEquals(
                0,
                new BigDecimal("100.00")
                        .compareTo(result.getItems().get(0).getUnitPrice())
        );

        verify(userRepository).findById(1L);

        verify(productRepository).findById(10L);

        verify(orderRepository).save(any(Order.class));
    }
    @Test
    void createOrder_shouldThrowInsufficientStockException_whenStockIsNotEnough() {

        // ARRANGE

        user testUser = new user();
        testUser.setId(1L);

        Product product = createProduct(
                10L,
                "Laptop",
                new BigDecimal("100.00"),
                2
        );

        OrderItemRequestDto itemDto = new OrderItemRequestDto();
        itemDto.setProductId(10L);
        itemDto.setQuantity(5);

        OrderRequestDto requestDto = new OrderRequestDto();
        requestDto.setUserId(1L);
        requestDto.setItems(List.of(itemDto));

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(testUser));

        when(productRepository.findById(10L))
                .thenReturn(Optional.of(product));


        // ACT + ASSERT

        InsufficientStockException exception = assertThrows(
                InsufficientStockException.class,
                () -> orderService.createOrder(requestDto)
        );

        assertEquals(
                "Insufficient stock for product id: 10",
                exception.getMessage()
        );

        assertEquals(2, product.getStock());

        verify(userRepository).findById(1L);
        verify(productRepository).findById(10L);

        verify(orderRepository, never())
                .save(any(Order.class));
    }
    @Test
    void createOrder_shouldThrowResourceNotFoundException_whenUserDoesNotExist() {

        // ARRANGE

        OrderItemRequestDto itemDto = new OrderItemRequestDto();
        itemDto.setProductId(10L);
        itemDto.setQuantity(2);

        OrderRequestDto requestDto = new OrderRequestDto();
        requestDto.setUserId(1L);
        requestDto.setItems(List.of(itemDto));

        when(userRepository.findById(1L))
                .thenReturn(Optional.empty());


        // ACT + ASSERT

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> orderService.createOrder(requestDto)
        );

        assertEquals(
                "User not found with id: 1",
                exception.getMessage()
        );

        verify(userRepository).findById(1L);

        verifyNoInteractions(productRepository, orderRepository);
    }
    @Test
    void createOrder_shouldThrowResourceNotFoundException_whenProductDoesNotExist() {

        // ARRANGE

        user testUser = new user();
        testUser.setId(1L);

        OrderItemRequestDto itemDto = new OrderItemRequestDto();
        itemDto.setProductId(10L);
        itemDto.setQuantity(2);

        OrderRequestDto requestDto = new OrderRequestDto();
        requestDto.setUserId(1L);
        requestDto.setItems(List.of(itemDto));

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(testUser));

        when(productRepository.findById(10L))
                .thenReturn(Optional.empty());


        // ACT + ASSERT

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> orderService.createOrder(requestDto)
        );

        assertEquals(
                "Product not found with id: 10",
                exception.getMessage()
        );

        verify(userRepository).findById(1L);

        verify(productRepository).findById(10L);

        verify(orderRepository, never())
                .save(any(Order.class));
    }
    @Test
    void createOrder_shouldCalculateTotalPrice_whenMultipleProductsOrdered() {

        // ARRANGE

        user testUser = new user();
        testUser.setId(1L);

        Product laptop = createProduct(
                10L,
                "Laptop",
                new BigDecimal("1000.00"),
                10
        );

        Product mouse = createProduct(
                20L,
                "Mouse",
                new BigDecimal("200.00"),
                15
        );

        OrderItemRequestDto laptopItem = new OrderItemRequestDto();
        laptopItem.setProductId(10L);
        laptopItem.setQuantity(2);

        OrderItemRequestDto mouseItem = new OrderItemRequestDto();
        mouseItem.setProductId(20L);
        mouseItem.setQuantity(3);

        OrderRequestDto requestDto = new OrderRequestDto();
        requestDto.setUserId(1L);
        requestDto.setItems(List.of(laptopItem, mouseItem));

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(testUser));

        when(productRepository.findById(10L))
                .thenReturn(Optional.of(laptop));

        when(productRepository.findById(20L))
                .thenReturn(Optional.of(mouse));

        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));


        // ACT

        OrderResponseDto result = orderService.createOrder(requestDto);


        // ASSERT

        assertNotNull(result);

        assertEquals(OrderStatus.CREATED, result.getStatus());

        assertEquals(
                0,
                new BigDecimal("2600.00")
                        .compareTo(result.getTotalPrice())
        );

        assertEquals(8, laptop.getStock());
        assertEquals(12, mouse.getStock());

        assertEquals(2, result.getItems().size());

        assertEquals(
                0,
                new BigDecimal("2000.00")
                        .compareTo(result.getItems().get(0).getLineTotal())
        );

        assertEquals(
                0,
                new BigDecimal("600.00")
                        .compareTo(result.getItems().get(1).getLineTotal())
        );

        verify(userRepository).findById(1L);

        verify(productRepository).findById(10L);
        verify(productRepository).findById(20L);

        verify(orderRepository).save(any(Order.class));
    }
    @Test
    void updateOrderStatus_shouldRestoreStock_whenOrderIsCancelled() {

        // ARRANGE

        user testUser = new user();
        testUser.setId(1L);

        Product product = createProduct(
                10L,
                "Laptop",
                new BigDecimal("100.00"),
                7
        );

        Order order = createOrder(
                1L,
                testUser,
                OrderStatus.CREATED,
                new BigDecimal("300.00")
        );

        OrderItem orderItem = createOrderItem(
                product,
                3,
                new BigDecimal("100.00")
        );

        order.addItem(orderItem);

        OrderStatusUpdateRequestDto requestDto =
                new OrderStatusUpdateRequestDto();

        requestDto.setStatus(OrderStatus.CANCELLED);

        when(orderRepository.findById(1L))
                .thenReturn(Optional.of(order));

        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));


        // ACT

        OrderResponseDto result =
                orderService.updateOrderStatus(1L, requestDto);


        // ASSERT

        assertNotNull(result);

        assertEquals(OrderStatus.CANCELLED, result.getStatus());

        assertEquals(10, product.getStock());

        assertEquals(OrderStatus.CANCELLED, order.getStatus());

        verify(orderRepository).findById(1L);

        verify(orderRepository).save(any(Order.class));
    }
    @Test
    void updateOrderStatus_shouldThrowException_whenTransitionIsInvalid() {

        // ARRANGE

        Order order = new Order();
        order.setId(1L);
        order.setStatus(OrderStatus.CREATED);

        OrderStatusUpdateRequestDto requestDto =
                new OrderStatusUpdateRequestDto();

        requestDto.setStatus(OrderStatus.DELIVERED);

        when(orderRepository.findById(1L))
                .thenReturn(Optional.of(order));


        // ACT + ASSERT

        assertThrows(
                InvalidOrderStatusException.class,
                () -> orderService.updateOrderStatus(1L, requestDto)
        );

        // Siparişin durumu değişmemeli
        assertEquals(OrderStatus.CREATED, order.getStatus());

        // Geçersiz işlem veritabanına kaydedilmemeli
        verify(orderRepository).findById(1L);

        verify(orderRepository, never())
                .save(any(Order.class));

        verifyNoInteractions(productRepository);
    }
    @Test
    void updateOrderStatus_shouldConfirmOrder_whenTransitionIsValid() {

        // ARRANGE

        user testUser = new user();
        testUser.setId(1L);

        Product product = createProduct(
                10L,
                "Laptop",
                new BigDecimal("100.00"),
                7
        );

        Order order = createOrder(
                1L,
                testUser,
                OrderStatus.CREATED,
                new BigDecimal("300.00")
        );

        OrderItem orderItem = createOrderItem(
                product,
                3,
                new BigDecimal("100.00")
        );

        order.addItem(orderItem);

        order.addItem(orderItem);

        OrderStatusUpdateRequestDto requestDto =
                new OrderStatusUpdateRequestDto();

        requestDto.setStatus(OrderStatus.CONFIRMED);

        when(orderRepository.findById(1L))
                .thenReturn(Optional.of(order));

        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));


        // ACT

        OrderResponseDto result =
                orderService.updateOrderStatus(1L, requestDto);


        // ASSERT

        assertNotNull(result);

        assertEquals(OrderStatus.CONFIRMED, result.getStatus());

        assertEquals(OrderStatus.CONFIRMED, order.getStatus());

        // Sipariş onaylanırken stok değişmemeli
        assertEquals(7, product.getStock());

        verify(orderRepository).findById(1L);

        verify(orderRepository).save(any(Order.class));

        verifyNoInteractions(productRepository);
    }
    @Test
    void updateOrderStatus_shouldRejectConfirmation_whenOrderIsCancelled() {

        // ARRANGE

        user testUser = new user();
        testUser.setId(1L);

        Order order = createOrder(
                1L,
                testUser,
                OrderStatus.CANCELLED,
                new BigDecimal("300.00")
        );

        OrderStatusUpdateRequestDto requestDto =
                new OrderStatusUpdateRequestDto();

        requestDto.setStatus(OrderStatus.CONFIRMED);

        when(orderRepository.findById(1L))
                .thenReturn(Optional.of(order));

        // ACT + ASSERT

        assertThrows(
                InvalidOrderStatusException.class,
                () -> orderService.updateOrderStatus(1L, requestDto)
        );

        assertEquals(OrderStatus.CANCELLED, order.getStatus());

        verify(orderRepository).findById(1L);

        verify(orderRepository, never())
                .save(any(Order.class));

        verifyNoInteractions(productRepository);
    }
    @Test
    void createOrder_shouldThrowInsufficientStockException_whenSameProductExceedsStock() {

        // ARRANGE

        user testUser = new user();
        testUser.setId(1L);

        Product product = createProduct(
                10L,
                "Laptop",
                new BigDecimal("1000.00"),
                5
        );

        OrderItemRequestDto firstItem = new OrderItemRequestDto();
        firstItem.setProductId(10L);
        firstItem.setQuantity(3);

        OrderItemRequestDto secondItem = new OrderItemRequestDto();
        secondItem.setProductId(10L);
        secondItem.setQuantity(3);

        OrderRequestDto requestDto = new OrderRequestDto();
        requestDto.setUserId(1L);
        requestDto.setItems(List.of(firstItem, secondItem));

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(testUser));

        when(productRepository.findById(10L))
                .thenReturn(Optional.of(product));


        // ACT + ASSERT

        assertThrows(
                InsufficientStockException.class,
                () -> orderService.createOrder(requestDto)
        );

        // İlk kalemde 3 adet düşülmüş olmalı.
        // Mockito transaction rollback yapmaz.
        assertEquals(2, product.getStock());

        verify(userRepository).findById(1L);

        verify(productRepository, times(2))
                .findById(10L);

        verify(orderRepository, never())
                .save(any(Order.class));
    }
    private Product createProduct(
            Long id,
            String name,
            BigDecimal price,
            int stock
    ) {
        Product product = new Product();
        product.setId(id);
        product.setName(name);
        product.setPrice(price);
        product.setStock(stock);

        return product;
    }
    private Order createOrder(
            Long id,
            user testUser,
            OrderStatus status,
            BigDecimal totalPrice
    ) {
        Order order = new Order();
        order.setId(id);
        order.setUser(testUser);
        order.setStatus(status);
        order.setTotalPrice(totalPrice);
        order.setCreatedAt(LocalDateTime.now());

        return order;
    }
    private OrderItem createOrderItem(
            Product product,
            int quantity,
            BigDecimal unitPrice
    ) {
        OrderItem orderItem = new OrderItem();

        orderItem.setProduct(product);
        orderItem.setQuantity(quantity);
        orderItem.setUnitPrice(unitPrice);

        return orderItem;
    }
}