# 시퀀스 다이어그램

## 브랜드 정보 조회

사용자가 특정 브랜드의 상세 정보를 조회하는 API입니다.

BrandController가 요청을 받아 BrandService를 통해 브랜드 정보를 조회하고,
브랜드가 존재하지 않을 경우 404 에러를 반환합니다.
성공 시 브랜드의 상세 정보(이름, 로고, 설명 등)를 반환합니다.

```mermaid
sequenceDiagram
    participant Client
    participant BrandController
    participant BrandService
    participant BrandRepository

    Client ->> BrandController: GET /api/v1/brands/{brandId}
    BrandController ->> BrandService: getBrandInfo(brandId)
    BrandService ->> BrandRepository: findByBrandId(brandId)
    BrandRepository -->> BrandService: BrandModel

    alt 브랜드 정보가 없는 경우
        BrandService -->> BrandController: 예외 발생
        BrandController -->> Client: 에러 응답 (404 Not Found)
    else 브랜드 정보가 있는 경우
        BrandService -->> BrandController: 브랜드 상세 정보
        BrandController -->> Client: 브랜드 정보 응답 (200 OK)
    end
```

## 상품 목록 조회

상품 목록을 페이징하여 조회하는 API입니다.

brandId 파라미터를 통해 특정 브랜드의 상품만 필터링할 수 있으며,
brandId가 없으면 전체 상품을 조회합니다.
정렬 옵션과 페이지 정보를 지정할 수 있으며, 기본값은 page=0, size=20입니다.

```mermaid
sequenceDiagram
    participant Client
    participant ProductController
    participant ProductService
    participant ProductRepository

    Client ->> ProductController: GET /api/v1/products
    Note left of ProductController: 쿼리 파라미터<br/> brandId, sort, page=0, size=20
    ProductController ->> ProductService: getProducts(brandId, pageable)
    alt brandId 있을 경우
        ProductService ->> ProductRepository: findAllByBrandId(brandId, pageable)
    else 전체 조회
        ProductService ->> ProductRepository: findAll(pageable)
    end

    ProductRepository -->> ProductService: Page<ProductModel>
    ProductService -->> ProductController: 상품 목록
    ProductController -->> Client: 상품 목록 응답 (200 OK)

```

## 상품 정보 조회

사용자가 특정 상품의 상세 정보를 조회하는 API입니다.

ProductController가 요청을 받아 ProductService를 통해 상품 정보를 조회하고,
상품이 존재하지 않을 경우 404 에러를 반환합니다.
성공 시 상품의 상세 정보(이름, 가격, 이미지, 좋아요 수 등)를 반환합니다.

```mermaid
sequenceDiagram
    participant Client
    participant ProductController
    participant ProductService
    participant ProductRepository

    Client ->> ProductController: GET /api/v1/products/{productId}
    ProductController ->> ProductService: getProductInfo(productId)
    ProductService ->> ProductRepository: findByProductId(productId)
    ProductRepository -->> ProductService: ProductModel

    alt 상품 정보가 없는 경우
        ProductService -->> ProductController: 예외 발생
        ProductController -->> Client: 에러 응답 (404 Not Found)
    else 상품 정보가 있는 경우
        ProductService -->> ProductController: 상품 상세 정보
        ProductController -->> Client: 상품 정보 응답 (200 OK)
    end
```

## 브랜드 & 상품 ADMIN

### 브랜드 관리 (Admin)

**1. 브랜드 목록 조회**

관리자가 전체 브랜드 목록을 페이징하여 조회하는 Admin API입니다.

삭제된 브랜드를 포함한 모든 브랜드 정보를 조회할 수 있으며,
정렬 옵션과 페이지 정보를 지정할 수 있습니다 (기본값: page=0, size=20).

```mermaid
sequenceDiagram
    participant Admin
    participant BrandAdminController
    participant BrandAdminService
    participant BrandRepository

    Admin ->> BrandAdminController: GET /api-admin/v1/brands
    Note right of Admin: sort, page=0, size=20
    BrandAdminController ->> BrandAdminService: getBrands(pageable)
    BrandAdminService ->> BrandRepository: findAll(pageable)
    BrandRepository -->> BrandAdminService: Page<BrandModel>
    BrandAdminService -->> BrandAdminController: 브랜드 목록
    BrandAdminController -->> Admin: 브랜드 목록 응답 (200 OK)
```

**2. 브랜드 상세 조회**

