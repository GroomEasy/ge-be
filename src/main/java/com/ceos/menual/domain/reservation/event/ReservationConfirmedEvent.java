package com.ceos.menual.domain.reservation.event;

public record ReservationConfirmedEvent(
    Long reservationId,
    Long consultationId,
    Long adminUserId
) {}

