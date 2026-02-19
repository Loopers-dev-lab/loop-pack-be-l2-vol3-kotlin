# Round 3 커밋 히스토리 재작성 계획

## 배경

`b8e1180` 커밋이 107 files / 6570 insertions 통짜로 들어가 있어 PR 리뷰가 불가능한 수준.
Round 3 전체(63 커밋)를 `main` 기준으로 soft reset 후, 도메인/레이어 단위로 잘게 재커밋한다.

## 실행 절차

```bash
# 1. 백업 브랜치 생성
git branch backup/round3-original

# 2. soft reset — 코드는 그대로, 커밋만 해체
git reset --soft main

# 3. 전부 unstage
git reset HEAD .

# 4. 아래 커밋 계획에 따라 순서대로 add + commit
# 5. 각 커밋 후 컴파일 확인 (최소 마지막에 전체 빌드+테스트)
# 6. 완료 후 backup 브랜치 삭제
```

**주의**: `git reset --soft main`은 워킹 디렉토리를 건드리지 않는다. 코드 손실 위험 없음.

---

## 커밋 계획 (19개)

### 1. `chore: 프로젝트 설정 및 빌드 구성`

```
.gitignore
.editorconfig
.coderabbit.yaml
.github/pull_request_template.md
build.gradle.kts
docker/infra-compose.yml
docker/grafana/prometheus.yml
modules/jpa/src/main/resources/jpa.yml
modules/kafka/src/main/resources/kafka.yml
apps/commerce-api/src/test/resources/docker-java.properties
supports/logging/src/main/resources/appenders/json-console-appender.xml
supports/logging/src/main/resources/appenders/plain-console-appender.xml
supports/logging/src/main/resources/appenders/slack-appender.xml
```

### 2. `chore: Claude Code 스킬 및 개발 도구 구성`

```
.claude/skills/README.md
.claude/skills/*/SKILL.md   (전체 스킬 파일)
README.md
```

### 3. `docs: Round 3 설계 문서 작성`

```
docs/design/01-requirements.md
docs/design/02-sequence-diagrams.md
docs/design/03-class-diagram.md
docs/design/04-erd.md
docs/design/05-flowcharts.md
docs/requirements/round2-requirements-analysis.md
docs/requirements/round3-requirements-analysis.md
docs/note/round2-design-decisions.md
docs/note/round3-decisions.md
plan.md
```

### 4. `docs: CLAUDE.md 아키텍처 가이드 작성`

```
CLAUDE.md
```

### 5. `refactor: interfaces/support 패키지 분리 및 공통 인프라`

```
apps/commerce-api/src/main/kotlin/com/loopers/interfaces/support/ApiResponse.kt
apps/commerce-api/src/main/kotlin/com/loopers/interfaces/support/ApiControllerAdvice.kt
apps/commerce-api/src/main/kotlin/com/loopers/interfaces/support/Constants.kt
apps/commerce-api/src/main/kotlin/com/loopers/interfaces/support/DateTimeRange.kt
apps/commerce-api/src/main/kotlin/com/loopers/interfaces/support/PageResultExtensions.kt
apps/commerce-api/src/main/kotlin/com/loopers/interfaces/support/config/SwaggerConfig.kt
apps/commerce-api/src/main/kotlin/com/loopers/interfaces/support/config/WebMvcConfig.kt
apps/commerce-api/src/main/kotlin/com/loopers/interfaces/support/auth/AuthUser.kt
apps/commerce-api/src/main/kotlin/com/loopers/interfaces/support/auth/AuthUserArgumentResolver.kt
apps/commerce-api/src/main/kotlin/com/loopers/interfaces/support/interceptor/AuthInterceptor.kt
apps/commerce-api/src/main/kotlin/com/loopers/interfaces/support/interceptor/AdminInterceptor.kt
apps/commerce-api/src/main/kotlin/com/loopers/support/error/ErrorType.kt
apps/commerce-api/src/main/kotlin/com/loopers/domain/PageResult.kt
```

### 6. `feat: User 도메인 — Entity, VO, Repository, Service`

