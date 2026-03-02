# 테스트 패턴

- `@Nested` + `@DisplayName`(한국어) BDD 스타일, **3A 원칙** (Arrange → Act → Assert)
- **통합 테스트**: `@SpringBootTest`, `@AfterEach`에서 `databaseCleanUp.truncateAllTables()`
- **E2E 테스트**: `@SpringBootTest(webEnvironment = RANDOM_PORT)` + `TestRestTemplate`
- **단위 테스트**: 도메인 로직은 외부 의존성 없이 검증한다. `@SpringBootTest`를 사용하지 않으며, **Mockito(`@Mock`, `@InjectMocks` 등) 사용을 엄격히 금지한다.**
  대신 인메모리 컬렉션을 활용한 Fake Repository(`FakeProductRepository` 등)를 직접 구현하여 상태를 검증한다.
- MySQL/Redis는 TestContainers 자동 구동 (프로파일: `test`), 타임존: `Asia/Seoul`
- 상세 테스트 작성 절차는 `/red`, `/e2e` 스킬 참고