관리자가 특정 브랜드의 상세 정보를 조회하는 Admin API입니다.

브랜드 ID로 조회하며, 브랜드가 존재하지 않을 경우 404 에러를 반환합니다.
성공 시 브랜드의 모든 정보(삭제 상태 포함)를 반환합니다.

```mermaid
sequenceDiagram
    participant Admin
    participant BrandAdminController
    participant BrandAdminService
    participant BrandRepository

    Admin ->> BrandAdminController: GET /api-admin/v1/brands/{brandId}
    BrandAdminController ->> BrandAdminService: getBrand(brandId)
    BrandAdminService ->> BrandRepository: findById(brandId)
    
    alt 브랜드 존재함
        BrandRepository -->> BrandAdminService: BrandModel
        BrandAdminService -->> BrandAdminController: 브랜드 상세 정보
        BrandAdminController -->> Admin: 브랜드 상세 정보 응답 (200 OK)
    else 브랜드 없음
        BrandRepository -->> BrandAdminService: null
        BrandAdminService -->> BrandAdminController: 예외 발생
        BrandAdminController -->> Admin: 404 Not Found
    end
```

**3. 브랜드 등록**

관리자가 새로운 브랜드를 등록하는 Admin API입니다.

요청 본문에 브랜드 정보(이름, 로고, 설명 등)를 포함하여 전송하고,
BrandRepository에 저장 후 생성된 브랜드 정보를 201 Created와 함께 반환합니다.

```mermaid
sequenceDiagram
    participant Admin
    participant BrandAdminController
    participant BrandAdminService
    participant BrandRepository

    Admin ->> BrandAdminController: POST /api-admin/v1/brands
    BrandAdminController ->> BrandAdminService: createBrand()
    BrandAdminService ->> BrandRepository: save()
    BrandRepository -->> BrandAdminService: Saved BrandModel
    BrandAdminService -->> BrandAdminController: 등록된 브랜드 정보
    BrandAdminController -->> Admin: 브랜드 등록 응답 (201 Created)
```

**4. 브랜드 수정**

관리자가 기존 브랜드의 정보를 수정하는 Admin API입니다.

브랜드 ID로 조회한 후 요청된 속성들을 업데이트하고 저장합니다.
브랜드가 존재하지 않으면 404 에러를 반환하고,
성공 시 수정된 브랜드 정보를 200 OK와 함께 반환합니다.

```mermaid
sequenceDiagram
    participant Admin
    participant BrandAdminController
    participant BrandAdminService
    participant BrandRepository

    Admin ->> BrandAdminController: PUT /api-admin/v1/brands/{brandId}
    BrandAdminController ->> BrandAdminService: updateBrand(brandId)
    BrandAdminService ->> BrandRepository: findById(brandId)
    
    alt 브랜드 존재함
        BrandRepository -->> BrandAdminService: BrandModel
        BrandAdminService ->> BrandAdminService: update properties
        BrandAdminService ->> BrandRepository: save(BrandModel)
        BrandAdminService -->> BrandAdminController: 수정된 브랜드 정보
        BrandAdminController -->> Admin: 브랜드 수정 응답 (200 OK)
    else 브랜드 없음
        BrandAdminService -->> BrandAdminController: 예외 발생
        BrandAdminController -->> Admin: 404 Not Found
    end
```

**5. 브랜드 삭제**

관리자가 브랜드를 삭제하는 Admin API입니다.

Soft Delete 방식으로 isDeleted 플래그를 true로 변경하여 논리적으로 삭제합니다.
브랜드 삭제 시 해당 브랜드의 모든 상품도 자동으로 삭제 상태로 변경됩니다.
브랜드가 존재하지 않으면 404 에러를, 성공 시 200 OK를 반환합니다.

```mermaid
sequenceDiagram
    participant Admin
    participant BrandAdminController
    participant BrandAdminService
    participant BrandRepository
    participant ProductRepository

    Admin ->> BrandAdminController: DELETE /api-admin/v1/brands/{brandId}
    BrandAdminController ->> BrandAdminService: deleteBrand(brandId)
    BrandAdminService ->> BrandRepository: findById(brandId)
    BrandRepository -->> BrandAdminService: BrandModel
    alt 브랜드 존재함
        BrandAdminService ->> BrandAdminService: isDeleted = true
        Note right of BrandAdminService: Soft Delete (상태 변경)
        BrandAdminService ->> BrandRepository: save()
        
        Note right of BrandAdminService: 해당 브랜드의 상품들도 일괄 상태 변경
        BrandAdminService ->> ProductRepository: updateIsDeletedByBrandId(brandId, true)
        
        BrandAdminService -->> BrandAdminController: 삭제 완료
        BrandAdminController -->> Admin: 브랜드 삭제 응답 (200 OK)
    else 브랜드 없음
        BrandAdminService -->> BrandAdminController: 예외 발생
        BrandAdminController -->> Admin: 404 Not Found
    end
```