```
apps/commerce-api/src/main/kotlin/com/loopers/domain/user/User.kt
apps/commerce-api/src/main/kotlin/com/loopers/domain/user/UserCommand.kt
apps/commerce-api/src/main/kotlin/com/loopers/domain/user/UserRepository.kt
apps/commerce-api/src/main/kotlin/com/loopers/domain/user/UserService.kt
apps/commerce-api/src/main/kotlin/com/loopers/domain/user/Email.kt
apps/commerce-api/src/main/kotlin/com/loopers/domain/user/LoginId.kt
apps/commerce-api/src/main/kotlin/com/loopers/domain/user/Name.kt
apps/commerce-api/src/main/kotlin/com/loopers/domain/user/Password.kt
apps/commerce-api/src/main/kotlin/com/loopers/infrastructure/user/UserJpaRepository.kt
apps/commerce-api/src/main/kotlin/com/loopers/infrastructure/user/UserRepositoryImpl.kt
apps/commerce-api/src/main/kotlin/com/loopers/application/auth/AuthService.kt
```

### 7. `test: User 도메인 단위/통합 테스트`

```
apps/commerce-api/src/test/kotlin/com/loopers/domain/user/UserTest.kt
apps/commerce-api/src/test/kotlin/com/loopers/domain/user/UserTestFixture.kt
apps/commerce-api/src/test/kotlin/com/loopers/domain/user/UserServiceTest.kt
apps/commerce-api/src/test/kotlin/com/loopers/domain/user/UserServiceIntegrationTest.kt
apps/commerce-api/src/test/kotlin/com/loopers/application/auth/AuthServiceTest.kt
apps/commerce-api/src/test/kotlin/com/loopers/interfaces/support/auth/AuthUserArgumentResolverTest.kt
apps/commerce-api/src/test/kotlin/com/loopers/interfaces/support/interceptor/AuthInterceptorTest.kt
apps/commerce-api/src/test/kotlin/com/loopers/interfaces/support/interceptor/AdminInterceptorTest.kt
```

### 8. `feat: Catalog 도메인 — Entity, VO, Repository, Service`

```
apps/commerce-api/src/main/kotlin/com/loopers/domain/catalog/CatalogCommand.kt
apps/commerce-api/src/main/kotlin/com/loopers/domain/catalog/CatalogService.kt
apps/commerce-api/src/main/kotlin/com/loopers/domain/catalog/ProductDetail.kt
apps/commerce-api/src/main/kotlin/com/loopers/domain/catalog/brand/Brand.kt
apps/commerce-api/src/main/kotlin/com/loopers/domain/catalog/brand/BrandName.kt
apps/commerce-api/src/main/kotlin/com/loopers/domain/catalog/brand/BrandRepository.kt
apps/commerce-api/src/main/kotlin/com/loopers/domain/catalog/product/Product.kt
apps/commerce-api/src/main/kotlin/com/loopers/domain/catalog/product/ProductRepository.kt
apps/commerce-api/src/main/kotlin/com/loopers/domain/catalog/product/ProductSort.kt
apps/commerce-api/src/main/kotlin/com/loopers/domain/catalog/product/Price.kt
apps/commerce-api/src/main/kotlin/com/loopers/domain/catalog/product/Stock.kt
apps/commerce-api/src/main/kotlin/com/loopers/infrastructure/catalog/brand/BrandJpaRepository.kt
apps/commerce-api/src/main/kotlin/com/loopers/infrastructure/catalog/brand/BrandRepositoryImpl.kt
apps/commerce-api/src/main/kotlin/com/loopers/infrastructure/catalog/product/ProductJpaRepository.kt
apps/commerce-api/src/main/kotlin/com/loopers/infrastructure/catalog/product/ProductRepositoryImpl.kt
```

### 9. `test: Catalog 도메인 단위 테스트`

