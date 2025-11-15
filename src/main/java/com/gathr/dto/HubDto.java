package com.gathr.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HubDto {
    private Long id;
    private String name;
    private String area;
    private String description;
}

