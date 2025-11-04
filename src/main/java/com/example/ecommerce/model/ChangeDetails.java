package com.example.ecommerce.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ChangeDetails {
    private String changeType;
    private BigDecimal oldPrice;
    private BigDecimal newPrice;
    private Integer oldStock;
    private Integer newStock;
    private String reason;
}
