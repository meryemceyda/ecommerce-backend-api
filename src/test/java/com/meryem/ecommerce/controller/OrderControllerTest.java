
package com.meryem.ecommerce.controller;

import com.meryem.ecommerce.exception.GlobalExceptionHandler;
import com.meryem.ecommerce.service.OrderService;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrderControllerTest {

    @Test
    void createOrder_shouldReturn409_whenOptimisticLockingFails()
            throws Exception {

        // ARRANGE - Mock service
        OrderService orderService = mock(OrderService.class);

        OrderController orderController =
                new OrderController(orderService);

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(orderController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        doThrow(new ObjectOptimisticLockingFailureException(
                "Product", 1L
        )).when(orderService).createOrder(any());

        String requestBody = """
                {
                  "userId": 1,
                  "items": [
                    {
                      "productId": 1,
                      "quantity": 4
                    }
                  ]
                }
                """;

        // ACT + ASSERT
        mockMvc.perform(
                        post("/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value(
                        "Veri başka bir işlem tarafından güncellendi. Lütfen tekrar deneyin."
                ))
                .andExpect(jsonPath("$.path").value("/orders"));
    }
}
