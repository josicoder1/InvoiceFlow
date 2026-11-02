package com.invoiceflow.infrastructure.persistence;

import java.math.BigDecimal;

public record MonthlyRevenueProjection(int year, int month, BigDecimal revenue) {}
