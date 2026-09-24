package com.plantahub.api.support;

import com.plantahub.api.domain.auth.AppUser;
import com.plantahub.api.domain.auth.enums.UserRole;
import com.plantahub.api.domain.catalog.content.ProductContent;
import com.plantahub.api.domain.catalog.PlanType;
import com.plantahub.api.domain.catalog.Product;
import com.plantahub.api.domain.catalog.ProductPlanType;
import com.plantahub.api.domain.catalog.enums.ProductStatus;
import com.plantahub.api.domain.catalog.DigitalAsset;
import com.plantahub.api.domain.downloads.DownloadEntitlement;
import com.plantahub.api.domain.downloads.EntitlementAsset;
import com.plantahub.api.domain.orders.Order;
import com.plantahub.api.domain.orders.OrderItem;
import com.plantahub.api.domain.orders.OrderItemSelection;
import com.plantahub.api.domain.orders.enums.OrderStatus;
import com.plantahub.api.repository.AppUserRepository;
import com.plantahub.api.repository.DigitalAssetRepository;
import com.plantahub.api.repository.DownloadEntitlementRepository;
import com.plantahub.api.repository.EntitlementAssetRepository;
import com.plantahub.api.repository.OrderRepository;
import com.plantahub.api.repository.PlanTypeRepository;
import com.plantahub.api.repository.ProductPlanTypeRepository;
import com.plantahub.api.repository.ProductRepository;
import org.springframework.boot.test.context.TestComponent;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/** Fixtures para os testes de integracao. */
@TestComponent
public class TestDataFactory {

    private static final AtomicInteger SEQ = new AtomicInteger();

    private final AppUserRepository userRepo;
    private final ProductRepository productRepo;
    private final PlanTypeRepository planTypeRepo;
    private final ProductPlanTypeRepository pptRepo;
    private final OrderRepository orderRepo;
    private final DownloadEntitlementRepository entitlementRepo;
    private final DigitalAssetRepository assetRepo;
    private final EntitlementAssetRepository pinRepo;

    public TestDataFactory(AppUserRepository userRepo,
                           ProductRepository productRepo,
                           PlanTypeRepository planTypeRepo,
                           ProductPlanTypeRepository pptRepo,
                           OrderRepository orderRepo,
                           DownloadEntitlementRepository entitlementRepo,
                           DigitalAssetRepository assetRepo,
                           EntitlementAssetRepository pinRepo) {
        this.userRepo = userRepo;
        this.productRepo = productRepo;
        this.planTypeRepo = planTypeRepo;
        this.pptRepo = pptRepo;
        this.orderRepo = orderRepo;
        this.entitlementRepo = entitlementRepo;
        this.assetRepo = assetRepo;
        this.pinRepo = pinRepo;
    }