```
apps/commerce-api/src/test/kotlin/com/loopers/domain/catalog/brand/BrandTest.kt
apps/commerce-api/src/test/kotlin/com/loopers/domain/catalog/brand/BrandNameTest.kt
apps/commerce-api/src/test/kotlin/com/loopers/domain/catalog/brand/BrandTestFixture.kt
apps/commerce-api/src/test/kotlin/com/loopers/domain/catalog/brand/FakeBrandRepository.kt
apps/commerce-api/src/test/kotlin/com/loopers/domain/catalog/product/ProductTest.kt
apps/commerce-api/src/test/kotlin/com/loopers/domain/catalog/product/ProductTestFixture.kt
apps/commerce-api/src/test/kotlin/com/loopers/domain/catalog/product/PriceTest.kt
apps/commerce-api/src/test/kotlin/com/loopers/domain/catalog/product/StockTest.kt
apps/commerce-api/src/test/kotlin/com/loopers/domain/catalog/product/FakeProductRepository.kt
apps/commerce-api/src/test/kotlin/com/loopers/domain/catalog/CatalogServiceTest.kt
```

### 10. `feat: Point 도메인 — Entity, VO, Repository, Service`

```
apps/commerce-api/src/main/kotlin/com/loopers/domain/point/UserPoint.kt
apps/commerce-api/src/main/kotlin/com/loopers/domain/point/PointHistory.kt
apps/commerce-api/src/main/kotlin/com/loopers/domain/point/PointHistoryType.kt
apps/commerce-api/src/main/kotlin/com/loopers/domain/point/Point.kt
apps/commerce-api/src/main/kotlin/com/loopers/domain/point/PointCommand.kt
apps/commerce-api/src/main/kotlin/com/loopers/domain/point/UserPointRepository.kt
apps/commerce-api/src/main/kotlin/com/loopers/domain/point/PointHistoryRepository.kt
apps/commerce-api/src/main/kotlin/com/loopers/domain/point/UserPointService.kt
apps/commerce-api/src/main/kotlin/com/loopers/domain/point/PointChargingService.kt
apps/commerce-api/src/main/kotlin/com/loopers/infrastructure/point/UserPointJpaRepository.kt
apps/commerce-api/src/main/kotlin/com/loopers/infrastructure/point/UserPointRepositoryImpl.kt
apps/commerce-api/src/main/kotlin/com/loopers/infrastructure/point/PointHistoryJpaRepository.kt
apps/commerce-api/src/main/kotlin/com/loopers/infrastructure/point/PointHistoryRepositoryImpl.kt
```

### 11. `test: Point 도메인 단위 테스트`

```
apps/commerce-api/src/test/kotlin/com/loopers/domain/point/UserPointTest.kt
apps/commerce-api/src/test/kotlin/com/loopers/domain/point/PointTest.kt
apps/commerce-api/src/test/kotlin/com/loopers/domain/point/FakeUserPointRepository.kt
apps/commerce-api/src/test/kotlin/com/loopers/domain/point/FakePointHistoryRepository.kt
apps/commerce-api/src/test/kotlin/com/loopers/domain/point/UserPointServiceTest.kt
apps/commerce-api/src/test/kotlin/com/loopers/domain/point/PointChargingServiceTest.kt
```

### 12. `feat: Order 도메인 — Entity, VO, Repository, Service`

```
apps/commerce-api/src/main/kotlin/com/loopers/domain/order/Order.kt
apps/commerce-api/src/main/kotlin/com/loopers/domain/order/OrderItem.kt
apps/commerce-api/src/main/kotlin/com/loopers/domain/order/OrderCommand.kt
apps/commerce-api/src/main/kotlin/com/loopers/domain/order/OrderProductInfo.kt
apps/commerce-api/src/main/kotlin/com/loopers/domain/order/OrderRepository.kt
apps/commerce-api/src/main/kotlin/com/loopers/domain/order/OrderService.kt
apps/commerce-api/src/main/kotlin/com/loopers/domain/order/Quantity.kt
apps/commerce-api/src/main/kotlin/com/loopers/infrastructure/order/OrderJpaRepository.kt
apps/commerce-api/src/main/kotlin/com/loopers/infrastructure/order/OrderRepositoryImpl.kt
```

### 13. `test: Order 도메인 단위 테스트`

```
apps/commerce-api/src/test/kotlin/com/loopers/domain/order/OrderTest.kt
apps/commerce-api/src/test/kotlin/com/loopers/domain/order/QuantityTest.kt
apps/commerce-api/src/test/kotlin/com/loopers/domain/order/FakeOrderRepository.kt
apps/commerce-api/src/test/kotlin/com/loopers/domain/order/OrderServiceTest.kt
```

