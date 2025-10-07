package org.dsa11.team1.kumarketto.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Entity
@Table(name = "product_region")
@EqualsAndHashCode
public class ProductRegionEntity {

    @EmbeddedId
    private ProductRegion id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("muniId") // ProductRegionId.muniId와 매핑
    @JoinColumn(name = "muni_id")
    private Municipality municipality;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("productId") // ProductRegionId.productId와 매핑
    @JoinColumn(name = "product_id")
    private Product product;

}
