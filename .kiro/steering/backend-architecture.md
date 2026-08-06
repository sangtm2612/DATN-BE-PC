---
inclusion: always
---

# Backend Architecture Rules - KinhDuanPC

## Layered Architecture

### 1. Controller Layer
**Trách nhiệm:**
- Nhận HTTP requests và trả về HTTP responses
- Validate request parameters (sử dụng `@Valid`)
- Authentication/Authorization check (sử dụng `@PreAuthorize`)
- Gọi Service layer để xử lý business logic
- **KHÔNG ĐƯỢC** chứa business logic
- **KHÔNG ĐƯỢC** truy cập Repository trực tiếp
- **KHÔNG ĐƯỢC** trả về Entity, phải dùng DTO

**Cấu trúc:**
```java
@RestController
@RequestMapping("/api-path")
@RequiredArgsConstructor
@Tag(name = "Name", description = "Description")
public class XxxController {
    
    private final XxxService xxxService;
    
    @GetMapping
    public ResponseEntity<ApiResponse<List<XxxDTO>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(xxxService.findAll()));
    }
    
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<XxxDTO>> create(@Valid @RequestBody XxxRequest request) {
        return ResponseEntity.ok(ApiResponse.success(xxxService.create(request)));
    }
}
```

### 2. Service Layer
**Trách nhiệm:**
- Chứa toàn bộ business logic
- Transaction management (sử dụng `@Transactional`)
- Mapping giữa Entity và DTO
- Validation business rules
- Gọi Repository để truy cập database
- Có thể gọi các Service khác
- **KHÔNG ĐƯỢC** trả về Entity ra ngoài, phải convert sang DTO

**Cấu trúc:**
```java
@Service
@RequiredArgsConstructor
@Transactional
public class XxxService {
    
    private final XxxRepository xxxRepo;
    private final RelatedRepository relatedRepo;
    
    public List<XxxDTO> findAll() {
        return xxxRepo.findAll().stream()
            .map(this::toDTO)
            .toList();
    }
    
    public XxxDTO create(XxxRequest request) {
        // Business validation
        validateBusinessRules(request);
        
        // Create entity
        Xxx entity = Xxx.builder()
            .field1(request.getField1())
            .build();
            
        Xxx saved = xxxRepo.save(entity);
        return toDTO(saved);
    }
    
    private XxxDTO toDTO(Xxx entity) {
        return XxxDTO.builder()
            .id(entity.getId())
            .field1(entity.getField1())
            .build();
    }
    
    private void validateBusinessRules(XxxRequest request) {
        if (xxxRepo.existsByUniqueField(request.getUniqueField())) {
            throw AppException.conflict("Duplicate field");
        }
    }
}
```

### 3. Repository Layer
**Trách nhiệm:**
- Truy cập database
- Chỉ chứa query methods
- **KHÔNG ĐƯỢC** chứa business logic

**Cấu trúc:**
```java
@Repository
public interface XxxRepository extends JpaRepository<Xxx, Long> {
    Optional<Xxx> findBySlug(String slug);
    List<Xxx> findByIsActiveTrue();
    boolean existsByUniqueField(String field);
    
    @Query("SELECT x FROM Xxx x WHERE x.condition = :value")
    List<Xxx> customQuery(@Param("value") String value);
}
```

### 4. DTO Layer
**Trách nhiệm:**
- Data Transfer Object giữa các layers
- Tránh expose Entity ra ngoài
- Tránh lỗi Hibernate lazy loading serialization

**Loại DTO:**

**Request DTO** - Nhận data từ client:
```java
@Data
public class XxxRequest {
    @NotBlank(message = "Field is required")
    private String field1;
    
    @Min(value = 0, message = "Must be >= 0")
    private Integer field2;
}
```

**Response DTO** - Trả data cho client:
```java
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class XxxDTO {
    private Long id;
    private String field1;
    private Integer field2;
    private LocalDateTime createdAt;
    
    // Nested objects should be flattened or use separate DTO
    private Long relatedId;
    private String relatedName;
}
```

