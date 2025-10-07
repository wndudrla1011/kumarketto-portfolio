package org.dsa11.team1.kumarketto.repository;

import org.dsa11.team1.kumarketto.domain.dto.ProductListDTO;
import org.dsa11.team1.kumarketto.domain.entity.Product;

import org.dsa11.team1.kumarketto.domain.enums.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    @Query("""
    SELECT new org.dsa11.team1.kumarketto.domain.dto.ProductListDTO(
        p.pid, p.name, p.price, p.viewCount, p.status, p.imageUrl,
        (SELECT COUNT(w) FROM WishList w WHERE w.product = p)
    )
    FROM Product p
    WHERE p.status = 'NEW'
      AND (:subCategoryId IS NULL OR p.subCategory.id = :subCategoryId)
      AND (:searchWord IS NULL OR p.name LIKE %:searchWord% OR p.member.nickname LIKE %:searchWord%)
    ORDER BY p.modifiedDate DESC, p.viewCount DESC,
             (SELECT COUNT(w2.id) FROM WishList w2 WHERE w2.product = p) DESC
    """)
    Page<ProductListDTO> mainProducts(@Param("searchWord") String searchWord,
                                                           @Param("subCategoryId") Integer subCategoryId,
                                                           Pageable pageable);

    @Query("SELECT p.pid FROM Product p")
    List<Long> findAllIds();

    /**
     * 특정 판매자(상점 주인)의 상품 목록을 페이징하여 조회
     * @param userNo 판매자의 회원 ID
     * @param pageable 페이징 정보
     * @return 페이징된 상품 DTO 목록
     */
    @Query("""
    SELECT new org.dsa11.team1.kumarketto.domain.dto.ProductListDTO(
        p.pid, p.name, p.price, p.viewCount, p.status, p.imageUrl,
        (SELECT COUNT(w) FROM WishList w WHERE w.product = p)
    )
    FROM Product p
    WHERE p.member.userNo = :userNo
      AND (:status IS NULL OR p.status = :status)
    """)
    Page<ProductListDTO> findProductsByUserNo(@Param("userNo") Long userNo, @Param("status") ProductStatus status, Pageable pageable);

    /**
     * 특정 판매자의 전체 상품 개수를 조회
     */
    long countByMember_UserNo(Long userNo);

    /**
     * 특정 판매자의 SOLDOUT 상태 상품 개수를 조회
     */
    long countByMember_UserNoAndStatus(Long userNo, ProductStatus status);

    // 사용자의 userNo와 상품 상태(Status)를 기준으로 판매 상품 리스트 조회(결제완료 최신순 정렬)
    List<Product> findByMember_UserNoAndStatusOrderByCreatedDateDesc(Long userNo, ProductStatus status);


}
