package com.techora.payment.infra.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface PaymentJpaRepository extends JpaRepository<PaymentJpaEntity, UUID> {

    Optional<PaymentJpaEntity> findByIdAndUserId(UUID paymentId, UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select payment from PaymentJpaEntity payment where payment.id = :paymentId")
    Optional<PaymentJpaEntity> findLockedById(@Param("paymentId") UUID paymentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select payment from PaymentJpaEntity payment
            where payment.orderId = :orderId
            """)
    Optional<PaymentJpaEntity> findLockedByOrderId(@Param("orderId") UUID orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select payment from PaymentJpaEntity payment
            where payment.orderId = :orderId
              and payment.userId = :userId
            """)
    Optional<PaymentJpaEntity> findLockedByOrderIdAndUserId(
            @Param("orderId") UUID orderId,
            @Param("userId") UUID userId
    );
}