## Rules for DTO

### ❌ WRONG - Trả Entity trực tiếp:
```java
// Controller
@GetMapping
public ResponseEntity<ApiResponse<List<Product>>> getAll() {
    return ResponseEntity.ok(ApiResponse.success(productRepo.findAll()));
}
```

**Vấn đề:**
- Hibernate lazy loading serialization error
- Expose toàn bộ entity structure
- Không kiểm soát được data trả về
- Circular reference issues

### ✅ CORRECT - Dùng DTO:
```java
// Service
public List<ProductDTO> findAll() {
    return productRepo.findAll().stream()
        .map(this::toDTO)
        .toList();
}

private ProductDTO toDTO(Product p) {
    return ProductDTO.builder()
        .id(p.getId())
        .name(p.getName())
        .price(p.getPrice())
        .categoryId(p.getCategory().getId())
        .categoryName(p.getCategory().getName())
        .build();
}

// Controller
@GetMapping
public ResponseEntity<ApiResponse<List<ProductDTO>>> getAll() {
    return ResponseEntity.ok(ApiResponse.success(productService.findAll()));
}
```

## Transaction Management

### Service Layer Transaction:
```java
@Service
@Transactional  // Class-level: tất cả methods đều transactional
public class OrderService {
    
    @Transactional(readOnly = true)  // Optimize for read operations
    public OrderDTO findById(Long id) {
        // ...
    }
    
    @Transactional  // Write operation
    public OrderDTO create(OrderRequest request) {
        // Multiple repository calls trong cùng 1 transaction
        Order order = orderRepo.save(...);
        orderItemRepo.saveAll(...);
        productRepo.updateStock(...);
        return toDTO(order);
    }
}
```

## Exception Handling

**Sử dụng AppException:**
```java
// Not found
if (!xxxRepo.existsById(id)) {
    throw AppException.notFound("Resource name");
}

// Conflict
if (xxxRepo.existsByUniqueField(field)) {
    throw AppException.conflict("Duplicate field");
}

// Bad request
if (quantity < 0) {
    throw AppException.badRequest("Invalid quantity");
}

// Forbidden
if (!hasPermission(user, resource)) {
    throw AppException.forbidden("No permission");
}
```

## Naming Conventions

### Controller Methods:
- `getAll()` - GET list
- `getById()` / `getBySlug()` - GET single
- `create()` - POST create
- `update()` - PUT update
- `delete()` - DELETE

### Service Methods:
- `findAll()` - Query list
- `findById()` / `findBySlug()` - Query single
- `create()` - Create new
- `update()` - Update existing
- `delete()` - Delete
- `validateXxx()` - Private validation methods
- `toDTO()` / `toEntity()` - Private mapping methods

### Repository Methods:
- `findByXxx()` - Query methods
- `existsByXxx()` - Check existence
- `countByXxx()` - Count
- `deleteByXxx()` - Delete by condition

## Migration Strategy

Khi refactor existing code:

1. **Tạo Service class** nếu chưa có
2. **Tạo DTOs** (Request/Response)
3. **Di chuyển logic từ Controller → Service**
4. **Thêm mapping methods** trong Service (toDTO, toEntity)
5. **Update Controller** để gọi Service thay vì Repository
6. **Update return types** từ Entity → DTO
7. **Test lại endpoints**

## Example: Complete Flow

