package com.javaguy.nhxserver.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record DarajaWebhookResponse(
        @JsonProperty("TransactionType")
        String transactionType,
        @JsonProperty("TransID")
        String transID,
        @JsonProperty("TransTime")
        String transTime,
        @JsonProperty("TransAmount")
        String transAmount,
        @JsonProperty("BusinessShortCode")
        String businessShortCode,
        @JsonProperty("BillRefNumber")
        String billRefNumber,
        @JsonProperty("InvoiceNumber")
        String invoiceNumber,
        @JsonProperty("OrgAccountBalance")
        String orgAccountBalance,
        @JsonProperty("ThirdPartyTransID")
        String thirdPartyTransID,
        @JsonProperty("MSISDN")
        String msisdn,
        @JsonProperty("FirstName")
        String firstName,
        @JsonProperty("MiddleName")
        String middleName,
        @JsonProperty("LastName")
        String lastName
) {
}