### 14. `feat: Like 도메인 — Entity, Repository, Service`

```
apps/commerce-api/src/main/kotlin/com/loopers/domain/like/Like.kt
apps/commerce-api/src/main/kotlin/com/loopers/domain/like/LikeRepository.kt
apps/commerce-api/src/main/kotlin/com/loopers/domain/like/LikeService.kt
apps/commerce-api/src/main/kotlin/com/loopers/infrastructure/like/LikeJpaRepository.kt
apps/commerce-api/src/main/kotlin/com/loopers/infrastructure/like/LikeRepositoryImpl.kt
```

### 15. `test: Like 도메인 단위 테스트`

```
apps/commerce-api/src/test/kotlin/com/loopers/domain/like/LikeTest.kt
apps/commerce-api/src/test/kotlin/com/loopers/domain/like/FakeLikeRepository.kt
apps/commerce-api/src/test/kotlin/com/loopers/domain/like/LikeServiceTest.kt
```

### 16. `feat: Application 계층 — Facade (UserFacade, OrderFacade, LikeFacade)`

```
apps/commerce-api/src/main/kotlin/com/loopers/application/user/UserFacade.kt
apps/commerce-api/src/main/kotlin/com/loopers/application/order/OrderFacade.kt
apps/commerce-api/src/main/kotlin/com/loopers/application/like/LikeFacade.kt
apps/commerce-api/src/main/kotlin/com/loopers/application/like/LikeInfo.kt
```

### 17. `test: Application 계층 Facade 테스트`

```
apps/commerce-api/src/test/kotlin/com/loopers/application/user/UserFacadeTest.kt
apps/commerce-api/src/test/kotlin/com/loopers/application/order/OrderFacadeTest.kt
apps/commerce-api/src/test/kotlin/com/loopers/application/like/LikeFacadeTest.kt
```

### 18. `feat: Presentation 계층 — Controller, ApiSpec, Dto, HTTP 테스트 파일`

```
apps/commerce-api/src/main/kotlin/com/loopers/interfaces/api/user/UserV1ApiSpec.kt
apps/commerce-api/src/main/kotlin/com/loopers/interfaces/api/user/UserV1Controller.kt
apps/commerce-api/src/main/kotlin/com/loopers/interfaces/api/user/UserV1Dto.kt
apps/commerce-api/src/main/kotlin/com/loopers/interfaces/api/product/ProductV1ApiSpec.kt
apps/commerce-api/src/main/kotlin/com/loopers/interfaces/api/product/ProductV1Controller.kt
apps/commerce-api/src/main/kotlin/com/loopers/interfaces/api/product/ProductV1Dto.kt
apps/commerce-api/src/main/kotlin/com/loopers/interfaces/api/product/ProductAdminV1ApiSpec.kt
apps/commerce-api/src/main/kotlin/com/loopers/interfaces/api/product/ProductAdminV1Controller.kt
apps/commerce-api/src/main/kotlin/com/loopers/interfaces/api/product/ProductAdminV1Dto.kt
apps/commerce-api/src/main/kotlin/com/loopers/interfaces/api/brand/BrandV1ApiSpec.kt
apps/commerce-api/src/main/kotlin/com/loopers/interfaces/api/brand/BrandV1Controller.kt
apps/commerce-api/src/main/kotlin/com/loopers/interfaces/api/brand/BrandV1Dto.kt
apps/commerce-api/src/main/kotlin/com/loopers/interfaces/api/brand/BrandAdminV1ApiSpec.kt
apps/commerce-api/src/main/kotlin/com/loopers/interfaces/api/brand/BrandAdminV1Controller.kt
apps/commerce-api/src/main/kotlin/com/loopers/interfaces/api/brand/BrandAdminV1Dto.kt
apps/commerce-api/src/main/kotlin/com/loopers/interfaces/api/order/OrderV1ApiSpec.kt
apps/commerce-api/src/main/kotlin/com/loopers/interfaces/api/order/OrderV1Controller.kt
apps/commerce-api/src/main/kotlin/com/loopers/interfaces/api/order/OrderV1Dto.kt
apps/commerce-api/src/main/kotlin/com/loopers/interfaces/api/order/OrderAdminV1ApiSpec.kt
apps/commerce-api/src/main/kotlin/com/loopers/interfaces/api/order/OrderAdminV1Controller.kt
apps/commerce-api/src/main/kotlin/com/loopers/interfaces/api/order/OrderAdminV1Dto.kt
apps/commerce-api/src/main/kotlin/com/loopers/interfaces/api/like/LikeV1ApiSpec.kt
apps/commerce-api/src/main/kotlin/com/loopers/interfaces/api/like/LikeV1Controller.kt
apps/commerce-api/src/main/kotlin/com/loopers/interfaces/api/like/LikeV1Dto.kt
apps/commerce-api/src/main/kotlin/com/loopers/interfaces/api/point/PointV1ApiSpec.kt
apps/commerce-api/src/main/kotlin/com/loopers/interfaces/api/point/PointV1Controller.kt
apps/commerce-api/src/main/kotlin/com/loopers/interfaces/api/point/PointV1Dto.kt
http/commerce-api/catalog.http
http/commerce-api/like.http
http/commerce-api/order.http
http/commerce-api/point.http
```