```java
// 1. Request DTO
@Data
public class CreateProductRequest {
    @NotBlank private String name;
    @NotNull private Long categoryId;
    @Min(0) private BigDecimal price;
}

// 2. Response DTO
@Getter @Setter @Builder
public class ProductDTO {
    private Long id;
    private String name;
    private String slug;
    private BigDecimal price;
    private Long categoryId;
    private String categoryName;
    private Integer stockQty;
}

// 3. Service
@Service
@RequiredArgsConstructor
@Transactional
public class ProductService {
    private final ProductRepository productRepo;
    private final CategoryRepository categoryRepo;
    
    public ProductDTO create(CreateProductRequest request) {
        // Validate
        Category category = categoryRepo.findById(request.getCategoryId())
            .orElseThrow(() -> AppException.notFound("Category"));
            
        if (productRepo.existsByName(request.getName())) {
            throw AppException.conflict("Product name already exists");
        }
        
        // Create entity
        Product product = Product.builder()
            .name(request.getName())
            .slug(generateSlug(request.getName()))
            .price(request.getPrice())
            .category(category)
            .stockQty(0)
            .build();
            
        Product saved = productRepo.save(product);
        return toDTO(saved);
    }
    
    @Transactional(readOnly = true)
    public List<ProductDTO> findAll() {
        return productRepo.findAll().stream()
            .map(this::toDTO)
            .toList();
    }
    
    private ProductDTO toDTO(Product p) {
        return ProductDTO.builder()
            .id(p.getId())
            .name(p.getName())
            .slug(p.getSlug())
            .price(p.getPrice())
            .categoryId(p.getCategory().getId())
            .categoryName(p.getCategory().getName())
            .stockQty(p.getStockQty())
            .build();
    }
    
    private String generateSlug(String name) {
        // Logic tạo slug
        return name.toLowerCase().replaceAll("\\s+", "-");
    }
}

// 4. Controller
@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
@Tag(name = "Products")
public class ProductController {
    private final ProductService productService;
    
    @GetMapping
    public ResponseEntity<ApiResponse<List<ProductDTO>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(productService.findAll()));
    }
    
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProductDTO>> create(
            @Valid @RequestBody CreateProductRequest request) {
        return ResponseEntity.ok(ApiResponse.success(productService.create(request)));
    }
}
```

## Common Pitfalls to Avoid

1. ❌ **Logic trong Controller**
2. ❌ **Trả Entity thay vì DTO**
3. ❌ **Repository trong Controller**
4. ❌ **Không dùng @Transactional trong Service**
5. ❌ **Nested objects trong DTO gây lazy loading error**
6. ❌ **Validation logic rải rác khắp nơi**
7. ❌ **Không handle exceptions đúng cách**

## Performance Optimization

### 1. N+1 Query Problem:
```java
// ❌ BAD - N+1 queries
List<Product> products = productRepo.findAll();
return products.stream()
    .map(p -> ProductDTO.builder()
        .categoryName(p.getCategory().getName()) // Lazy load!
        .build())
    .toList();

// ✅ GOOD - Join fetch
@Query("SELECT p FROM Product p JOIN FETCH p.category")
List<Product> findAllWithCategory();
```

### 2. Use Projections for read-only:
```java
public interface ProductProjection {
    Long getId();
    String getName();
    BigDecimal getPrice();
    @Value("#{target.category.name}")
    String getCategoryName();
}

@Query("SELECT p FROM Product p JOIN p.category")
List<ProductProjection> findAllProjections();
```

## Testing

Service layer nên được unit test:
```java
@ExtendWith(MockitoExtension.class)
class ProductServiceTest {
    @Mock private ProductRepository productRepo;
    @Mock private CategoryRepository categoryRepo;
    @InjectMocks private ProductService productService;
    
    @Test
    void create_ShouldThrowException_WhenCategoryNotFound() {
        // Arrange
        CreateProductRequest request = new CreateProductRequest();
        request.setCategoryId(999L);
        when(categoryRepo.findById(999L)).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThrows(AppException.class, () -> productService.create(request));
    }
}
```

## Summary Checklist

Khi implement feature mới:
- [ ] Tạo Request DTO với validation
- [ ] Tạo Response DTO
- [ ] Tạo Service class
- [ ] Business logic trong Service
- [ ] Mapping methods (toDTO/toEntity)
- [ ] Controller chỉ delegate tới Service
- [ ] Không trả Entity trực tiếp
- [ ] Thêm @Transactional cho Service
- [ ] Handle exceptions properly
- [ ] Test Service layer
