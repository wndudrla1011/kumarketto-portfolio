package org.dsa11.team1.kumarketto.service;

import org.dsa11.team1.kumarketto.domain.entity.ProductDocument;
import org.dsa11.team1.kumarketto.repository.ProductElasticsearchRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ProductServiceTest {

    @Mock
    private ProductElasticsearchRepository productElasticsearchRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    public void testGetMainPageProducts() throws Exception {
        //given
        Pageable pageable = PageRequest.of(0, 10);

        List<ProductDocument> documents = List.of(
                createProductDocument(1L, "상품1", 50, 10L, 0),
                createProductDocument(2L, "상품2", 30, 5L, 0),
                createProductDocument(3L, "상품3", 80, 2L, 1),
                createProductDocument(4L, "상품4", 20, 15L, 3),
                createProductDocument(5L, "상품5", 60, 8L, 2)
        );

        when(productElasticsearchRepository.findByCustomScore()).thenReturn(documents);

        //when
        Page<ProductDocument> result = productService.getMainPageProducts(pageable);

        //then
        assertEquals(5, result.getTotalElements());
        assertEquals("상품1", result.getContent().get(0).getName());
        assertEquals("상품2", result.getContent().get(1).getName());
        assertEquals("상품3", result.getContent().get(2).getName());
        assertEquals("상품4", result.getContent().get(3).getName());
        assertEquals("상품5", result.getContent().get(4).getName());
    }

    private ProductDocument createProductDocument(Long pid, String name, int viewCount, long likeCount, int daysAgo) {
        ProductDocument doc = ProductDocument.builder()
                .pid(pid)
                .name(name)
                .price(1000)
                .viewCount(viewCount)
                .likeCount(likeCount)
                .status("NEW")
                .imageUrl("img" + pid + ".jpg")
                .modifiedDate(LocalDateTime.now().minusDays(daysAgo))
                .build();

        return doc;
    }

}
