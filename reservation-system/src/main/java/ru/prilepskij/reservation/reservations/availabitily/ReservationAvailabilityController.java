package ru.prilepskij.reservation.reservations.availabitily;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/reservation/availability")
public class ReservationAvailabilityController {

    private final ReservationAvailabilityService reservationAvailabilityService;

    private final Logger logger = LoggerFactory.getLogger(ReservationAvailabilityController.class);

    public ReservationAvailabilityController(ReservationAvailabilityService reservationAvailabilityService) {
        this.reservationAvailabilityService = reservationAvailabilityService;
    }

    @PostMapping("/check")
    public ResponseEntity<CheckAvailabilityResponse> checkAvailability(
            @Valid CheckAvailabilityRequest request
    ){
        logger.info("Checking availability availability request={}",request);
        boolean isAvailable =
                reservationAvailabilityService.isReservationAvailable(request.roomId(),
                request.startDate(), request.endDate());

        var message = isAvailable ? "Room available to reservation"
                : "Room not available to reservation";
        var status = isAvailable ? AvailabilityStatus.AVAILABLE
                : AvailabilityStatus.RESERVED;

        return  ResponseEntity.status(200)
                .body(new CheckAvailabilityResponse(message, status));
    }
}