### 상품 관리 (Admin)

**1. 상품 목록 조회**

관리자가 상품 목록을 페이징하여 조회하는 Admin API입니다.

brandId 파라미터로 특정 브랜드의 상품만 필터링할 수 있으며, 이 경우 브랜드 존재 여부를 먼저 확인합니다.

브랜드가 없으면 404 에러를 반환하고, 성공 시 삭제된 상품을 포함한 목록을 반환합니다.
brandId가 없으면 전체 상품을 조회합니다 (기본값: page=0, size=20).

```mermaid
sequenceDiagram
    participant Admin
    participant ProductAdminController
    participant ProductAdminService
    participant BrandRepository
    participant ProductRepository

    Admin ->> ProductAdminController: GET /api-admin/v1/products
    Note right of Admin: brandId, sort, page=0, size=20
    ProductAdminController ->> ProductAdminService: getProducts(brandId, pageable)
    
    alt brandId 제공됨
        ProductAdminService ->> BrandRepository: existsByBrandId(brandId)
        alt 브랜드 존재함
            BrandRepository -->> ProductAdminService: true
            ProductAdminService ->> ProductRepository: findAllByBrandId(brandId, pageable)
            ProductRepository -->> ProductAdminService: Page<ProductModel>
            ProductAdminService -->> ProductAdminController: 상품 목록
            ProductAdminController -->> Admin: 상품 목록 응답 (200 OK)
        else 브랜드 없음
            BrandRepository -->> ProductAdminService: false
            ProductAdminService -->> ProductAdminController: 예외 발생
            ProductAdminController -->> Admin: 404 Not Found
        end
    else 전체 조회
        ProductAdminService ->> ProductRepository: findAll(pageable)
        ProductRepository -->> ProductAdminService: Page<ProductModel>
        ProductAdminService -->> ProductAdminController: 상품 목록
        ProductAdminController -->> Admin: 상품 목록 응답 (200 OK)
    end
```

**2. 상품 상세 조회**

관리자가 특정 상품의 상세 정보를 조회하는 Admin API입니다.

상품 ID로 조회하며, 상품이 존재하지 않을 경우 404 에러를 반환합니다.\
성공 시 상품의 모든 정보(삭제 상태 포함)를 반환합니다.

```mermaid
sequenceDiagram
    participant Admin
    participant ProductAdminController
    participant ProductAdminService
    participant ProductRepository

    Admin ->> ProductAdminController: GET /api-admin/v1/products/{productId}
    ProductAdminController ->> ProductAdminService: getProduct(productId)
    ProductAdminService ->> ProductRepository: findById(productId)
     
    alt 상품 존재함
        ProductRepository -->> ProductAdminService: ProductModel
        ProductAdminService -->> ProductAdminController: 상품 상세 정보
        ProductAdminController -->> Admin: 상품 상세 정보 응답 (200 OK)
    else 상품 없음
        ProductRepository -->> ProductAdminService: null
        ProductAdminService -->> ProductAdminController: 예외 발생
        ProductAdminController -->> Admin: 404 Not Found
    end
```

**3. 상품 등록**

관리자가 새로운 상품을 등록하는 Admin API입니다.

요청 본문에 상품 정보와 브랜드 ID를 포함하여 전송하며, 브랜드 존재 여부를 먼저 확인합니다.\
브랜드가 없으면 400/404 에러를 반환하고,
브랜드가 있으면 상품을 저장 후 등록된 상품 정보를 200 OK와 함께 반환합니다.

