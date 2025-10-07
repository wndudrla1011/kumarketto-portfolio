package org.dsa11.team1.kumarketto.service;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchAllQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dsa11.team1.kumarketto.domain.dto.*;
import org.dsa11.team1.kumarketto.domain.entity.*;
import org.dsa11.team1.kumarketto.domain.enums.ProductStatus;
import org.dsa11.team1.kumarketto.repository.*;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.dsa11.team1.kumarketto.domain.dto.ProductListDTO;
import org.dsa11.team1.kumarketto.domain.entity.ProductDocument;
import org.dsa11.team1.kumarketto.repository.ProductElasticsearchRepository;
import org.dsa11.team1.kumarketto.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Transactional
@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final MemberRepository memberRepository;
    private final SubCategoryRepository subCategoriesRepository;
    private final CategoryRepository categoriesRepository;
    private final RegionRepository regionRepository;
    private final PrefectureRepository prefectureRepository;
    private final MunicipalityRepository municipalityRepository;
    private final ProductRegionRepository productRegionRepository;
    private final ProductElasticsearchRepository productElasticsearchRepository;
    private final ProductImageRepository productImageRepository;
    private final WishListRepository wishListRepository;
    private final ElasticsearchOperations elasticsearchOperations;
    private final S3Service s3Service;

    // ------------------ 검색 관련 ------------------

    /**
     * 메인 화면 로딩 (최신, 조회수, 찜 수 가중치 적용)
     * @param pageable  페이징 객체
     * @return 상품 목록(메인)
     */
    public Page<ProductDocument> getMainPageProducts(Pageable pageable) {

        List<ProductDocument> allProducts = productElasticsearchRepository.findByCustomScore();
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), allProducts.size());

        List<ProductDocument> pageContent;

        if (start >= allProducts.size()) {
            pageContent = Collections.emptyList();
        } else {
            pageContent = allProducts.subList(start, end);
        }

        return new PageImpl<>(pageContent, pageable, allProducts.size());

    }

    /**
     * 검색 후 지정한 한 페이지 분량의 상품 목록 조회
     * @param pageable      페이지 정보
     * @param searchWord    검색어
     * @param subCategoryId 서브 카테고리
     * @return 상품 목록(검색)
     */
    public Page<ProductListDTO> getList(Pageable pageable, String searchWord, Integer subCategoryId) {

        String keyword = (searchWord != null && !searchWord.isBlank()) ? searchWord : null;

        Page<ProductListDTO> dtoPage = productRepository.mainProducts(keyword, subCategoryId, pageable);

        log.debug("조회된 결과 페이지: {}", dtoPage.getContent());
        return dtoPage;

    }

    /**
     * 필터링된 상품 목록 검색
     * @param pageable          // 페이징 객체
     * @param muniIds           // 선택된 지역 목록
     * @param subCategoryId     // 선택된 카테고리
     * @param maxPrice          // 최대 가격
     * @param minPrice          // 최소 가격
     * @return                  // Page<ProductListDTO>
     */
    public Page<ProductListDTO> getFilteredList(
            Pageable pageable,
            List<Long> muniIds,         // 지역 ID
            Integer subCategoryId,      // 서브 카테고리 ID
            Integer maxPrice,           // 최대 가격
            Integer minPrice,           // 최소 가격
            String keyword) {

        NativeQueryBuilder queryBuilder = NativeQuery.builder();

        BoolQuery.Builder boolQueryBuilder = QueryBuilders.bool();
        List<co.elastic.clients.elasticsearch._types.query_dsl.Query> filters = new ArrayList<>();
        List<co.elastic.clients.elasticsearch._types.query_dsl.Query> mustQueries = new ArrayList<>();

        // 지역 필터(여러 지역으로 매핑할 생각이라면 term -> terms)
        if (muniIds != null && !muniIds.isEmpty()) {
            List<FieldValue> fieldValues = muniIds.stream()
                    .map(FieldValue::of)
                    .toList();

            filters.add(QueryBuilders.terms(t -> t
                    .field("muniIds")
                    .terms(tf -> tf.value(fieldValues))
            ));

            log.info("Region Filter Added: muniIds = {}", muniIds);
        }

        // 카테고리 필터
        if (subCategoryId != null) {
            filters.add(QueryBuilders.term(t -> t.field("subcategory_id").value(subCategoryId)));
            log.info("Category Filter Added: subCategoryId = {}", subCategoryId);
        }

        // 가격 범위 필터
        if (minPrice != null || maxPrice != null) {
            filters.add(QueryBuilders.range(r->r.number(n -> {
                        n.field("price");

                        if (minPrice != null) {
                            n.gte((double) minPrice);
                        }
                        if (maxPrice != null) {
                            n.lte((double) maxPrice);
                        }
                        return n;
                    })
            ));
            log.info("Price Filter Added: minPrice = {}, maxPrice = {}", minPrice, maxPrice);
        }

        // 상품 상태 필터
        filters.add(QueryBuilders.term(t -> t.field("status").value(ProductStatus.NEW.name())));
        log.info("Default Status Filter Added: NEW");

        // 검색어 필터
        if (keyword != null && !keyword.trim().isEmpty()) {
            mustQueries.add(QueryBuilders.multiMatch(m -> m
                    .query(keyword)
                    .fields("name", "description")
            ));
            log.info("Keyword Search Added: keyword = {}", keyword);
        }

        // BoolQuery 에 must 와 filter 조건 추가
        if (!mustQueries.isEmpty()) {
            boolQueryBuilder.must(mustQueries);
        }

        if (!filters.isEmpty()) {
            boolQueryBuilder.filter(filters);
        }

        // 아무런 쿼리 조건이 없을 경우 -> match_all (모든 상품 조회)
        if (mustQueries.isEmpty() && filters.isEmpty()) {
            queryBuilder.withQuery(co.elastic.clients.elasticsearch._types.query_dsl.Query.of(
                    q -> q.matchAll(new MatchAllQuery.Builder().build())
            ));
        } else {
            queryBuilder.withQuery(co.elastic.clients.elasticsearch._types.query_dsl.Query.of(
                    q -> q.bool(boolQueryBuilder.build())
            ));
        }

        // 페이지네이션 및 정렬 적용
        queryBuilder.withPageable(pageable);

        // 정렬 선택x -> Default DESC modifiedDate
        if (pageable.getSort().isUnsorted()) {
            queryBuilder.withSort(Sort.by(Sort.Direction.DESC, "modifiedDate"));
        }

        // 쿼리 실행
        Query searchQuery = queryBuilder.build();
        SearchHits<ProductDocument> searchHits = elasticsearchOperations.search(searchQuery, ProductDocument.class);

        // SearchHists -> Page<ProductDocument> 변환
        List<ProductDocument> productDocuments = searchHits.stream()
                .map(SearchHit::getContent)
                .toList();

        long totalCount = searchHits.getTotalHits();

        // Spring Page 객체로 변환
        Page<ProductDocument> productPage = new PageImpl<>(
                productDocuments,
                pageable,
                totalCount
        );

        return productPage.map(doc -> ProductListDTO.builder()
                .pid(doc.getPid())
                .name(doc.getName())
                .price(doc.getPrice())
                .viewCount(doc.getViewCount())
                .status(ProductStatus.valueOf(doc.getStatus()))
                .imageUrl(doc.getImageUrl())
                .likeCount(doc.getLikeCount())
                .build());

    }

    /**
     * 상품 등록
     *
     * @param productDTO
     * @param mainUpload
     * @param detailUploads
     * @throws IOException
     */
    @CacheEvict(value = "mainProducts", allEntries = true)
    public Long write(ProductDTO productDTO, MultipartFile mainUpload, List<MultipartFile> detailUploads) throws IOException {
        ProductStatus status;
        try {
            status = ProductStatus.valueOf(productDTO.getStatus());
        } catch (IllegalArgumentException | NullPointerException e) {
            log.warn("알 수 없는 ProductStatus: {}. 기본값 NEW 사용", productDTO.getStatus());
            status = ProductStatus.NEW;
        }


        // 회원 확인
        MemberEntity memberEntity = memberRepository.findById(productDTO.getUserNo())
                .orElseThrow(() -> new EntityNotFoundException("회원 아이디가 없습니다. 다시 로그인해주세요."));

        // 하위 카테고리 확인
        SubCategory subCategory = subCategoriesRepository.findById(productDTO.getSubcategoryId())
                .orElseThrow(() -> new EntityNotFoundException("하위 카테고리를 찾을 수 없습니다."));

        // Product 엔티티 생성
        Product product = Product.builder()
                .name(productDTO.getName())
                .price(productDTO.getPrice())
                .viewCount(0)
                .status(status)
                .description(productDTO.getDescription())
                .build();

        product.bindSubcategory(subCategory);
        product.setMember(memberEntity);

        // 메인 이미지 S3 업로드
        if (mainUpload != null && !mainUpload.isEmpty()) {
            String mainUrl = s3Service.uploadFile(mainUpload);
            ProductImage mainImage = ProductImage.builder()
                    .imageUrl(mainUrl)
                    .isMain(true)
                    .product(product)
                    .imageSeq(1)
                    .build();
            product.addImage(mainImage);

            product.setImageUrl(mainUrl);
        }
        // 상세 이미지 S3 업로드
        if (detailUploads != null && !detailUploads.isEmpty()) {
            int seq = 2;
            for (MultipartFile upload : detailUploads) {
                if (upload == null || upload.isEmpty()) {
                    continue; // 빈 파일은 건너뛰기
                }

                String fileUrl = s3Service.uploadFile(upload);

                ProductImage detailImage = ProductImage.builder()
                        .imageUrl(fileUrl)
                        .isMain(false)
                        .product(product)
                        .imageSeq(seq++)
                        .build();

                product.addImage(detailImage);
            }
        }


        productRepository.save(product);//Product 저장

        // ProductRegion 생성

        Long muniId = productDTO.getMunicipalityId();
        Municipality municipality = municipalityRepository.findById(muniId)
                .orElseThrow(() -> new EntityNotFoundException("시구를 찾을 수 없습니다."));

        ProductRegionEntity productRegion = new ProductRegionEntity();
        productRegion.setId(new ProductRegion(product.getPid(), municipality.getMId()));
        productRegion.setProduct(product);
        productRegion.setMunicipality(municipality);

        productRegionRepository.save(productRegion);

        /* ES 동기화 */
        productElasticsearchRepository.save(ProductDocument.fromProduct(product, 0L));

        return product.getPid();
    }


    public ProductDTO getProduct(Long pid) {
        Product product = productRepository.findById(pid)
                .orElseThrow(() -> new EntityNotFoundException("상품을 찾을 수 없습니다."));

        List<ProductImageDTO> images = product.getImages().stream()
                .map(img -> ProductImageDTO.builder()
                        .imageId(img.getId())
                        .imageUrl(img.getImageUrl())
                        .isMain(img.getIsMain())
                        .imageSeq(img.getImageSeq())
                        .build())
                .collect(Collectors.toList());

        // 수정: 메인 이미지 ID 찾기
        Long mainImageId = images.stream()
                .filter(img -> img.getImageSeq() != null && img.getImageSeq() == 1)
                .map(ProductImageDTO::getImageId)
                .findFirst()
                .orElse(null);

        Integer subcategoryId = null;
        Integer categoryId = null;

        if (product.getSubCategory() != null) {
            subcategoryId = product.getSubCategory().getId();
            categoryId = product.getSubCategory().getCategory().getId();
        }

        Long selectedRegionId = null;
        Long selectedPrefectureId = null;
        Long municipalityId = null;

        if (product.getProductRegions() != null && !product.getProductRegions().isEmpty()) {
            ProductRegionEntity productRegion = product.getProductRegions().get(0);
            if (productRegion != null && productRegion.getMunicipality() != null) {
                Municipality municipality = productRegion.getMunicipality();
                municipalityId = municipality.getMId();
                if (municipality.getPrefecture() != null) {
                    Prefecture prefecture = municipality.getPrefecture();
                    selectedPrefectureId = prefecture.getPrefId();
                    if (prefecture.getRegion() != null) {
                        Region region = prefecture.getRegion();
                        selectedRegionId = region.getRgnId();
                    }
                }
            }
        }

        // 수정: 이 메소드에서 View에 필요한 모든 목록 데이터를 생성해서 DTO에 담아 반환
        List<CategoriesDTO> allCategories = getAllCategories();
        List<RegionDTO> allRegions = getAllRegions();

        List<SubCategoriesDTO> allSubCategories = Collections.emptyList();
        if(categoryId != null) {
            allSubCategories = getSubCategoriesByCategories(categoryId);
        }

        List<PrefectureDTO> allPrefectures = Collections.emptyList();
        if (selectedRegionId != null) {
            allPrefectures = getPrefecturesByRegion(selectedRegionId);
        }

        List<MunicipalityDTO> allMunicipalities = Collections.emptyList();
        if (selectedPrefectureId != null) {
            allMunicipalities = getMunicipalitiesByPrefecture(selectedPrefectureId);
        }

        return ProductDTO.builder()
                .pid(product.getPid())
                .name(product.getName())
                .price(product.getPrice())
                .status(product.getStatus().name())
                .viewCount(product.getViewCount())
                .description(product.getDescription())
                .createDate(product.getCreatedDate())
                .modifiedDate(product.getModifiedDate())
                .imageUrl(product.getImageUrl())
                .mainImageId(mainImageId) // 수정: 메인 이미지 ID 추가
                .images(images)
                .subcategoryId(subcategoryId)
                .categoryId(categoryId)
                .userNo(product.getMember().getUserNo())
                .municipalityId(municipalityId)
                .selectedRegionId(selectedRegionId)
                .selectedPrefectureId(selectedPrefectureId)
                // 수정: View 렌더링에 필요한 전체 목록을 DTO에 포함
                .regions(allRegions)
                .prefectures(allPrefectures)
                .municipalities(allMunicipalities)
                .subCategories(allSubCategories)
                .categories(allCategories)
                .build();
    }

    /**
     * 상품 수정
     * @param productDTO
     * @param mainUpload
     * @param detailUploads
     * @throws Exception
     */
    @CacheEvict(value = "mainProducts", allEntries = true)
    public void update(ProductDTO productDTO, MultipartFile mainUpload, List<MultipartFile> detailUploads) throws Exception {
        Product product = productRepository.findById(productDTO.getPid())
                .orElseThrow(() -> new EntityNotFoundException("게시글이 없습니다."));

        if (!product.getMember().getUserNo().equals(productDTO.getUserNo())) {
            throw new RuntimeException("수정 권한이 없습니다.");
        }

        product.setName(productDTO.getName());
        product.setPrice(productDTO.getPrice());
        product.setStatus(ProductStatus.NEW);
        product.setDescription(productDTO.getDescription());

        SubCategory subCategory = subCategoriesRepository.findById(productDTO.getSubcategoryId())
                .orElseThrow(() -> new EntityNotFoundException("하위 카테고리를 찾을 수 없습니다."));
        product.bindSubcategory(subCategory);

        Long muniId = productDTO.getMunicipalityId();
        Municipality municipality = municipalityRepository.findById(muniId)
                .orElseThrow(() -> new EntityNotFoundException("시구를 찾을 수 없습니다."));

        // 기존 상품-지역 연결 삭제
        if (!product.getProductRegions().isEmpty()) {
            product.getProductRegions().clear();
        }

        productRegionRepository.deleteByProduct(product);

        // 새 상품-지역 연결 생성
        ProductRegionEntity newProductRegion = new ProductRegionEntity();

        newProductRegion.setId(new ProductRegion(product.getPid(), municipality.getMId()));
        newProductRegion.setProduct(product);
        newProductRegion.setMunicipality(municipality);

        product.addProductRegion(newProductRegion);

        // --- 이미지 처리 로직 수정 ---

        // 1. 기존 이미지끼리 메인 <-> 상세 교체
        if (productDTO.getNewMainImageId() != null) {
            Optional<ProductImage> oldMainOpt = product.getImages().stream().filter(img -> img.getImageSeq() == 1).findFirst();
            Optional<ProductImage> newMainOpt = productImageRepository.findById(productDTO.getNewMainImageId());

            if (oldMainOpt.isPresent() && newMainOpt.isPresent()) {
                ProductImage oldMain = oldMainOpt.get();
                ProductImage newMain = newMainOpt.get();

                // 순서 교환
                int oldMainPrevSeq = oldMain.getImageSeq();
                int newMainPrevSeq = newMain.getImageSeq();

                oldMain.setImageSeq(newMainPrevSeq);
                oldMain.setMain(false);

                newMain.setImageSeq(oldMainPrevSeq);
                newMain.setMain(true);

                // Product 엔티티의 대표 이미지 URL 업데이트
                product.setImageUrl(newMain.getImageUrl());
            }
        }

        // 2. 삭제할 상세 이미지 처리
        if (productDTO.getDeletedImageIds() != null && !productDTO.getDeletedImageIds().isEmpty()) {
            for (Long imageId : productDTO.getDeletedImageIds()) {
                productImageRepository.findById(imageId).ifPresent(img -> {
                    if (img.getImageSeq() != 1) { // 메인 이미지는 삭제 불가
                        s3Service.deleteFile(img.getImageUrl());
                        product.getImages().remove(img); // 컬렉션에서 제거
                        productImageRepository.delete(img);
                    }
                });
            }
        }

        // 3. 새로운 메인 이미지 업로드 (기존 메인 이미지 교체)
        if (mainUpload != null && !mainUpload.isEmpty()) {
            product.getImages().stream().filter(img -> img.getImageSeq() == 1).findFirst().ifPresent(oldMain -> {
                s3Service.deleteFile(oldMain.getImageUrl());
                product.getImages().remove(oldMain);
                productImageRepository.delete(oldMain);
            });

            String newMainUrl = s3Service.uploadFile(mainUpload);
            ProductImage newMainImage = ProductImage.builder()
                    .imageUrl(newMainUrl)
                    .isMain(true)
                    .product(product)
                    .imageSeq(1)
                    .build();
            product.addImage(newMainImage);
            product.setImageUrl(newMainUrl);
        }

        // 4. 새로운 상세 이미지 추가
        if (detailUploads != null && !detailUploads.isEmpty()) {
            for (MultipartFile file : detailUploads) {
                if (file != null && !file.isEmpty()) {
                    String url = s3Service.uploadFile(file);
                    // 새 순번 계산 (기존 상세 이미지 개수 + 2)
                    int maxSeq = product.getImages().stream()
                            .filter(img -> !img.getIsMain())
                            .mapToInt(ProductImage::getImageSeq)
                            .max().orElse(1);

                    product.addImage(ProductImage.builder()
                            .imageUrl(url)
                            .isMain(false)
                            .product(product)
                            .imageSeq(maxSeq + 1)
                            .build());
                }
            }
        }

        // 5. 최종 순번 재정렬 (메인 이미지 교체 등으로 순서가 꼬였을 수 있으므로)
        int detailSeq = 2;
        List<ProductImage> sortedImages = product.getImages().stream()
                .sorted((a,b) -> a.getImageSeq().compareTo(b.getImageSeq()))
                .collect(Collectors.toList());

        for (ProductImage img : sortedImages) {
            if (img.getIsMain()) {
                img.setImageSeq(1);
            } else {
                img.setImageSeq(detailSeq++);
            }
        }

        Product updatedProduct = productRepository.save(product);

        List<Long> productIds = List.of(updatedProduct.getPid());
        Long currentLikeCount = 0L;
        List<Object[]> likeCounts = wishListRepository.countLikesByProductIds(productIds);
        if (!likeCounts.isEmpty()) {
            currentLikeCount = (Long) likeCounts.get(0)[1];
        }

        productElasticsearchRepository.save(ProductDocument.fromProduct(updatedProduct, currentLikeCount));

    }

    /**
     * 모든 상위 카테고리 조회
     * @return 카테고리 목록
     */
    public List<CategoriesDTO> getAllCategories() {
        List<Category> categories = categoriesRepository.findAll();
        return categories.stream()
                .map(c -> new CategoriesDTO(c.getId(), c.getName()))
                .toList();
    }

    /**
     * 서브 카테고리 ID -> SubCategoriesDTO 변환
     * @param subcategoryId 서브 카테고리 ID
     * @return SubCategoriesDTO
     */
    public SubCategoriesDTO getSubCategoriesById(Integer subcategoryId) {
        SubCategory subCategory = subCategoriesRepository.findById(subcategoryId)
                .orElseThrow(() -> new EntityNotFoundException("서브 카테고리를 찾을 수 없습니다."));
        return SubCategoriesDTO.builder()
                .subcategoryId(subCategory.getId())
                .subcategoryName(subCategory.getName())
                .categoryId(subCategory.getCategory().getId())
                .build();
    }

    /**
     * 카테고리 ID -> List<SubCategoriesDTO> 반환
     * @param categoryId    카테고리 ID
     * @return List<SubCategoriesDTO>
     */
    public List<SubCategoriesDTO> getSubCategoriesByCategories(Integer categoryId) {
        List<SubCategory> subCategories = subCategoriesRepository.findByCategory_Id(categoryId);
        return subCategories.stream()
                .map(sub -> SubCategoriesDTO.builder()
                        .subcategoryId(sub.getId())
                        .subcategoryName(sub.getName())
                        .categoryId(sub.getCategory().getId())
                        .build())
                .collect(Collectors.toList());
    }

    // ------------------ 지역 선택 관련 ------------------

    // Region ID로 Prefecture DTO + 그 안의 Municipality DTO까지 가져오기
    //@RequestParam("regionId")임시추가2
    public List<PrefectureDTO> getPrefecturesWithMunicipalities(@RequestParam("regionId") Long regionId) {
        // Repository 인스턴스 이름 소문자로 사용
        Region region = regionRepository.findById(regionId)
                .orElseThrow(() -> new EntityNotFoundException("Region not found"));

     //   return prefectureRepository.findByRegion_RgnId(region)
        return prefectureRepository.findByRegion_RgnId(region.getRgnId()) //임시추가2
                .stream()
                .map(pref -> {
                    // Prefecture -> Municipality DTO
                  //  List<MunicipalityDTO> municipalities = municipalityRepository.findByPrefecture_PrefId(pref) 임시수정2
                    List<MunicipalityDTO> municipalities = municipalityRepository.findByPrefecture_PrefId(pref.getPrefId())// 임시추가2
                            .stream()
                            .map(muni -> MunicipalityDTO.builder()
                                    .mId(muni.getMId())
                                    .muniName(muni.getMuniName())
                                    .build())
                            .toList();

                    return PrefectureDTO.builder()
                            .prefId(pref.getPrefId())
                            .prefName(pref.getPrefName())
                            .municipalities(municipalities)
                            .build();
                })
                .toList();
    }

    public List<MunicipalityDTO> getMunicipalitiesByPrefecture(Long prefectureId) {
        Prefecture pref = prefectureRepository.findById(prefectureId)
                .orElseThrow(() -> new EntityNotFoundException("Prefecture not found"));


       // return municipalityRepository.findByPrefecture_PrefId(pref) 임시수정
        return municipalityRepository.findByPrefecture_PrefId(pref.getPrefId())///  임시추가
                .stream()
                .map(muni -> MunicipalityDTO.builder()
                        .mId(muni.getMId())
                        .muniName(muni.getMuniName())
                        .build())
                .toList();
    }

    public List<RegionDTO> getAllRegions() {
        return regionRepository.findAll()
                .stream()
                .map(r -> new RegionDTO(r.getRgnId(), r.getRgnName()))
                .toList();
    }

    /**
     * 삭제
     *
     * @param pid         상품id
     * @param loginUserId 로그인id
     */
    @CacheEvict(value = "mainProducts", allEntries = true)
    public void delete(Long pid, String loginUserId) {

        Product product = productRepository.findById(pid)
                .orElseThrow(() -> new EntityNotFoundException("상품을 찾을 수 없습니다. id=" + pid));
        MemberEntity loginUser = memberRepository.findByUserId(loginUserId)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));

        Long loginUserNo = loginUser.getUserNo();
        Long productOwnerNo = product.getMember().getUserNo();
        // 로그인 유저와 글 작성자가 동일할 경우 삭제 가능
        if (!loginUserNo.equals(productOwnerNo)) {
            throw new RuntimeException("글 작성자가 아닙니다. 삭제 권한이 없습니다.");
        }
        // 상태가 new인 경우에만 삭제 가능
        if (product.getStatus() != ProductStatus.NEW) {
            throw new RuntimeException("거래중, 거래완료, 신고중 상태에서는 삭제가 불가능합니다.");
        }

        productRepository.delete(product);

        /* ES 반영 */
        productElasticsearchRepository.deleteById(pid);
    }

    public List<PrefectureDTO> getPrefecturesByRegion(Long regionId) {
        return prefectureRepository.findByRegion_RgnId(regionId).stream()
                .map(pref -> PrefectureDTO.builder()
                        .prefId(pref.getPrefId())
                        .prefName(pref.getPrefName())
                        .build())
                .toList(); }

    // ------------------ 상점 관련 ------------------

    /**
     * 특정 상점의 상품 목록을 페이징하여 조회
     * @param userNo 상점 주인의 ID
     * @param pageable 페이징 정보
     * @return 페이징된 상품 DTO 목록
     */
    public Page<ProductListDTO> getProductsByStore(Long userNo, ProductStatus status, Pageable pageable) {

        if (!memberRepository.existsById(userNo)) {
            throw new EntityNotFoundException("해당 ID의 회원을 찾을 수 없습니다: " + userNo);
        }
        return productRepository.findProductsByUserNo(userNo, status, pageable);

    }

}

