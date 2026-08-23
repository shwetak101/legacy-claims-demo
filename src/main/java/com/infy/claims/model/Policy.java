package com.infy.claims.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "POLICIES")
public class Policy {

    @Id
    private String policyNumber;
    private String customerId;
    private String policyType;
    private Double sumInsured;
    private Double premium;
    private Double deductible;
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean isGroupPolicy;
    private Integer groupSize;

    public String getPolicyNumber() { return policyNumber; }
    public void setPolicyNumber(String policyNumber) { this.policyNumber = policyNumber; }
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public String getPolicyType() { return policyType; }
    public void setPolicyType(String policyType) { this.policyType = policyType; }
    public Double getSumInsured() { return sumInsured; }
    public void setSumInsured(Double sumInsured) { this.sumInsured = sumInsured; }
    public Double getPremium() { return premium; }
    public void setPremium(Double premium) { this.premium = premium; }
    public Double getDeductible() { return deductible; }
    public void setDeductible(Double deductible) { this.deductible = deductible; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public Boolean getIsGroupPolicy() { return isGroupPolicy; }
    public void setIsGroupPolicy(Boolean groupPolicy) { this.isGroupPolicy = groupPolicy; }
    public Integer getGroupSize() { return groupSize; }
    public void setGroupSize(Integer groupSize) { this.groupSize = groupSize; }
}
