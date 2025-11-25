package com.gathr.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedInteractionRequest {

    @NotNull
    private Long activityId;

    private Long hubId;

    @NotNull
    @Pattern(regexp = "viewed|clicked|joined|dismissed", message = "Invalid action")
    private String action;

    @Min(0)
    @Max(100)
    private Integer position;

    private Double score;

    private String sessionId;
}
