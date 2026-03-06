# 테스트 패턴

- @Nested + @DisplayName(한국어) BDD 스타일, **3A 원칙** (Arrange → Act → Assert)
- **통합 테스트**: @SpringBootTest, @AfterEach에서 databaseCleanUp.truncateAllTables()
- **E2E 테스트**: @SpringBootTest(webEnvironment = RANDOM_PORT) + TestRestTemplate
- **단위 테스트**: 도메인 로직은 외부 의존성 없이 검증. @SpringBootTest 사용 금지. **Mockito 사용을 엄격히 금지.** 인메모리 컬렉션 활용한 Fake Repository 직접 구현
- MySQL/Redis는 TestContainers 자동 구동 (프로파일: test), 타임존: Asia/Seoul
- 상세 절차는 /red, /e2e 스킬 참고
