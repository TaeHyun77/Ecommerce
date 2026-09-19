package com.park.ecommerce.cart;

import com.park.ecommerce.cart.dto.CartItemResponse;
import com.park.ecommerce.exception.OrderErrorCode;
import com.park.ecommerce.exception.OrderException;
import com.park.ecommerce.exception.ProductServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CartController.class)
class CartControllerTest {
    private static final String MEMBER_ID_HEADER = "X-Member-Id";
    private static final String ADD_REQUEST = """
            {
              "productId": 10,
              "quantity": 2
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CartService cartService;

    // 메인 클래스의 @EnableJpaAuditing이 웹 슬라이스 테스트에서도 JPA 메타모델을 요구하므로 대체
    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    @DisplayName("장바구니에 담으면 201과 담긴 항목을 응답한다")
    void respondsCreated() throws Exception {
        given(cartService.addItem(1L, 10L, 2)).willReturn(cartItemResponse());

        mockMvc.perform(post("/api/carts/items")
                        .header(MEMBER_ID_HEADER, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ADD_REQUEST))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.productId").value(10))
                .andExpect(jsonPath("$.quantity").value(2));
    }

    @Test
    @DisplayName("회원 식별 헤더가 없으면 400을 응답한다")
    void respondsBadRequestWhenMemberHeaderMissing() throws Exception {
        mockMvc.perform(post("/api/carts/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ADD_REQUEST))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    @Test
    @DisplayName("수량이 1개 미만이면 400과 검증 메시지를 응답한다")
    void respondsBadRequestWhenQuantityIsZero() throws Exception {
        mockMvc.perform(post("/api/carts/items")
                        .header(MEMBER_ID_HEADER, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ADD_REQUEST.replace("\"quantity\": 2", "\"quantity\": 0")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("수량은 1개 이상이어야 합니다."));
    }

    @Test
    @DisplayName("품절 상품을 담으면 409를 응답한다")
    void respondsConflictWhenSoldOut() throws Exception {
        given(cartService.addItem(anyLong(), anyLong(), anyInt()))
                .willThrow(new OrderException(OrderErrorCode.PRODUCT_SOLD_OUT));

        mockMvc.perform(post("/api/carts/items")
                        .header(MEMBER_ID_HEADER, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ADD_REQUEST))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PRODUCT_SOLD_OUT"));
    }

    @Test
    @DisplayName("장바구니를 조회하면 200과 항목 목록을 응답한다")
    void respondsCart() throws Exception {
        given(cartService.getCart(1L)).willReturn(com.park.ecommerce.cart.dto.CartResponse.from(
                java.util.List.of(cartItemResponse())
        ));

        mockMvc.perform(get("/api/carts").header(MEMBER_ID_HEADER, 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].productId").value(10))
                .andExpect(jsonPath("$.totalAmount").value(7800));
    }

    @Test
    @DisplayName("수량을 변경하면 200과 변경된 항목을 응답한다")
    void respondsChangedItem() throws Exception {
        given(cartService.changeQuantity(1L, 10L, 3)).willReturn(cartItemResponse());

        mockMvc.perform(patch("/api/carts/items/10")
                        .header(MEMBER_ID_HEADER, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\": 3}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("항목을 삭제하면 204를 응답한다")
    void respondsNoContent() throws Exception {
        mockMvc.perform(delete("/api/carts/items/10").header(MEMBER_ID_HEADER, 1L))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("담겨 있지 않은 상품을 삭제하면 404를 응답한다")
    void respondsNotFoundWhenCartItemMissing() throws Exception {
        willThrow(new OrderException(OrderErrorCode.CART_ITEM_NOT_FOUND))
                .given(cartService).removeItem(1L, 10L);

        mockMvc.perform(delete("/api/carts/items/10").header(MEMBER_ID_HEADER, 1L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CART_ITEM_NOT_FOUND"));
    }

    @Test
    @DisplayName("상품 서비스 장애면 503을 응답한다")
    void respondsServiceUnavailable() throws Exception {
        given(cartService.getCart(1L)).willThrow(new ProductServiceUnavailableException());

        mockMvc.perform(get("/api/carts").header(MEMBER_ID_HEADER, 1L))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("PRODUCT_SERVICE_UNAVAILABLE"));
    }

    @Test
    @DisplayName("서킷이 열려 호출이 차단되면 503을 응답한다")
    void respondsServiceUnavailableWhenCircuitOpen() throws Exception {
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("productService");
        circuitBreaker.transitionToOpenState();
        given(cartService.getCart(1L))
                .willThrow(CallNotPermittedException.createCallNotPermittedException(circuitBreaker));

        mockMvc.perform(get("/api/carts").header(MEMBER_ID_HEADER, 1L))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("PRODUCT_SERVICE_UNAVAILABLE"));
    }

    private static CartItemResponse cartItemResponse() {
        return new CartItemResponse(10L, "유기농 우유 900ml", 3_900, 2, 7_800, null, false, true);
    }
}
