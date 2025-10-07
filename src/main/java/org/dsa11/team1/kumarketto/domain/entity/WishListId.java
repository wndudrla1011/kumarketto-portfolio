package org.dsa11.team1.kumarketto.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WishListId implements Serializable {

    @Column(name = "user_no")
    private Long userNo;
    @Column(name = "product_id")
    private Long productId;

    public WishListId(Long userNo, Long productId) {
        this.userNo = userNo;
        this.productId = productId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WishListId)) return false;
        WishListId that = (WishListId) o;
        return userNo.equals(that.userNo) && productId.equals(that.productId);
    }

    @Override
    public int hashCode() {
        return userNo.hashCode() + productId.hashCode();
    }

}
