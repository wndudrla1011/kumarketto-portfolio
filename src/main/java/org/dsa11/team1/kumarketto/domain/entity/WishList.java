package org.dsa11.team1.kumarketto.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "wish_lists", uniqueConstraints = {
        @UniqueConstraint(name = "uq_user_product", columnNames = {"user_no", "product_id"})
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WishList extends BaseTimeEntity {

    @EmbeddedId
    private WishListId id; // 복합 PK (user_no, product_id)

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userNo")
    @JoinColumn(name = "user_no")
    private MemberEntity member; // 회원

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("productId")
    @JoinColumn(name = "product_id")
    private Product product; // 상품

    @Builder
    public WishList(MemberEntity member, Product product) {
        this.id = new WishListId(member.getUserNo(), product.getPid());
        this.member = member;
        this.product = product;
    }

}
