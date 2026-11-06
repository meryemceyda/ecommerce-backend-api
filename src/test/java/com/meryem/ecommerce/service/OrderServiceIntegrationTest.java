
package com.meryem.ecommerce.service;

import com.meryem.ecommerce.dto.OrderItemRequestDto;
import com.meryem.ecommerce.dto.OrderRequestDto;
import com.meryem.ecommerce.entity.Product;
import com.meryem.ecommerce.entity.user;
import com.meryem.ecommerce.exception.InsufficientStockException;
import com.meryem.ecommerce.repository.OrderRepository;
import com.meryem.ecommerce.repository.ProductRepository;
import com.meryem.ecommerce.repository.UserRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.OptimisticLockException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Testcontainers
class OrderServiceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:17");

    @Autowired
    private DataSource dataSource;

    @Autowired
    private OrderService orderService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    // TEST 1: PostgreSQL bağlantısı

    @Test
    void shouldConnectToTestcontainersPostgreSQL()
            throws SQLException {

        try (Connection connection = dataSource.getConnection()) {

            assertTrue(connection.isValid(2));

            assertEquals(
                    "PostgreSQL",
                    connection.getMetaData().getDatabaseProductName()
            );
        }
    }

    // TEST 2: Stok yetersizliğinde rollback

    @Test
    void createOrder_shouldRollbackStockChanges_whenSecondProductHasInsufficientStock() {

        // ARRANGE - Kullanıcı oluştur

        user testUser = new user();
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setEmail("rollback-test@example.com");
        testUser.setPassword("test-password");

        testUser = userRepository.save(testUser);

        // ARRANGE - Ürünleri oluştur

        Product laptop = new Product();
        laptop.setName("Laptop");
        laptop.setPrice(new BigDecimal("1000.00"));
        laptop.setStock(10);

        Product mouse = new Product();
        mouse.setName("Mouse");
        mouse.setPrice(new BigDecimal("200.00"));
        mouse.setStock(2);

        laptop = productRepository.save(laptop);
        mouse = productRepository.save(mouse);

        Long laptopId = laptop.getId();

        // ARRANGE - Sipariş kalemlerini oluştur

        OrderItemRequestDto laptopItem =
                new OrderItemRequestDto();
        laptopItem.setProductId(laptopId);
        laptopItem.setQuantity(3);

        OrderItemRequestDto mouseItem =
                new OrderItemRequestDto();
        mouseItem.setProductId(mouse.getId());
        mouseItem.setQuantity(5);

        OrderRequestDto request = new OrderRequestDto();
        request.setUserId(testUser.getId());
        request.setItems(List.of(laptopItem, mouseItem));

        long orderCountBefore = orderRepository.count();

        // ACT + ASSERT

        assertThrows(
                InsufficientStockException.class,
                () -> orderService.createOrder(request)
        );

        Product laptopAfterRollback =
                productRepository.findById(laptopId)
                        .orElseThrow();

        assertEquals(10, laptopAfterRollback.getStock());

        assertEquals(
                orderCountBefore,
                orderRepository.count()
        );
    }

    // TEST 3: Optimistic Locking

    @Test
    void shouldPreventLostUpdate_whenTwoTransactionsModifySameProduct() {

        // ARRANGE - Ürün oluştur

        Product product = new Product();
        product.setName("Concurrent Test Product");
        product.setPrice(new BigDecimal("500.00"));
        product.setStock(5);

        Product savedProduct = productRepository.save(product);
        Long productId = savedProduct.getId();

        // İki bağımsız persistence context

        EntityManager emA =
                entityManagerFactory.createEntityManager();

        EntityManager emB =
                entityManagerFactory.createEntityManager();

        try {

            // İki transaction başlat

            emA.getTransaction().begin();
            emB.getTransaction().begin();

            // Aynı ürünün aynı versiyonunu oku

            Product productA =
                    emA.find(Product.class, productId);

            Product productB =
                    emB.find(Product.class, productId);

            assertEquals(
                    productA.getVersion(),
                    productB.getVersion()
            );

            // Transaction A stok günceller

            productA.setStock(4);
            emA.getTransaction().commit();

            // Transaction B eski versiyonla günceller

            productB.setStock(3);

            // Eski versiyon reddedilmeli

            assertThrows(
                    OptimisticLockException.class,
                    () -> emB.flush()
            );

        } finally {

            if (emA.getTransaction().isActive()) {
                emA.getTransaction().rollback();
            }

            if (emB.getTransaction().isActive()) {
                emB.getTransaction().rollback();
            }

            emA.close();
            emB.close();
        }

        // ASSERT - İlk güncelleme korunmalı

        Product finalProduct =
                productRepository.findById(productId)
                        .orElseThrow();

        assertEquals(4, finalProduct.getStock());
    }

    @Test
    void shouldNotOversellProduct_whenTwoOrdersAreCreatedConcurrently()
            throws Exception {

        // ARRANGE - İki kullanıcı oluştur

        user userA = new user();
        userA.setFirstName("Concurrent");
        userA.setLastName("UserA");
        userA.setEmail("concurrent-a@example.com");
        userA.setPassword("test-password");
        userA = userRepository.save(userA);

        user userB = new user();
        userB.setFirstName("Concurrent");
        userB.setLastName("UserB");
        userB.setEmail("concurrent-b@example.com");
        userB.setPassword("test-password");
        userB = userRepository.save(userB);

        // ARRANGE - Stoku 5 olan ürün

        Product product = new Product();
        product.setName("Concurrent Order Laptop");
        product.setPrice(new BigDecimal("1000.00"));
        product.setStock(5);

        product = productRepository.save(product);

        Long productId = product.getId();

        // ARRANGE - İki sipariş isteği

        OrderItemRequestDto itemA = new OrderItemRequestDto();
        itemA.setProductId(productId);
        itemA.setQuantity(4);

        OrderRequestDto requestA = new OrderRequestDto();
        requestA.setUserId(userA.getId());
        requestA.setItems(List.of(itemA));

        OrderItemRequestDto itemB = new OrderItemRequestDto();
        itemB.setProductId(productId);
        itemB.setQuantity(4);

        OrderRequestDto requestB = new OrderRequestDto();
        requestB.setUserId(userB.getId());
        requestB.setItems(List.of(itemB));

        long ordersBefore = orderRepository.count();

        ExecutorService executor = Executors.newFixedThreadPool(2);

        CountDownLatch readyGate = new CountDownLatch(2);
        CountDownLatch startGate = new CountDownLatch(1);

        AtomicInteger successfulOrders = new AtomicInteger();
        AtomicInteger failedOrders = new AtomicInteger();

        // ACT - İki siparişi paralel çalıştır

        Callable<Void> taskA = () -> {
            readyGate.countDown();

            if (!startGate.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Start gate timeout");
            }

            try {
                orderService.createOrder(requestA);
                successfulOrders.incrementAndGet();
            } catch (RuntimeException ex) {
                failedOrders.incrementAndGet();
            }

            return null;
        };

        Callable<Void> taskB = () -> {
            readyGate.countDown();

            if (!startGate.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Start gate timeout");
            }

            try {
                orderService.createOrder(requestB);
                successfulOrders.incrementAndGet();
            } catch (RuntimeException ex) {
                failedOrders.incrementAndGet();
            }

            return null;
        };

        try {
            Future<Void> futureA = executor.submit(taskA);
            Future<Void> futureB = executor.submit(taskB);

            assertTrue(
                    readyGate.await(10, TimeUnit.SECONDS),
                    "İki thread de hazır olmalı"
            );

            startGate.countDown();

            // İki işlemin tamamlanmasını bekle
            futureA.get(30, TimeUnit.SECONDS);
            futureB.get(30, TimeUnit.SECONDS);

        } finally {
            startGate.countDown();
            executor.shutdownNow();
        }

        // ASSERT - Sadece bir sipariş başarılı olmalı

        assertEquals(1, successfulOrders.get());
        assertEquals(1, failedOrders.get());

        // PostgreSQL'deki son stok
        Product finalProduct = productRepository.findById(productId)
                .orElseThrow();

        assertEquals(1, finalProduct.getStock());

        // Veritabanında yalnızca bir yeni sipariş bulunmalı
        assertEquals(ordersBefore + 1, orderRepository.count());
    }

}
