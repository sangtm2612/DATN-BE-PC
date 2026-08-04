package com.kinhduanpc.service;

import com.kinhduanpc.dto.order.ReturnRequestDecisionRequest;
import com.kinhduanpc.dto.order.ReturnRequestRequest;
import com.kinhduanpc.dto.order.ReturnRequestResponse;
import com.kinhduanpc.dto.order.ReturnRequestReviewRequest;
import com.kinhduanpc.entity.Order;
import com.kinhduanpc.entity.OrderItem;
import com.kinhduanpc.entity.ReturnMedia;
import com.kinhduanpc.entity.ReturnRequest;
import com.kinhduanpc.entity.ReturnRequestItem;
import com.kinhduanpc.entity.User;
import com.kinhduanpc.exception.AppException;
import com.kinhduanpc.repository.OrderRepository;
import com.kinhduanpc.repository.ReturnMediaRepository;
import com.kinhduanpc.repository.ReturnRequestItemRepository;
import com.kinhduanpc.repository.ReturnRequestRepository;
import com.kinhduanpc.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Transactional
public class ReturnRequestService {

    private final ReturnRequestRepository returnRequestRepo;
    private final ReturnRequestItemRepository returnRequestItemRepo;
    private final ReturnMediaRepository returnMediaRepo;
    private final OrderRepository orderRepo;
    private final UserRepository userRepo;

    private static final Set<String> ALLOWED_REASONS = Set.of(
        "defective", "wrong_item", "damaged_delivery", "not_satisfied");
    private static final Set<String> ALLOWED_RESOLUTIONS = Set.of("exchange", "refund");
    private static final int RETURN_WINDOW_DAYS = 15;

    private static final AtomicInteger sequence = new AtomicInteger(1);

    public ReturnRequestResponse createReturnRequest(Long orderId, Long userId, ReturnRequestRequest req) {
        Order order = orderRepo.findById(orderId)
            .orElseThrow(() -> AppException.notFound("Đơn hàng"));
        if (!order.getUser().getId().equals(userId)) {
            throw AppException.forbidden("Bạn không có quyền tạo yêu cầu đổi/trả cho đơn hàng này");
        }
        if (order.getStatus() != Order.OrderStatus.delivered && order.getStatus() != Order.OrderStatus.completed) {
            throw AppException.badRequest("ORDER_NOT_DELIVERED", "Chỉ có thể đổi/trả đơn hàng đã giao");
        }
        if (order.getDeliveredAt() == null
                || LocalDateTime.now().isAfter(order.getDeliveredAt().plusDays(RETURN_WINDOW_DAYS))) {
            throw AppException.badRequest("RETURN_WINDOW_EXPIRED",
                "Đã quá thời hạn " + RETURN_WINDOW_DAYS + " ngày đổi/trả kể từ khi nhận hàng");
        }
        if (!ALLOWED_REASONS.contains(req.getReasonType())) {
            throw AppException.badRequest("INVALID_REASON", "Lý do đổi/trả không hợp lệ");
        }
        if (req.getMediaUrls() != null && req.getMediaUrls().size() > 5) {
            throw AppException.badRequest("TOO_MANY_MEDIA", "Tối đa 5 file ảnh/video");
        }

        User user = userRepo.findById(userId).orElseThrow(() -> AppException.notFound("Người dùng"));

        // So luong da "giu cho" boi cac yeu cau khac (khac rejected) tren cung don hang,
        // + tich luy dan trong vong lap ben duoi de chan luon trung orderItemId ngay trong 1 request.
        Map<Long, Integer> claimedQty = new HashMap<>();
        for (ReturnRequest existing : returnRequestRepo.findByOrderId(orderId)) {
            if (existing.getStatus() == ReturnRequest.ReturnStatus.rejected) continue;
            for (ReturnRequestItem it : existing.getItems()) {
                claimedQty.merge(it.getOrderItem().getId(), it.getQuantity(), Integer::sum);
            }
        }

        ReturnRequest rr = ReturnRequest.builder()
            .order(order).user(user)
            .returnCode(generateReturnCode())
            .reasonType(req.getReasonType())
            .reasonDetail(req.getReasonDetail())
            .status(ReturnRequest.ReturnStatus.pending)
            .build();
        rr = returnRequestRepo.save(rr);

        List<ReturnRequestItem> items = new ArrayList<>();
        for (ReturnRequestRequest.Item itemReq : req.getItems()) {
            OrderItem orderItem = order.getItems().stream()
                .filter(oi -> oi.getId().equals(itemReq.getOrderItemId()))
                .findFirst()
                .orElseThrow(() -> AppException.badRequest("INVALID_ORDER_ITEM",
                    "Sản phẩm không thuộc đơn hàng này"));

            int alreadyClaimed = claimedQty.getOrDefault(orderItem.getId(), 0);
            int newTotal = alreadyClaimed + itemReq.getQuantity();
            if (newTotal > orderItem.getQuantity()) {
                throw AppException.badRequest("INVALID_QUANTITY",
                    "Số lượng đổi/trả vượt quá số lượng có thể đổi/trả (đã có yêu cầu trước đó hoặc trùng lặp trong cùng yêu cầu)");
            }
            claimedQty.put(orderItem.getId(), newTotal);

            items.add(ReturnRequestItem.builder()
                .returnRequest(rr).orderItem(orderItem).quantity(itemReq.getQuantity()).build());
        }
        rr.setItems(items);

        if (req.getMediaUrls() != null && !req.getMediaUrls().isEmpty()) {
            List<ReturnMedia> mediaList = new ArrayList<>();
            int i = 0;
            for (String url : req.getMediaUrls()) {
                mediaList.add(ReturnMedia.builder()
                    .returnRequest(rr).mediaUrl(url).mediaType("image").sortOrder(i++).build());
            }
            rr.setMedia(mediaList);
        }

        return toResponse(returnRequestRepo.save(rr));
    }