### 19. `test: E2E 테스트`

```
apps/commerce-api/src/test/kotlin/com/loopers/interfaces/api/user/UserV1ApiE2ETest.kt
apps/commerce-api/src/test/kotlin/com/loopers/interfaces/api/like/LikeV1ApiE2ETest.kt
apps/commerce-api/src/test/kotlin/com/loopers/interfaces/api/order/OrderV1ApiE2ETest.kt
apps/commerce-api/src/test/kotlin/com/loopers/interfaces/api/point/PointV1ApiE2ETest.kt
apps/commerce-api/src/test/kotlin/com/loopers/interfaces/api/ExampleV1ApiE2ETest.kt
```

---

## 삭제된 파일 처리

round3에서 삭제된 example 패키지 파일들은 커밋 5번(support 패키지) 이전에
`chore: example 패키지 삭제`로 별도 처리하거나, 해당 도메인 커밋에서 함께 삭제한다.

삭제 대상:
```
apps/commerce-api/src/main/kotlin/com/loopers/application/example/ExampleFacade.kt
apps/commerce-api/src/main/kotlin/com/loopers/application/example/ExampleInfo.kt
apps/commerce-api/src/main/kotlin/com/loopers/domain/example/ExampleModel.kt
apps/commerce-api/src/main/kotlin/com/loopers/domain/example/ExampleRepository.kt
apps/commerce-api/src/main/kotlin/com/loopers/domain/example/ExampleService.kt
apps/commerce-api/src/main/kotlin/com/loopers/infrastructure/example/ExampleJpaRepository.kt
apps/commerce-api/src/main/kotlin/com/loopers/infrastructure/example/ExampleRepositoryImpl.kt
apps/commerce-api/src/test/kotlin/com/loopers/domain/example/ExampleModelTest.kt
apps/commerce-api/src/test/kotlin/com/loopers/domain/example/ExampleServiceIntegrationTest.kt
apps/commerce-api/src/main/kotlin/com/loopers/interfaces/api/example/ExampleV1ApiSpec.kt
apps/commerce-api/src/main/kotlin/com/loopers/interfaces/api/example/ExampleV1Controller.kt
apps/commerce-api/src/main/kotlin/com/loopers/interfaces/api/example/ExampleV1Dto.kt
```

## 기타 모듈 변경

아래 파일들은 커밋 1(프로젝트 설정)에 포함:
```
apps/commerce-batch/src/main/kotlin/com/loopers/batch/listener/StepMonitorListener.kt
apps/commerce-batch/src/test/kotlin/com/loopers/CommerceBatchApplicationTest.kt
apps/commerce-batch/src/test/kotlin/com/loopers/job/demo/DemoJobE2ETest.kt
modules/kafka/src/main/kotlin/com/loopers/config/kafka/KafkaConfig.kt
```

---

## 검증

모든 커밋 완료 후:
```bash
./gradlew ktlintCheck
./gradlew :apps:commerce-api:test
```

## 롤백

```bash
# 문제 발생 시 원본 복원
git reset --hard backup/round3-original
```
