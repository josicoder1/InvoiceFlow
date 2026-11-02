package com.invoiceflow.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface InvoiceItemJpaRepository extends JpaRepository<InvoiceItemJpaEntity, UUID> {

    @Modifying
    @Query("DELETE FROM InvoiceItemJpaEntity i WHERE i.invoiceId = :invoiceId")
    void deleteAllByInvoiceId(@Param("invoiceId") UUID invoiceId);
}
