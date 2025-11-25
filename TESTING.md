# Testing Guide & TDD Workflow

This document outlines the testing strategy and Test-Driven Development (TDD) workflow for the `gathr` project.

## TDD Workflow (Red-Green-Refactor)

1.  **Red**: Write a failing test that defines the desired behavior.
    -   Ensure the test fails for the right reason (e.g., "AssertionError: expected 200 but got 404").
2.  **Green**: Write the minimum amount of code to make the test pass.
    -   Do not worry about code quality or optimization yet.
3.  **Refactor**: Improve the code structure, readability, and performance without changing behavior.
    -   Ensure tests stay green throughout this phase.

## Backend Testing (Spring Boot)

### Unit Tests (`*Test.java`)
-   **Location**: `src/test/java/com/gathr/service` (or other packages)
-   **Tools**: JUnit 5, Mockito, AssertJ
-   **Scope**: Test individual classes in isolation. Mock all dependencies.
-   **Naming**: `ClassNameTest` (e.g., `AuthServiceTest`)

```java
@ExtendWith(MockitoExtension.class)
class MyServiceTest {
    @Mock private MyRepository repository;
    @InjectMocks private MyService service;

    @Test
    void shouldReturnValue_WhenConditionMet() {
        // Given
        when(repository.find()).thenReturn("data");
        // When
        String result = service.doSomething();
        // Then
        assertThat(result).isEqualTo("data");
    }
}
```

### Integration Tests (`*IntegrationTest.java`)
-   **Location**: `src/test/java/com/gathr/controller` (or `integration`)
-   **Tools**: `@SpringBootTest`, `@AutoConfigureMockMvc`, H2 Database (or Testcontainers)
-   **Scope**: Test the interaction between components (Controller -> Service -> Repository -> DB).
-   **Naming**: `FeatureIntegrationTest`

```java
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MyControllerIntegrationTest {
    @Autowired private MockMvc mockMvc;

    @Test
    void shouldReturn200_WhenRequestIsValid() throws Exception {
        mockMvc.perform(get("/api/resource"))
               .andExpect(status().isOk());
    }
}
```

## Frontend Testing (React Native)

### Component Tests
-   **Location**: `__tests__` folder alongside components.
-   **Tools**: Jest, React Native Testing Library (RNTL)
-   **Scope**: Render components and assert on UI elements.

```javascript
test('renders correctly', () => {
  const { getByText } = render(<MyComponent />);
  expect(getByText('Hello')).toBeTruthy();
});
```

### Hook Tests
-   **Location**: `src/hooks/__tests__`
-   **Tools**: `@testing-library/react-hooks`
-   **Scope**: Test custom hooks logic.

## Running Tests

-   **Backend**: `mvn test`
-   **Frontend**: `npm test` (in `frontend` directory)