```mermaid
sequenceDiagram
    participant Admin
    participant ProductAdminController
    participant ProductAdminService
    participant BrandRepository
    participant ProductRepository

    Admin ->> ProductAdminController: POST /api-admin/v1/products
    ProductAdminController ->> ProductAdminService: createProduct()
    ProductAdminService ->> BrandRepository: existsByBrandId(brandId)
    
    alt 브랜드 존재함
        BrandRepository -->> ProductAdminService: true
        ProductAdminService ->> ProductRepository: save()
        ProductRepository -->> ProductAdminService: 저장 완료
        ProductAdminService -->> ProductAdminController: 등록된 상품 정보
        ProductAdminController -->> Admin: 상품 등록 응답 (200 OK)
    else 브랜드 없음
        BrandRepository -->> ProductAdminService: false
        ProductAdminService -->> ProductAdminController: 예외 발생
        ProductAdminController -->> Admin: 400 Bad Request / 404 Not Found
    end
```

**4. 상품 정보 수정**

관리자가 기존 상품의 정보를 수정하는 Admin API입니다.

상품 ID로 조회한 후 요청된 속성들을 업데이트하고 저장합니다.
브랜드 정보는 수정할 수 없으며 기존 값을 유지합니다.\
상품이 존재하지 않으면 404 에러를, 성공 시 수정된 상품 정보를 200 OK와 함께 반환합니다.

```mermaid
sequenceDiagram
    participant Admin
    participant ProductAdminController
    participant ProductAdminService
    participant ProductRepository

    Admin ->> ProductAdminController: PUT /api-admin/v1/products/{productId}
    ProductAdminController ->> ProductAdminService: updateProduct(productId)
    ProductAdminService ->> ProductRepository: findById(productId)
    
    alt 상품 존재함
        ProductRepository -->> ProductAdminService: ProductModel
        ProductAdminService ->> ProductAdminService: update properties (except brand)
        Note right of ProductAdminService: 브랜드는 수정 불가 (기존 값 유지)
        ProductAdminService ->> ProductRepository: save()
        ProductAdminService -->> ProductAdminController: 수정된 상품 정보
        ProductAdminController -->> Admin: 상품 수정 응답 (200 OK)
    else 상품 없음
        ProductAdminService -->> ProductAdminController: 예외 발생
        ProductAdminController -->> Admin: 404 Not Found
    end
```

**5. 상품 삭제**

관리자가 상품을 삭제하는 Admin API입니다.

Soft Delete 방식으로 isDeleted 플래그를 true로 변경하여 논리적으로 삭제합니다.\
상품이 존재하지 않으면 404 에러를, 성공 시 200 OK를 반환합니다.

```mermaid
sequenceDiagram
    participant Admin
    participant ProductAdminController
    participant ProductAdminService
    participant ProductRepository

    Admin ->> ProductAdminController: DELETE /api-admin/v1/products/{productId}
    ProductAdminController ->> ProductAdminService: deleteProduct(productId)
    ProductAdminService ->> ProductRepository: findByProductId(productId)
    ProductRepository -->> ProductAdminService: ProductModel
    alt 상품 존재함
        ProductAdminService ->> ProductAdminService: isDeleted = true
        Note right of ProductAdminService: Soft Delete (상태 변경)
        ProductAdminService ->> ProductRepository: save()
        ProductAdminService -->> ProductAdminController: 삭제 완료
        ProductAdminController -->> Admin: 상품 삭제 응답 (200 OK)
    else 상품 없음
        ProductAdminService -->> ProductAdminController: 예외 발생
        ProductAdminController -->> Admin: 404 Not Found
    end
```

## 좋아요 (Likes)

**1. 상품 좋아요 등록**

사용자가 상품에 좋아요를 추가하는 API입니다.

먼저 상품 존재 여부를 확인하고, 상품이 없으면 404 에러를 반환합니다.\
이미 좋아요를 했다면 추가 작업 없이 성공을 반환하고,
좋아요를 하지 않았다면 LikeModel을 저장하고 상품의 좋아요 수를 1 증가시킵니다.

```mermaid
sequenceDiagram
    participant User
    participant LikeController
    participant LikeService
    participant ProductService
    participant LikeRepository
    participant ProductRepository

    User ->> LikeController: POST /api/v1/products/{productId}/likes
    LikeController ->> LikeService: addLike(userId, productId)
    LikeService ->> ProductService: getProduct(productId)
    
    alt 상품 존재함
        ProductService -->> LikeService: ProductModel
        LikeService ->> LikeRepository: existsByUserIdAndProductId(userId, productId)
        alt 이미 좋아요 함
            LikeRepository -->> LikeService: true
            LikeService -->> LikeController: 성공 (추가 작업 없음)
            LikeController -->> User: 좋아요 등록 성공 (201 Created)
        else 좋아요 안 함
            LikeRepository -->> LikeService: false
            LikeService ->> LikeRepository: save()
            LikeService ->> ProductRepository: incrementLikeCount(productId)
            LikeService -->> LikeController: 성공
            LikeController -->> User: 좋아요 등록 성공 (201 Created)
        end
    else 상품 없음
        ProductService -->> LikeController: 예외 발생
        LikeController -->> User: 404 Not Found
    end
```

