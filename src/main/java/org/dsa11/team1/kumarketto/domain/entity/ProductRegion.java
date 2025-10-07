package org.dsa11.team1.kumarketto.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;


@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductRegion implements Serializable {
    private Long productId;
    private Long muniId;
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProductRegion)) return false;
        ProductRegion that = (ProductRegion) o;
        return productId.equals(that.productId) &&
                muniId.equals(that.muniId);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(productId, muniId);
    }
}