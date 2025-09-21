package ru.prilepskij.reservation.reservations.availabitily;

public record CheckAvailabilityResponse(
        String message,
        AvailabilityStatus status
) {
}