**2. 상품 좋아요 취소**

사용자가 상품의 좋아요를 취소하는 API입니다.

사용자와 상품 ID로 좋아요 정보를 조회하고,
좋아요 정보가 존재하면 삭제 후 상품의 좋아요 수를 1 감소시킵니다.\
좋아요 정보가 없으면 404 에러를, 성공 시 200 OK를 반환합니다.

```mermaid
sequenceDiagram
    participant User
    participant LikeController
    participant LikeService
    participant LikeRepository
    participant ProductRepository

    User ->> LikeController: DELETE /api/v1/products/{productId}/likes
    LikeController ->> LikeService: removeLike(userId, productId)
    LikeService ->> LikeRepository: findByUserIdAndProductId(userId, productId)

    alt 좋아요 존재함
        LikeRepository -->> LikeService: LikeModel
        LikeService ->> LikeRepository: delete()
        LikeService ->> ProductRepository: decrementLikeCount(productId)
        LikeService -->> LikeController: 성공
        LikeController -->> User: 좋아요 취소 성공 (200 OK)
    else 좋아요 없음
        LikeRepository -->> LikeService: null
        LikeService -->> LikeController: 예외 발생 (Not Found)
        LikeController -->> User: 404 Not Found
    end
```

**3. 좋아요 한 상품 목록 조회**

사용자가 자신이 좋아요 한 상품 목록을 페이징하여 조회하는 API입니다.

사용자 ID로 좋아요 목록을 먼저 조회한 후,
해당하는 상품 ID들로 실제 상품 정보를 조회하여 반환합니다.\
페이징 정보를 포함한 상품 목록을 200 OK와 함께 반환합니다.

```mermaid
sequenceDiagram
    participant User
    participant LikeController
    participant LikeService
    participant LikeRepository
    participant ProductRepository

    User ->> LikeController: GET /api/v1/users/{userId}/likes
    LikeController ->> LikeService: getLikedProducts(userId, pageable)
    LikeService ->> LikeRepository: findAllByUserId(userId, pageable)
    LikeRepository -->> LikeService: Page<LikeModel>
    LikeService ->> ProductRepository: findAllByProductIds(productIds)
    ProductRepository -->> LikeService: List<ProductModel>
    LikeService -->> LikeController: 좋아요 한 상품 목록
    LikeController -->> User: 상품 목록 응답 (200 OK)
```

## 주문 (Orders)

**1. 주문 요청**

사용자가 상품을 주문하는 API입니다.

주문할 각 상품에 대해 존재 여부와 재고를 확인하고, 재고가 부족하면 400 에러를 반환합니다.\
재고가 충분하면 재고를 차감하고 주문 상세 정보(스냅샷)을 생성합니다.\
모든 상품 검증이 완료되면 OrderModel을 저장하고 201 Created와 함께 생성된 주문 정보를 반환합니다.\
상품이 존재하지 않으면 404 에러를 반환합니다.

```mermaid
sequenceDiagram
    participant User
    participant OC as OrderController
    participant OS as OrderService
    participant PS as ProductService
    participant PIS as ProductInventoryService
    participant PR as ProductRepository
    participant PIR as ProductInventoryRepository
    participant OR as OrderRepository

    User ->> OC: POST /api/v1/orders
    OC ->> OS: createOrder(userId, items)
    
    loop 상품별 재고 확인 및 스냅샷 생성
        OS ->> PS: getProduct(productId)
        PS ->> PR: findById(productId)

        alt 상품 있음
            PR -->> OS: ProductModel
            OS ->> PIS: getProductStock(productId)
            PIS ->> PIR: findByProductId(productId)
            PIR -->> OS: ProductInventoryModel

        alt 재고 충분
            OS ->> PIS: decreaseStock(productId, quantity)
            PIS ->> PIR: decreaseStockByProductId(productId, quantity)
            PIR -->> OS: 재고 차감 (stock - quantity)
            OS ->> OS: 주문 상세(Snapshot) 생성
        else 재고 부족
            OS -->> OC: 예외 발생 (Out of Stock)
            OC -->> User: 400 Bad Request
        end

        else 상품 없음
            PR -->> PS: null
            PS -->> OS: 예외 발생 (Not Found)
            OS -->> OC: 예외 발생 (Not Found)
            OC -->> User: 404 Not Found
        end
    end

    OS ->> OR: save(OrderModel)
    OR -->> OS: Saved OrderModel
    OS -->> OC: 생성된 주문 정보
    OC -->> User: 주문 생성 성공 (201 Created)
```

