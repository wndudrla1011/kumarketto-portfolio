package org.dsa11.team1.kumarketto.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.dsa11.team1.kumarketto.domain.dto.TransactionHistoryDTO;
import org.dsa11.team1.kumarketto.domain.entity.Product;
import org.dsa11.team1.kumarketto.domain.enums.ProductStatus;
import org.dsa11.team1.kumarketto.domain.entity.Transaction;
import org.dsa11.team1.kumarketto.domain.entity.WishList;
import org.dsa11.team1.kumarketto.repository.ProductRepository;
import org.dsa11.team1.kumarketto.repository.TransactionRepository;
import org.dsa11.team1.kumarketto.repository.WishListRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.dsa11.team1.kumarketto.domain.enums.TransactionStatus;

@Slf4j
@RequiredArgsConstructor
@Transactional
@Service
public class MyPageService {
    private final WishListRepository wishListRepository;
    private final ProductRepository productRepository;
    private final TransactionRepository transactionRepository;


    // 특정 회원의 위시리스트 전체 조회
    public List<Product> getWishlistItems(Long user) {

        // userNo를 사용하여 위시리스트를 조회
        List<WishList> wishLists = wishListRepository.findByMember_UserNoOrderByModifiedDateDesc(user);

        // WishList 엔티티에서 Product만 추출하여 반환합니다.
        return wishLists.stream()
                .map(WishList::getProduct)
                .collect(Collectors.toList());

    }

        // 판매 완료 리스트
        public List<TransactionHistoryDTO> getSoldOut(Long user) {

        // 1. userNo와 SOLD_OUT 상태를 기준으로 상품을 조회
        List<TransactionHistoryDTO> soldOutList = transactionRepository.findTransactionHistoriesByUser(user, ProductStatus.SOLDOUT);

        // 3. 자바 코드로 결제 확정 시간(confirmTime)을 기준으로 내림차순 정렬합니다.
        soldOutList.sort(Comparator.comparing(TransactionHistoryDTO::getConfirmTime, Comparator.nullsLast(Comparator.reverseOrder())));

        return soldOutList;
    }


    public List<TransactionHistoryDTO> getConfirmed(Long user) {
        // 구매 완료 리스트
            // 1. 유저 ID와 '거래 완료(CONFIRMED)' 상태로 Transaction을 조회합니다.
            List<Transaction> completedTransactions = transactionRepository.findByMember_UserNoAndStatusOrderByConfirmTimeDesc(user, TransactionStatus.CONFIRMED);

            // 2. 조회된 거래(Transaction) 리스트를 DTO로 변환합니다.
            return completedTransactions.stream()
                    .map(transaction -> {
                        Product product = transaction.getProduct(); // 거래에 연결된 상품 정보를 가져옵니다.

                        return TransactionHistoryDTO.builder()
                                .pid(product.getPid())
                                .name(product.getName())
                                .price(product.getPrice())
                                .imageUrl(product.getImageUrl())
                                .confirmTime(transaction.getConfirmTime()) // 거래 확정 시간
                                // 지역 정보
                                .regionName(product.getProductRegions().stream().findFirst()
                                        .map(pr -> pr.getMunicipality().getPrefecture().getPrefName() + " · " + pr.getMunicipality().getMuniName())
                                        .orElse(null))
                                .build();
                    })
                    .collect(Collectors.toList());
        }
}