    public List<ReturnRequestResponse> getMyReturnRequests(Long userId) {
        List<ReturnRequest> requests = returnRequestRepo.findByUserId(userId).stream()
            .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
            .toList();
        return toResponseList(requests);
    }

    public List<ReturnRequestResponse> getAdminReturnRequests(String status) {
        List<ReturnRequest> requests;
        if (status != null) {
            try {
                requests = returnRequestRepo.findByStatusOrderByCreatedAtDesc(
                    ReturnRequest.ReturnStatus.valueOf(status));
            } catch (IllegalArgumentException e) {
                throw AppException.badRequest("INVALID_STATUS", "Trạng thái không hợp lệ");
            }
        } else {
            requests = returnRequestRepo.findAllByOrderByCreatedAtDesc();
        }
        return toResponseList(requests);
    }

    public ReturnRequestResponse review(Long id, ReturnRequestReviewRequest req) {
        ReturnRequest rr = returnRequestRepo.findById(id)
            .orElseThrow(() -> AppException.notFound("Yêu cầu đổi/trả"));
        if (rr.getStatus() != ReturnRequest.ReturnStatus.pending) {
            throw AppException.badRequest("INVALID_STATUS_TRANSITION",
                "Chỉ có thể xem xét yêu cầu đang ở trạng thái chờ xử lý");
        }
        rr.setStatus(ReturnRequest.ReturnStatus.reviewing);
        if (req.getStaffNote() != null) rr.setStaffNote(req.getStaffNote());
        return toResponse(returnRequestRepo.save(rr));
    }

