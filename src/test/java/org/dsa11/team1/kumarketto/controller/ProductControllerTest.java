package org.dsa11.team1.kumarketto.controller;

import org.dsa11.team1.kumarketto.domain.dto.CategoriesDTO;
import org.dsa11.team1.kumarketto.service.ProductService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
public class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductService productService;

    @Test
    public void testWriteFormLoadsCategories() throws Exception {
        // Mocking 서비스
        CategoriesDTO cat1 = new CategoriesDTO();
        cat1.setCategoryId(1);
        cat1.setCategoryName("음식");

        CategoriesDTO cat2 = new CategoriesDTO();
        cat2.setCategoryId(2);
        cat2.setCategoryName("음료");

        List<CategoriesDTO> mockCategories = Arrays.asList(cat1, cat2);
        Mockito.when(productService.getAllCategories()).thenReturn(mockCategories);

        // 컨트롤러 호출 테스트
        mockMvc.perform(get("/product/write"))
                .andExpect(status().isOk())
                .andExpect(view().name("writeForm"))
                .andExpect(model().attributeExists("product"))
                .andExpect(model().attributeExists("categories"))
                .andExpect(model().attribute("categories", mockCategories));
    }
}