    public static String unique(String prefix) {
        return prefix + "-" + SEQ.incrementAndGet() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    /** Codigo de colecao valido para o CHECK {@code ^[A-Z][A-Z0-9_]{1,39}$}. */
    public static String uniqueCode(String prefix) {
        return (prefix + "_" + SEQ.incrementAndGet() + "_" + UUID.randomUUID().toString().substring(0, 6))
                .toUpperCase()
                .replace('-', '_');
    }

    /**
     * Conteudo minimo que satisfaz a validacao de publicacao.
     *
     * <p>O record tem 22 componentes e quase todos sao opcionais; escrever a lista de nulos
     * em cada teste esconderia, no meio do ruido, o unico campo que o teste realmente quer.
     */
    public static ProductContent contentWithHeadline(String headline) {
        return new ProductContent(
                headline, null, null,
                null, null, List.of(),
                null, null, List.of(),
                null, null, List.of(),
                null, null, List.of(),
                null, null, List.of(),
                null, null,
                List.of(), List.of());
    }

    public AppUser user() {
        return user(unique("user") + "@plantahub.test");
    }

    public AppUser user(String email) {
        return userRepo.save(AppUser.builder()
                .email(email.toLowerCase())
                .passwordHash("{noop}irrelevante")
                .fullName("Usuario de Teste")
                .role(UserRole.USER)
                .createdAt(Instant.now())
                .active(true)
                .build());
    }

    public Product product(ProductStatus status) {
        return product(unique("prod"), "casas", unique("slug"), status);
    }

    public Product product(String id, String category, String slug, ProductStatus status) {
        Instant now = Instant.now();
        Product p = new Product();
        p.setId(id);
        p.setCategory(category);
        p.setSlug(slug);
        p.setName("Produto " + slug);
        p.setShortDesc("Descricao curta");
        p.setAreaM2(100);
        p.setBasePriceCents(0);
        p.setCustomizable(true);
        p.setCreatedAt(now);
        p.setUpdatedAt(now);
        p.applyStatus(status);
        return productRepo.save(p);
    }

    /** Colecao comum: comprável, nao acompanha outras ofertas. */
    public PlanType collection(String code) {
        return collection(code, true, false);
    }

    public PlanType collection(String code, boolean purchasable, boolean bundledWithEveryOffer) {
        return planTypeRepo.save(PlanType.builder()
                .code(code)
                .name("Colecao " + code)
                .description("Colecao de teste")
                .purchasable(purchasable)
                .bundledWithEveryOffer(bundledWithEveryOffer)
                .active(true)
                .sortOrder(1)
                .build());
    }

    public ProductPlanType offer(Product product, PlanType planType, int priceCents) {
        return offer(product, planType, priceCents, true);
    }

    public ProductPlanType offer(Product product, PlanType planType, int priceCents, boolean available) {
        return pptRepo.save(ProductPlanType.builder()
                .product(product)
                .planType(planType)
                .priceCents(priceCents)
                .includedInBundle(false)
                .available(available)
                .sortOrder(1)
                .build());
    }

    public Order order(AppUser user, OrderStatus status, Product product, PlanType planType, int priceCents) {
        Order order = new Order();
        order.setUser(user);
        order.setStatus(status);
        order.setCurrency("BRL");
        order.setTotalCents(priceCents);
        order.setCreatedAt(Instant.now());
        if (status == OrderStatus.PAID) {
            order.setPaidAt(Instant.now());
        }

        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setProduct(product);
        item.setQuantity(1);
        item.setUnitPriceCents(priceCents);
        item.setTotalCents(priceCents);

        OrderItemSelection selection = new OrderItemSelection();
        selection.setOrderItem(item);
        selection.setPlanType(planType);
        selection.setPriceCents(priceCents);
        item.getSelections().add(selection);

        order.getItems().add(item);

        return orderRepo.save(order);
    }

    /** Um arquivo pendurado numa colecao de um produto. */
    public DigitalAsset asset(ProductPlanType offer, String filename) {
        return assetRepo.save(DigitalAsset.builder()
                .productPlanType(offer)
                .version(1)
                .filename(filename)
                // Chave literal e unica: e assim que o bucket real se comporta.
                .storageKey("products/" + offer.getProduct().getId()
                        + "/" + offer.getPlanType().getCode()
                        + "/" + unique("k") + "/" + filename)
                .sizeBytes(1024L)
                .kind("FILE")
                .keyScheme("LEGACY")
                .reconciliationStatus("UNKNOWN")
                .sortOrder(0)
                .createdAt(Instant.now())
                .build());
    }

    /** Vincula um arquivo a uma concessao, como a pinagem faz no pagamento. */
    public EntitlementAsset pin(DownloadEntitlement entitlement, DigitalAsset asset) {
        return pinRepo.save(EntitlementAsset.builder()
                .entitlement(entitlement)
                .digitalAsset(asset)
                .source(EntitlementAsset.Source.PURCHASED)
                .build());
    }

    public DownloadEntitlement entitlement(AppUser user, Order order, Product product, PlanType planType) {
        return entitlementRepo.save(DownloadEntitlement.builder()
                .user(user)
                .order(order)
                .product(product)
                .planType(planType)
                .grantedAt(Instant.now())
                .build());
    }
}
