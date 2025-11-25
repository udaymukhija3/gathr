package com.gathr.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HubDto {
    private Long id;
    private String name;
    private String area;
    private String description;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private Boolean isPartner;
    private String partnerTier;

    // Constructor for backwards compatibility
    public HubDto(Long id, String name, String area, String description) {
        this.id = id;
        this.name = name;
        this.area = area;
        this.description = description;
    }
}

