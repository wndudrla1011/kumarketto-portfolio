package org.dsa11.team1.kumarketto.repository;

import org.dsa11.team1.kumarketto.domain.dto.ProductRegionDTO;
import org.dsa11.team1.kumarketto.domain.entity.Product;
import org.dsa11.team1.kumarketto.domain.entity.ProductRegionEntity;
import org.dsa11.team1.kumarketto.domain.entity.ProductRegion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ProductRegionRepository extends JpaRepository<ProductRegionEntity, ProductRegion> {

    // 1️⃣ DTO 조회용
    @Query("SELECT new org.dsa11.team1.kumarketto.domain.dto.ProductRegionDTO(pr.id.productId, pr.id.muniId) FROM ProductRegionEntity pr")
    List<ProductRegionDTO> findAllProductRegions();

    // 2️⃣ 특정 시구로 조회
    // EmbeddedId 안의 muniId를 이용하는 방법
    List<ProductRegionEntity> findById_MuniId(Long muniId);

    // 또는 @Query 사용
    // @Query("SELECT pr FROM ProductRegionEntity pr WHERE pr.muni.muniId = :muniId")
    // List<ProductRegionEntity> findByMuniId(@Param("muniId") Long muniId);
    Optional<ProductRegionEntity> findById_ProductId(Long productId);
    Optional<ProductRegionEntity> findByProduct(Product product);
    void deleteByProduct(Product product);

}