    public ReturnRequestResponse decide(Long id, Long staffUserId, ReturnRequestDecisionRequest req) {
        ReturnRequest rr = returnRequestRepo.findById(id)
            .orElseThrow(() -> AppException.notFound("Yêu cầu đổi/trả"));
        if (rr.getStatus() != ReturnRequest.ReturnStatus.reviewing) {
            throw AppException.badRequest("INVALID_STATUS_TRANSITION",
                "Chỉ có thể quyết định yêu cầu đang ở trạng thái đang xem xét");
        }

        boolean approved = "approved".equals(req.getDecision());
        if (!approved && !"rejected".equals(req.getDecision())) {
            throw AppException.badRequest("INVALID_DECISION", "Quyết định không hợp lệ");
        }

        if (approved) {
            if (!ALLOWED_RESOLUTIONS.contains(req.getResolution())) {
                throw AppException.badRequest("INVALID_RESOLUTION", "Vui lòng chọn hướng xử lý (đổi hàng/hoàn tiền)");
            }
            if ("refund".equals(req.getResolution())) {
                if (req.getRefundAmount() == null || req.getRefundAmount().compareTo(BigDecimal.ZERO) <= 0) {
                    throw AppException.badRequest("MISSING_REFUND_AMOUNT", "Vui lòng nhập số tiền hoàn");
                }
                // Tran hoan tien = gia tri thuc te cua cac san pham trong YEU CAU NAY (khong phai toan bo don hang),
                // tranh nhap nham gay hoan qua so tien san pham duoc tra.
                BigDecimal maxRefund = rr.getItems().stream()
                    .map(i -> i.getOrderItem().getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                if (req.getRefundAmount().compareTo(maxRefund) > 0) {
                    throw AppException.badRequest("REFUND_EXCEEDS_ITEMS_VALUE",
                        "Số tiền hoàn không được vượt quá giá trị sản phẩm trong yêu cầu đổi/trả này (" + maxRefund + ")");
                }
                rr.setRefundAmount(req.getRefundAmount());
            }
            rr.setResolution(req.getResolution());
            rr.setStatus(ReturnRequest.ReturnStatus.approved);
        } else {
            rr.setStatus(ReturnRequest.ReturnStatus.rejected);
        }

        if (req.getStaffNote() != null) rr.setStaffNote(req.getStaffNote());
        rr.setReviewedBy(userRepo.findById(staffUserId).orElse(null));
        rr.setReviewedAt(LocalDateTime.now());

        return toResponse(returnRequestRepo.save(rr));
    }

    public ReturnRequestResponse complete(Long id) {
        ReturnRequest rr = returnRequestRepo.findById(id)
            .orElseThrow(() -> AppException.notFound("Yêu cầu đổi/trả"));
        if (rr.getStatus() != ReturnRequest.ReturnStatus.approved || rr.getCompletedAt() != null) {
            throw AppException.badRequest("INVALID_STATUS_TRANSITION",
                "Chỉ có thể hoàn tất yêu cầu đã được duyệt và chưa hoàn tất trước đó");
        }

        // Doc truoc cac gia tri can dung, vi buoc update nguyen tu ben duoi se clear persistence
        // context (tranh Hibernate ghi de lai gia tri cu khi flush entity `rr` dang duoc quan ly).
        Long orderId = rr.getOrder().getId();
        String resolution = rr.getResolution();
        BigDecimal refundAmount = rr.getRefundAmount();
        LocalDateTime now = LocalDateTime.now();

        int updated = returnRequestRepo.markCompletedIfApproved(
            id, now, ReturnRequest.ReturnStatus.approved, ReturnRequest.ReturnStatus.completed);
        if (updated == 0) {
            throw AppException.conflict("ALREADY_COMPLETED",
                "Yêu cầu đã được hoàn tất hoặc không còn ở trạng thái đã duyệt");
        }

        if ("refund".equals(resolution) && refundAmount != null) {
            orderRepo.addRefund(orderId, refundAmount, now);
        }

        ReturnRequest refreshed = returnRequestRepo.findById(id)
            .orElseThrow(() -> AppException.notFound("Yêu cầu đổi/trả"));
        return toResponse(refreshed);
    }

    private ReturnRequestResponse toResponse(ReturnRequest rr) {
        List<String> mediaUrls = returnMediaRepo.findByReturnRequestIdOrderBySortOrderAsc(rr.getId())
            .stream().map(ReturnMedia::getMediaUrl).toList();
        List<ReturnRequestResponse.ItemResponse> items = toItemResponses(
            returnRequestItemRepo.findByReturnRequestId(rr.getId()));
        return buildResponse(rr, mediaUrls, items);
    }

    /** Batch-fetch media/items 1 lan cho ca danh sach thay vi truy van tren tung dong (tranh N+1). */
    private List<ReturnRequestResponse> toResponseList(List<ReturnRequest> requests) {
        if (requests.isEmpty()) return List.of();

        List<Long> ids = requests.stream().map(ReturnRequest::getId).toList();
        Map<Long, List<String>> mediaByRequestId = returnMediaRepo
            .findByReturnRequestIdInOrderBySortOrderAsc(ids).stream()
            .collect(java.util.stream.Collectors.groupingBy(
                m -> m.getReturnRequest().getId(),
                java.util.stream.Collectors.mapping(ReturnMedia::getMediaUrl, java.util.stream.Collectors.toList())));
        Map<Long, List<ReturnRequestItem>> itemsByRequestId = returnRequestItemRepo
            .findByReturnRequestIdIn(ids).stream()
            .collect(java.util.stream.Collectors.groupingBy(i -> i.getReturnRequest().getId()));

        return requests.stream()
            .map(rr -> buildResponse(rr,
                mediaByRequestId.getOrDefault(rr.getId(), List.of()),
                toItemResponses(itemsByRequestId.getOrDefault(rr.getId(), List.of()))))
            .toList();
    }

    private List<ReturnRequestResponse.ItemResponse> toItemResponses(List<ReturnRequestItem> items) {
        return items.stream()
            .map(i -> ReturnRequestResponse.ItemResponse.builder()
                .orderItemId(i.getOrderItem().getId())
                .productName(i.getOrderItem().getProductName())
                .quantity(i.getQuantity())
                .build())
            .toList();
    }

    private ReturnRequestResponse buildResponse(
            ReturnRequest rr, List<String> mediaUrls, List<ReturnRequestResponse.ItemResponse> items) {
        return ReturnRequestResponse.builder()
            .id(rr.getId())
            .returnCode(rr.getReturnCode())
            .orderId(rr.getOrder().getId())
            .orderCode(rr.getOrder().getOrderCode())
            .status(rr.getStatus().name())
            .reasonType(rr.getReasonType())
            .reasonDetail(rr.getReasonDetail())
            .resolution(rr.getResolution())
            .refundAmount(rr.getRefundAmount())
            .staffNote(rr.getStaffNote())
            .reviewedByName(rr.getReviewedBy() != null ? rr.getReviewedBy().getFullName() : null)
            .reviewedAt(rr.getReviewedAt())
            .completedAt(rr.getCompletedAt())
            .createdAt(rr.getCreatedAt())
            .mediaUrls(mediaUrls)
            .items(items)
            .userId(rr.getUser() != null ? rr.getUser().getId() : null)
            .userName(rr.getUser() != null ? rr.getUser().getFullName() : null)
            .userPhone(rr.getUser() != null ? rr.getUser().getPhone() : null)
            .build();
    }

    private String generateReturnCode() {
        String year = String.valueOf(LocalDateTime.now().getYear());
        return "RT-" + year + "-" + String.format("%06d", sequence.getAndIncrement());
    }
}