**2. 유저의 주문 목록 조회**

사용자가 자신의 주문 목록을 조회하는 API입니다.

startAt과 endAt 파라미터로 기간을 지정할 수 있으며,
지정된 기간 내의 주문 목록을 페이징하여 반환합니다.\
주문 목록을 200 OK와 함께 반환합니다.

```mermaid
sequenceDiagram
    participant User
    participant OrderController
    participant OrderService
    participant OrderRepository

    User ->> OrderController: GET /api/v1/orders
    Note right of User: startAt, endAt
    OrderController ->> OrderService: getOrders(userId, startAt, endAt, pageable)
    OrderService ->> OrderRepository: findAllByUserIdAndPeriod(userId, startAt, endAt, pageable)
    OrderRepository -->> OrderService: Page<OrderModel>
    OrderService -->> OrderController: 주문 목록
    OrderController -->> User: 주문 목록 응답 (200 OK)
```

**3. 단일 주문 상세 조회**

사용자가 특정 주문의 상세 정보를 조회하는 API입니다.

주문 ID로 주문 정보를 조회하며, 주문이 존재하지 않으면 404 에러를 반환합니다.\
성공 시 주문 상세 정보(주문 상품, 수량, 가격 등)를 반환합니다.

```mermaid
sequenceDiagram
    participant User
    participant OrderController
    participant OrderService
    participant OrderRepository

    User ->> OrderController: GET /api/v1/orders/{orderId}
    OrderController ->> OrderService: getOrder(orderId)
    OrderService ->> OrderRepository: findByOrderId(orderId)
    
    alt 주문 존재함
        OrderRepository -->> OrderService: OrderModel
    else 주문 없음
        OrderRepository -->> OrderService: null
        OrderService -->> OrderController: 예외 발생
        OrderController -->> User: 404 Not Found
    end
```

### 주문 관리 (Order Admin)

**1. 주문 목록 조회**

관리자가 전체 주문 목록을 페이징하여 조회하는 Admin API입니다.

모든 사용자의 주문 정보를 조회할 수 있으며,
페이지 정보를 지정할 수 있습니다 (기본값: page=0, size=20).
주문 목록을 200 OK와 함께 반환합니다.

```mermaid
sequenceDiagram
    participant Admin
    participant OrderAdminController
    participant OrderAdminService
    participant OrderRepository

    Admin ->> OrderAdminController: GET /api-admin/v1/orders
    Note right of Admin: page=0, size=20
    OrderAdminController ->> OrderAdminService: getOrders(pageable)
    OrderAdminService ->> OrderRepository: findAll(pageable)
    OrderRepository -->> OrderAdminService: Page<OrderModel>
    OrderAdminService -->> OrderAdminController: 주문 목록
    OrderAdminController -->> Admin: 주문 목록 응답 (200 OK)
```

**2. 단일 주문 상세 조회**

관리자가 특정 주문의 상세 정보를 조회하는 Admin API입니다.

주문 ID로 주문 정보를 조회하며, 주문이 존재하지 않으면 404 에러를 반환합니다.\
성공 시 주문 상세 정보(사용자 정보, 주문 상품, 수량, 가격 등)를 200 OK와 함께 반환합니다.

```mermaid
sequenceDiagram
    participant Admin
    participant OrderAdminController
    participant OrderAdminService
    participant OrderRepository

    Admin ->> OrderAdminController: GET /api-admin/v1/orders/{orderId}
    OrderAdminController ->> OrderAdminService: getOrder(orderId)
    OrderAdminService ->> OrderRepository: findByOrderId(orderId)

    alt 주문 존재함
        OrderRepository -->> OrderAdminService: OrderModel
        OrderAdminService -->> OrderAdminController: 주문 상세 정보
        OrderAdminController -->> Admin: 주문 상세 정보 응답 (200 OK)
    else 주문 없음
        OrderRepository -->> OrderAdminService: null
        OrderAdminService -->> OrderAdminController: 예외 발생
        OrderAdminController -->> Admin: 404 Not Found
    end
```