package com.kinhduanpc.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

/**
 * Configuration for order-related settings
 */
@Configuration
@ConfigurationProperties(prefix = "app.order")
@Data
public class OrderConfig {
    
    /**
     * Số giờ tự động hủy đơn hàng COD chưa thanh toán
     */
    private Integer autoCancelHours = 48;
    
    /**
     * Số ngày tự động hoàn tất đơn hàng sau khi giao
     */
    private Integer autoCompleteDays = 7;
    
    /**
     * Số ngày hạn đổi trả hàng
     */
    private Integer returnDeadlineDays = 15;
    
    /**
     * Số tiền cọc cho đơn hàng COD (VND)
     */
    private Long codDepositAmount = 100000L;
    
    /**
     * Get deposit amount as BigDecimal
     */
    public BigDecimal getCodDepositAmountAsBigDecimal() {
        return BigDecimal.valueOf(codDepositAmount);
    }
}
