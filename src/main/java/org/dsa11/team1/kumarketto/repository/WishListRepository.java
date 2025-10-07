package org.dsa11.team1.kumarketto.repository;

import org.dsa11.team1.kumarketto.domain.entity.MemberEntity;
import org.dsa11.team1.kumarketto.domain.entity.Product;
import org.dsa11.team1.kumarketto.domain.entity.WishList;
import org.dsa11.team1.kumarketto.domain.entity.WishListId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WishListRepository extends JpaRepository<WishList, WishListId> {

    @Query("SELECT w.product.pid, COUNT(w) FROM WishList w WHERE w.product.pid IN :productIds GROUP BY w.product.pid")
    List<Object[]> countLikesByProductIds(@Param("productIds") List<Long> productIds);

    // 특정 회원이 특정 상품을 이미 찜했는지 확인
    Optional<WishList> findByMemberAndProduct(MemberEntity wishUserno, Product wishPid);

    // 회원의 찜 목록 가져오기
    List<WishList> findAllByMember(MemberEntity wishUserno);

    // 상품 찜 수 카운트
    long countByProduct(Product wishPid);

    // 찜 삭제
    void deleteByMemberAndProduct(MemberEntity wishUserno, Product wishPid);


    List<WishList> findByMember(MemberEntity memberEntity);

    // Member 엔티티의 userNo 필드를 기준으로 WishList 목록을 조회(정렬기능 없음)
    // List<WishList> findByMember_UserNo(Long userNo);


    // MemberEntity의 userNo로 WishList를 조회하고, modifiedDate를 기준으로 내림차순 정렬
    List<WishList> findByMember_UserNoOrderByModifiedDateDesc(Long userNo);

}
