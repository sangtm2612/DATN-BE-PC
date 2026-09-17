package com.kinhduanpc.service;

import com.kinhduanpc.config.OrderConfig;
import com.kinhduanpc.dto.order.*;
import com.kinhduanpc.entity.*;
import com.kinhduanpc.repository.WarrantyRepository;
import com.kinhduanpc.exception.AppException;
import com.kinhduanpc.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OrderService {

    // Các chuyển trạng thái hợp lệ cho Staff/Admin
    private static final Map<Order.OrderStatus, Set<Order.OrderStatus>> VALID_TRANSITIONS = Map.of(
        // pending_deposit: admin có thể hủy hoặc xác nhận cọc thủ công (khi nhận chuyển khoản)
        Order.OrderStatus.pending_deposit, Set.of(Order.OrderStatus.pending, Order.OrderStatus.cancelled),
        Order.OrderStatus.pending,    Set.of(Order.OrderStatus.confirmed,   Order.OrderStatus.cancelled),
        Order.OrderStatus.confirmed,  Set.of(Order.OrderStatus.processing,  Order.OrderStatus.cancelled),
        Order.OrderStatus.processing, Set.of(Order.OrderStatus.shipping,    Order.OrderStatus.cancelled),
        Order.OrderStatus.shipping,   Set.of(Order.OrderStatus.delivered),
        Order.OrderStatus.delivered,  Set.of(Order.OrderStatus.completed,   Order.OrderStatus.cancelled)
    );

    private final OrderRepository orderRepo;
    private final OrderHistoryRepository orderHistoryRepo;
    private final CartRepository cartRepo;
    private final ProductRepository productRepo;
    private final UserRepository userRepo;
    private final VoucherRepository voucherRepo;
    private final VoucherService voucherService;
    private final PcBuildRepository pcBuildRepo;
    private final PromotionService promotionService;
    private final ShippingMethodRepository shippingMethodRepo;
    private final OrderShippingRepository orderShippingRepo;
    private final ProductStockByStoreRepository stockByStoreRepo;
    private final StoreRepository storeRepo;
    private final EmailService emailService;
    private final NotificationService notificationService;
    private final VoucherPolicyService voucherPolicyService;
    private final OrderConfig orderConfig;
    private final WarrantyRepository warrantyRepo;

    private static final BigDecimal DEFAULT_SHIPPING_FEE = BigDecimal.valueOf(30000);

    public OrderResponse createOrder(Long userId, String sessionId, CreateOrderRequest req) {
        // Xác định user (có thể null cho guest)
        User user = null;
        if (userId != null) {
            user = userRepo.findById(userId)
                .orElseThrow(() -> AppException.notFound("Người dùng"));
        }
        
        // Lấy cart (ưu tiên userId, fallback sessionId)
        Cart cart;
        if (userId != null) {
            cart = cartRepo.findByUserId(userId)
                .orElseThrow(() -> AppException.badRequest("EMPTY_CART", "Giỏ hàng trống"));
        } else if (sessionId != null && !sessionId.isEmpty()) {
            cart = cartRepo.findBySessionId(sessionId)
                .orElseThrow(() -> AppException.badRequest("EMPTY_CART", "Giỏ hàng trống"));
        } else {
            throw AppException.badRequest("MISSING_USER_OR_SESSION", "Thiếu thông tin user hoặc session");
        }

        if (cart.getItems().isEmpty()) {
            throw AppException.badRequest("EMPTY_CART", "Giỏ hàng không có sản phẩm");
        }

        // Calculate subtotal
        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItem item : cart.getItems()) {
            Product p = item.getProduct();
            if (p.getStockQty() < item.getQuantity()) {
                throw AppException.badRequest("INSUFFICIENT_STOCK",
                    "Sản phẩm " + p.getName() + " không đủ hàng (còn " + p.getStockQty() + ")");
            }
            subtotal = subtotal.add(p.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        }

        // Build PC (nếu đặt hàng từ cấu hình đã lưu) — chỉ cho user đã login
        PcBuild build = null;
        if (req.getBuildId() != null) {
            if (userId == null) {
                throw AppException.badRequest("BUILD_PC_REQUIRE_LOGIN", "Đặt hàng từ cấu hình PC yêu cầu đăng nhập");
            }
            build = pcBuildRepo.findById(req.getBuildId())
                .orElseThrow(() -> AppException.notFound("Cấu hình PC"));
            if (!build.getUserId().equals(userId)) {
                throw AppException.forbidden("Bạn không có quyền dùng cấu hình PC này");
            }
        }

        // Voucher (nhập tay) - với validation quyền sử dụng
        BigDecimal voucherDiscount = BigDecimal.ZERO;
        Voucher voucher = null;
        if (req.getVoucherCode() != null && !req.getVoucherCode().isBlank()) {
            // Sử dụng VoucherService để check quyền (hỗ trợ cả PUBLIC và PERSONAL voucher)
            try {
                voucherService.checkVoucher(req.getVoucherCode(), userId);
            } catch (Exception e) {
                throw AppException.badRequest("VOUCHER_CHECK_FAILED", e.getMessage());
            }
            
            voucher = voucherRepo.findValidByCode(req.getVoucherCode(), LocalDateTime.now())
                .orElseThrow(() -> AppException.badRequest("INVALID_VOUCHER", "Mã voucher không hợp lệ hoặc đã hết hạn"));

            if (subtotal.compareTo(voucher.getMinOrderValue()) < 0) {
                throw AppException.badRequest("VOUCHER_MIN_ORDER",
                    "Đơn hàng cần tối thiểu " + voucher.getMinOrderValue() + "đ để dùng voucher này");
            }

            if (voucher.getDiscountType() == Voucher.DiscountType.percent) {
                voucherDiscount = subtotal.multiply(voucher.getDiscountValue()).divide(BigDecimal.valueOf(100));
                if (voucher.getMaxDiscount() != null && voucherDiscount.compareTo(voucher.getMaxDiscount()) > 0) {
                    voucherDiscount = voucher.getMaxDiscount();
                }
            } else if (voucher.getDiscountType() == Voucher.DiscountType.fixed_amount) {
                voucherDiscount = voucher.getDiscountValue();
            }

            // Note: Không tăng usedCount ở đây nữa, sẽ xử lý sau khi order success
        }

        // Khuyến mãi tự động (Promotion) — snapshot tại thời điểm đặt hàng, không dùng lại
        // giá trị FE gửi lên để tránh khách sửa giá qua request hoặc promotion đã hết hạn.
        List<PromotionService.LineItem> lineItems = cart.getItems().stream()
            .map(i -> new PromotionService.LineItem(i.getProduct(), i.getQuantity(), i.getProduct().getPrice()))
            .toList();
        PromotionService.PromotionCalcResult promoResult =
            promotionService.calculateDiscount(lineItems, subtotal, build);

        // Voucher + Promotion cộng dồn, không vượt quá subtotal đơn hàng
        BigDecimal discountAmount = voucherDiscount
            .add(promoResult.getDiscountAmount())
            .add(promoResult.getCashBonus());
        if (discountAmount.compareTo(subtotal) > 0) discountAmount = subtotal;

        // Shipping fee
        ShippingMethod shippingMethod = null;
        BigDecimal shippingFee;
        if (req.getShippingMethodId() != null) {
            shippingMethod = shippingMethodRepo.findById(req.getShippingMethodId())
                .orElseThrow(() -> AppException.notFound("Phương thức giao hàng"));
            shippingFee = shippingMethod.getBaseFee();
            if (shippingMethod.getFreeThreshold() != null
                    && subtotal.compareTo(shippingMethod.getFreeThreshold()) >= 0) {
                shippingFee = BigDecimal.ZERO;
            }
        } else {
            shippingFee = DEFAULT_SHIPPING_FEE;
        }
        if (voucher != null && voucher.getDiscountType() == Voucher.DiscountType.free_shipping) {
            shippingFee = BigDecimal.ZERO;
        }
        if (req.getPickupStoreId() != null) {
            Store pickupStore = storeRepo.findById(req.getPickupStoreId())
                .orElseThrow(() -> AppException.notFound("Cửa hàng"));
            if (!Boolean.TRUE.equals(pickupStore.getIsActive())) {
                throw AppException.badRequest("STORE_INACTIVE", "Cửa hàng này hiện không nhận đơn tại quầy");
            }
            shippingFee = BigDecimal.ZERO; // Nhận tại showroom — không phát sinh phí ship
        }

        BigDecimal totalAmount = subtotal.subtract(discountAmount).add(shippingFee);
        if (totalAmount.compareTo(BigDecimal.ZERO) < 0) totalAmount = BigDecimal.ZERO;

        // Xử lý cọc tiền cho đơn hàng COD
        BigDecimal depositAmount = BigDecimal.ZERO;
        BigDecimal remainingAmount = totalAmount;
        boolean depositPaid = false;
        Order.OrderStatus initialStatus = Order.OrderStatus.pending;
        
        if (Order.PaymentMethod.valueOf(req.getPaymentMethod()) == Order.PaymentMethod.cod) {
            // COD yêu cầu cọc theo config
            depositAmount = orderConfig.getCodDepositAmountAsBigDecimal();
            remainingAmount = totalAmount.subtract(depositAmount);
            // depositPaid sẽ được cập nhật sau khi thanh toán cọc thành công
            depositPaid = false;
            // Order COD bắt đầu với status pending_deposit (chờ thanh toán cọc)
            initialStatus = Order.OrderStatus.pending_deposit;
        }

        // Create order
        Order order = Order.builder()
            .orderCode(generateOrderCode())
            .user(user)
            .sessionId(sessionId)
            .guestEmail(user == null && req.getGuestEmail() != null && !req.getGuestEmail().isBlank()
                ? req.getGuestEmail() : null)
            .status(initialStatus)
            .paymentMethod(Order.PaymentMethod.valueOf(req.getPaymentMethod()))
            .paymentStatus(Order.PaymentStatus.pending)
            .shippingName(req.getShippingName())
            .shippingPhone(req.getShippingPhone())
            .shippingProvince(req.getShippingProvince())
            .shippingDistrict(req.getShippingDistrict())
            .shippingWard(req.getShippingWard())
            .shippingAddress(req.getShippingAddress())
            .subtotal(subtotal)
            .shippingFee(shippingFee)
            .discountAmount(discountAmount)
            .totalAmount(totalAmount)
            .depositAmount(depositAmount)
            .depositPaid(depositPaid)
            .remainingAmount(remainingAmount)
            .voucher(voucher)
            .voucherCode(req.getVoucherCode())
            .pickupStoreId(req.getPickupStoreId())
            .note(req.getNote())
            .build(build)
            .build();


        // Create order items + deduct stock
        for (CartItem cartItem : cart.getItems()) {
            Product p = cartItem.getProduct();
            OrderItem oi = OrderItem.builder()
                .order(order).product(p)
                .productName(p.getName()).productSku(p.getSku())
                .productImage(p.getThumbnail())
                .warrantyMonths(p.getWarrantyMonths())
                .quantity(cartItem.getQuantity())
                .unitPrice(p.getPrice())
                .discountPrice(BigDecimal.ZERO)
                .totalPrice(p.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())))
                .build();
            order.getItems().add(oi);

            // Deduct stock
            p.setStockQty(p.getStockQty() - cartItem.getQuantity());
            p.setSoldQty(p.getSoldQty() + cartItem.getQuantity());
            productRepo.save(p);
        }

        orderRepo.save(order);

        // Ghi nhận phương thức/phí giao hàng cho đơn
        orderShippingRepo.save(OrderShipping.builder()
            .order(order)
            .method(shippingMethod)
            .pickupStoreId(req.getPickupStoreId())
            .shippingFee(shippingFee)
            .status("pending")
            .build());

        // Nhận tại showroom: trừ tồn kho riêng của showroom đó (lớp bổ sung, không đụng
        // products.stock_qty tổng — showroom có thể chưa được seed tồn kho riêng nên bỏ qua
        // nếu không có bản ghi, tránh chặn luồng đặt hàng vì thiếu dữ liệu bổ trợ).
        if (req.getPickupStoreId() != null) {
            for (OrderItem item : order.getItems()) {
                ProductStockByStoreId key = new ProductStockByStoreId(item.getProduct().getId(), req.getPickupStoreId());
                stockByStoreRepo.findById(key).ifPresent(stock -> {
                    stock.setStockQty(Math.max(0, stock.getStockQty() - item.getQuantity()));
                    stockByStoreRepo.save(stock);
                });
            }
        }

        // Clear cart
        cart.getItems().clear();
        cartRepo.save(cart);

        // Mark voucher as used (nếu có)
        if (voucher != null && userId != null) {
            voucherService.markVoucherAsUsed(userId, voucher.getCode(), order.getId());
        }

        // Send email confirmation
        String recipientEmail = null;
        String recipientName = null;
        String recipientPhone = null;
        
        if (user != null && user.getEmail() != null) {
            // Khách hàng đã đăng nhập
            recipientEmail = user.getEmail();
            recipientName = user.getFullName();
        } else if (req.getGuestEmail() != null && !req.getGuestEmail().isBlank()) {
            // Khách hàng guest có cung cấp email
            recipientEmail = req.getGuestEmail();
            recipientName = req.getShippingName();
            recipientPhone = req.getShippingPhone();
        }
        
        // Với VNPay/ZaloPay/MoMo: email gửi sau khi callback xác nhận thanh toán thành công
        boolean isOnlinePayment = order.getPaymentMethod() == Order.PaymentMethod.vnpay
            || order.getPaymentMethod() == Order.PaymentMethod.zalopay
            || order.getPaymentMethod() == Order.PaymentMethod.momo;

        if (recipientEmail != null && !isOnlinePayment) {
            // Format số tiền với dấu phân cách hàng nghìn
            String formattedTotal = formatCurrency(totalAmount);
            String formattedSubtotal = formatCurrency(subtotal);
            String formattedShippingFee = formatCurrency(shippingFee);
            String formattedDiscount = formatCurrency(discountAmount);
            
            // Prepare order items for email
            java.util.List<EmailService.OrderItemDto> emailItems = order.getItems().stream()
                .map(item -> new EmailService.OrderItemDto(
                    item.getProductName(),
                    item.getQuantity(),
                    formatCurrency(item.getTotalPrice())
                ))
                .collect(java.util.stream.Collectors.toList());
            
            // Build full shipping address theo chuẩn VN (bỏ District vì form chỉ dùng Province + Ward)
            String fullAddress = String.format("%s - %s\n%s, %s, %s",
                order.getShippingName(),
                order.getShippingPhone(),
                order.getShippingAddress(),
                order.getShippingWard(),
                order.getShippingProvince()
            );
            
            // Map payment method to Vietnamese
            String paymentMethodLabel = getPaymentMethodLabel(order.getPaymentMethod());
            
            boolean isCodDeposit = initialStatus == Order.OrderStatus.pending_deposit;
            String depositAmountFormatted = isCodDeposit && order.getDepositAmount() != null
                ? formatCurrency(order.getDepositAmount()) : null;
            String remainingAmountFormatted = isCodDeposit && order.getRemainingAmount() != null
                ? formatCurrency(order.getRemainingAmount()) : null;

            emailService.sendOrderConfirmation(
                recipientEmail,
                recipientName,
                order.getOrderCode(),
                formattedTotal,
                recipientPhone,
                order.getShippingPhone(),
                emailItems,
                formattedSubtotal,
                formattedShippingFee,
                formattedDiscount,
                fullAddress,
                paymentMethodLabel,
                isCodDeposit,
                depositAmountFormatted,
                remainingAmountFormatted
            );
        }

        // Notification (chỉ cho user đã login)
        if (userId != null) {
            notificationService.createNotification(userId, "order_update",
                "Đặt hàng thành công", "Đơn hàng " + order.getOrderCode() + " đã được tạo",
                "order", order.getId());
        }

        // Ghi lịch sử khởi tạo đơn
        String historyMessage = "Đơn hàng được tạo" + (sessionId != null && userId == null ? " (guest checkout)" : "");
        if (initialStatus == Order.OrderStatus.pending_deposit) {
            historyMessage += " - Chờ thanh toán cọc";
        }
        recordHistory(order, null, initialStatus, user, "customer", historyMessage);

        log.info("Order created: {} with status {} for user {} / session {}", 
            order.getOrderCode(), initialStatus, userId, sessionId);
        return toResponse(order);
    }

    public OrderResponse trackOrder(String orderCode, String phone) {
        Order order = orderRepo.findByOrderCodeAndPhone(orderCode, phone)
            .orElseThrow(() -> AppException.notFound("Đơn hàng"));
        return toResponse(order);
    }

    public Page<OrderResponse> getUserOrders(Long userId, String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Order> orders;
        if (userId == null) {
            orders = status != null
                ? orderRepo.findAllByStatusOrderByCreatedAtDesc(Order.OrderStatus.valueOf(status), pageable)
                : orderRepo.findAllByOrderByCreatedAtDesc(pageable);
        } else {
            orders = status != null
                ? orderRepo.findByUserIdAndStatus(userId, Order.OrderStatus.valueOf(status), pageable)
                : orderRepo.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        }
        return orders.map(this::toResponse);
    }

    public Page<OrderResponse> getAdminOrders(String status, String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Order.OrderStatus orderStatus = (status != null && !status.isBlank())
            ? Order.OrderStatus.valueOf(status) : null;
        String kw = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;

        // Phân nhánh hoàn toàn — không bao giờ truyền null vào query có LIKE
        // để tránh lỗi "could not determine data type" trên PostgreSQL
        if (kw == null && orderStatus == null) {
            return orderRepo.findAllByOrderByCreatedAtDesc(pageable).map(this::toResponse);
        } else if (kw == null) {
            return orderRepo.findAllByStatusOrderByCreatedAtDesc(orderStatus, pageable).map(this::toResponse);
        } else if (orderStatus == null) {
            return orderRepo.searchAdminOrdersByKeyword(kw, pageable).map(this::toResponse);
        } else {
            return orderRepo.searchAdminOrdersByStatusAndKeyword(orderStatus, kw, pageable).map(this::toResponse);
        }
    }

    @Transactional(readOnly = true)
    public OrderResponse getAdminOrderDetail(Long orderId) {
        Order order = orderRepo.findById(orderId)
            .orElseThrow(() -> AppException.notFound("Đơn hàng"));
        return toResponseWithHistory(order);
    }

    public OrderResponse getOrderDetail(Long orderId, Long userId) {
        Order order = orderRepo.findById(orderId)
            .orElseThrow(() -> AppException.notFound("Đơn hàng"));
        // Guest order (user == null) hoặc userId không khớp → từ chối
        if (userId != null && (order.getUser() == null || !order.getUser().getId().equals(userId))) {
            throw AppException.forbidden("Bạn không có quyền xem đơn hàng này");
        }
        return toResponseWithHistory(order);
    }

    public OrderResponse cancelOrder(Long orderId, Long userId, String reason) {
        Order order = orderRepo.findById(orderId)
            .orElseThrow(() -> AppException.notFound("Đơn hàng"));

        // Kiểm tra quyền (khách hàng chỉ hủy đơn của mình; guest không hủy được qua API này)
        if (order.getUser() == null || !order.getUser().getId().equals(userId)) {
            throw AppException.forbidden("Bạn không có quyền hủy đơn hàng này");
        }
        if (order.getStatus() != Order.OrderStatus.pending
                && order.getStatus() != Order.OrderStatus.pending_deposit) {
            throw AppException.badRequest("CANNOT_CANCEL",
                "Chỉ có thể hủy đơn hàng ở trạng thái chờ xác nhận hoặc chờ đặt cọc");
        }

        Order.OrderStatus fromStatus = order.getStatus();
        order.setStatus(Order.OrderStatus.cancelled);
        order.setCancelledAt(LocalDateTime.now());
        order.setCancelledReason(reason);

        // Hoàn lại tồn kho
        for (OrderItem item : order.getItems()) {
            Product p = item.getProduct();
            p.setStockQty(p.getStockQty() + item.getQuantity());
            p.setSoldQty(Math.max(0, p.getSoldQty() - item.getQuantity()));
            productRepo.save(p);
        }

        orderRepo.save(order);

        // Ghi lịch sử
        User customer = order.getUser();
        recordHistory(order, fromStatus, Order.OrderStatus.cancelled, customer, "customer",
            reason != null ? "Khách hủy: " + reason : "Khách hủy đơn");

        String cancelEmail = (customer != null && customer.getEmail() != null)
            ? customer.getEmail() : order.getGuestEmail();
        String cancelName  = (customer != null) ? customer.getFullName() : order.getShippingName();
        if (cancelEmail != null) {
            emailService.sendOrderStatusUpdate(cancelEmail, cancelName,
                order.getOrderCode(), "cancelled", reason);
        }

        return toResponse(order);
    }

    public OrderResponse updateOrderStatus(Long orderId, String status, String staffNote, Long performedByUserId) {
        Order order = orderRepo.findById(orderId)
            .orElseThrow(() -> AppException.notFound("Đơn hàng"));

        Order.OrderStatus currentStatus = order.getStatus();
        Order.OrderStatus newStatus;
        try {
            newStatus = Order.OrderStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw AppException.badRequest("INVALID_STATUS", "Trạng thái không hợp lệ: " + status);
        }

        // Validate chuyển trạng thái hợp lệ
        Set<Order.OrderStatus> allowed = VALID_TRANSITIONS.getOrDefault(currentStatus, Set.of());
        if (!allowed.contains(newStatus)) {
            throw AppException.badRequest("INVALID_TRANSITION",
                "Không thể chuyển từ [" + currentStatus.name() + "] sang [" + newStatus.name() + "]. " +
                "Các trạng thái cho phép: " + allowed.stream().map(Enum::name).sorted().toList());
        }

        order.setStatus(newStatus);
        if (staffNote != null && !staffNote.isBlank()) order.setStaffNote(staffNote);

        switch (newStatus) {
            case confirmed   -> order.setConfirmedAt(LocalDateTime.now());
            case processing  -> order.setProcessingAt(LocalDateTime.now());
            case shipping    -> order.setShippedAt(LocalDateTime.now());
            case delivered   -> order.setDeliveredAt(LocalDateTime.now());
            case completed   -> order.setCompletedAt(LocalDateTime.now());
            case cancelled   -> order.setCancelledAt(LocalDateTime.now());
            default -> {}
        }

        orderRepo.save(order);

        // Tự động tạo warranty cho từng sản phẩm khi đơn được giao
        if (newStatus == Order.OrderStatus.delivered && order.getUser() != null) {
            createWarrantiesForOrder(order);
        }

        // Tìm nhân viên thực hiện để ghi history
        User performer = (performedByUserId != null)
            ? userRepo.findById(performedByUserId).orElse(null) : null;
        recordHistory(order, currentStatus, newStatus, performer, "staff", staffNote);

        String recipientEmail = (order.getUser() != null && order.getUser().getEmail() != null)
            ? order.getUser().getEmail() : order.getGuestEmail();
        String recipientName  = (order.getUser() != null)
            ? order.getUser().getFullName() : order.getShippingName();
        if (recipientEmail != null) {
            emailService.sendOrderStatusUpdate(recipientEmail, recipientName,
                order.getOrderCode(), status, staffNote);
        }

        // Trigger chính sách voucher khi đơn hàng hoàn thành
        if (newStatus == Order.OrderStatus.completed && order.getUser() != null) {
            voucherPolicyService.onOrderCompleted(order.getUser().getId());
        }

        return toResponse(order);
    }

    private void createWarrantiesForOrder(Order order) {
        LocalDate today = LocalDate.now();
        for (OrderItem item : order.getItems()) {
            if (item.getWarrantyMonths() == null || item.getWarrantyMonths() <= 0) continue;
            if (warrantyRepo.existsByOrderItemId(item.getId())) continue;
            warrantyRepo.save(Warranty.builder()
                .orderItemId(item.getId())
                .product(item.getProduct())
                .user(order.getUser())
                .purchaseDate(today)
                .warrantyExpiresAt(today.plusMonths(item.getWarrantyMonths()))
                .warrantyMonths(item.getWarrantyMonths())
                .status("active")
                .build());
        }
    }

    private void recordHistory(Order order, Order.OrderStatus from, Order.OrderStatus to,
                               User performer, String actorType, String note) {
        OrderHistory h = OrderHistory.builder()
            .order(order)
            .fromStatus(from != null ? from.name() : null)
            .toStatus(to.name())
            .performedBy(performer)
            .performedByName(performer != null ? performer.getFullName() : null)
            .performedByRole(performer != null ? performer.getRole().name() : null)
            .actorType(actorType != null ? actorType : "system")
            .note(note)
            .build();
        orderHistoryRepo.save(h);
    }

    /**
     * Public method to record order status history from external services (payment callbacks)
     */
    public void recordOrderHistory(Order order, Order.OrderStatus from, Order.OrderStatus to, String note) {
        recordHistory(order, from, to, null, "system", note);
    }

    public OrderResponse toResponseWithHistory(Order o) {
        OrderResponse resp = toResponse(o);
        List<OrderHistory> history = orderHistoryRepo.findByOrderIdOrderByCreatedAtAsc(o.getId());
        resp.setHistory(history.stream().map(h -> {
            String username = null;
            if (h.getPerformedBy() != null && h.getPerformedBy().getEmail() != null) {
                String email = h.getPerformedBy().getEmail();
                int atIdx = email.indexOf('@');
                username = atIdx != -1 ? email.substring(0, atIdx) : email;
            }
            return OrderResponse.OrderHistoryEntry.builder()
                .id(h.getId())
                .fromStatus(h.getFromStatus())
                .toStatus(h.getToStatus())
                .performedById(h.getPerformedBy() != null ? h.getPerformedBy().getId() : null)
                .performedByName(h.getPerformedByName())
                .performedByUsername(username)
                .performedByRole(h.getPerformedByRole())
                .actorType(h.getActorType())
                .note(h.getNote())
                .createdAt(h.getCreatedAt())
                .build();
        }).toList());
        return resp;
    }

    private String generateOrderCode() {
        String year = String.valueOf(LocalDateTime.now().getYear());
        
        // Query last order code in current year
        String prefix = "HC-" + year + "-";
        List<Order> lastOrders = orderRepo.findTop1ByOrderCodeStartingWithOrderByOrderCodeDesc(prefix);
        
        int nextNumber = 1;
        if (!lastOrders.isEmpty()) {
            String lastCode = lastOrders.get(0).getOrderCode();
            // Extract number from "HC-2026-000001"
            String numberPart = lastCode.substring(lastCode.lastIndexOf('-') + 1);
            nextNumber = Integer.parseInt(numberPart) + 1;
        }
        
        return prefix + String.format("%06d", nextNumber);
    }

    public OrderResponse toResponse(Order o) {
        User user = o.getUser();
        return OrderResponse.builder()
            .id(o.getId()).orderCode(o.getOrderCode())
            .status(o.getStatus().name()).paymentMethod(o.getPaymentMethod().name())
            .paymentStatus(o.getPaymentStatus().name())
            .userId(user != null ? user.getId() : null)
            .customerName(user != null ? user.getFullName() : o.getShippingName())
            .customerEmail(user != null ? user.getEmail() : null)
            .customerPhone(user != null ? user.getPhone() : o.getShippingPhone())
            .shippingName(o.getShippingName()).shippingPhone(o.getShippingPhone())
            .shippingProvince(o.getShippingProvince()).shippingDistrict(o.getShippingDistrict())
            .shippingWard(o.getShippingWard()).shippingAddress(o.getShippingAddress())
            .subtotal(o.getSubtotal()).shippingFee(o.getShippingFee())
            .discountAmount(o.getDiscountAmount()).totalAmount(o.getTotalAmount())
            .depositAmount(o.getDepositAmount()).depositPaid(o.getDepositPaid())
            .remainingAmount(o.getRemainingAmount())
            .refundAmount(o.getRefundAmount())
            .voucherCode(o.getVoucherCode()).note(o.getNote())
            .staffNote(o.getStaffNote())
            .cancelledReason(o.getCancelledReason())
            .buildId(o.getBuild() != null ? o.getBuild().getId() : null)
            .buildName(o.getBuild() != null ? o.getBuild().getName() : null)
            .createdAt(o.getCreatedAt()).confirmedAt(o.getConfirmedAt())
            .shippedAt(o.getShippedAt()).deliveredAt(o.getDeliveredAt())
            .completedAt(o.getCompletedAt()).cancelledAt(o.getCancelledAt())
            .items(o.getItems().stream().map(i -> OrderResponse.OrderItemResponse.builder()
                .id(i.getId()).productId(i.getProduct().getId())
                .productName(i.getProductName()).productSku(i.getProductSku())
                .productImage(i.getProductImage()).quantity(i.getQuantity())
                .unitPrice(i.getUnitPrice()).totalPrice(i.getTotalPrice())
                .warrantyMonths(i.getWarrantyMonths()).build()).toList())
            .build();
    }

    /**
     * Format tiền theo chuẩn VND: 1.500.000đ
     * Đồng nhất với frontend (numeral format '0,0' + 'đ' nhưng dùng dấu chấm thay vì dấu phẩy)
     */
    private String formatCurrency(BigDecimal amount) {
        if (amount == null) return "0đ";
        
        // Convert to long để format (VND không có phần thập phân)
        long value = amount.longValue();
        
        // Format với dấu chấm làm phân cách hàng nghìn
        String formatted = String.format("%,d", value).replace(',', '.');
        
        return formatted + "đ";
    }

    /**
     * Map payment method enum to Vietnamese label
     */
    private String getPaymentMethodLabel(Order.PaymentMethod method) {
        return switch (method) {
            case cod -> "Thanh toán khi nhận hàng (COD)";
            case bank_transfer -> "Chuyển khoản ngân hàng";
            case vnpay -> "Thanh toán qua VNPay";
            case momo -> "Thanh toán qua Ví MoMo";
            case zalopay -> "Thanh toán qua ZaloPay";
            case installment -> "Trả góp 0%";
        };
    }
}
