package com.example.ureka02.settlement.dto;

import com.example.ureka02.settlement.enums.SettlementStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettlementProgressResponse {
    private Long settlementId;
    private SettlementStatus status;
    private Integer totalAmount;
    private Integer amountPerPerson;
    private Integer completedCount;
    private Integer totalCount;
    private Boolean isCompleted;

    public Double getProgressPercentage() {
        if (totalCount == 0) return 0.0;
        return (completedCount * 100.0) / totalCount;
    }
}
