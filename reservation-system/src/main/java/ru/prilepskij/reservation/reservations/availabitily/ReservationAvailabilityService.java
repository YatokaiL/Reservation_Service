package ru.prilepskij.reservation.reservations.availabitily;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.prilepskij.reservation.reservations.ReservationRepository;
import ru.prilepskij.reservation.reservations.ReservationStatus;

import java.time.LocalDate;
import java.util.List;

@Service
public class ReservationAvailabilityService {

    private static final Logger log = LoggerFactory.getLogger(ReservationAvailabilityService.class);

    private final ReservationRepository repository;


    public ReservationAvailabilityService(ReservationRepository repository) {
        this.repository = repository;
    }

    public boolean isReservationAvailable(
            Long roomId,
            LocalDate startDate,
            LocalDate endDate
    ){
        if (!endDate.isAfter(startDate)){
            throw new IllegalArgumentException("Start date must be 1 day earlier tham end date");
        }

        List<Long> conflictingIds = repository.findConflictReservationsIds(roomId, startDate, endDate, ReservationStatus.APPROVED);

        if (conflictingIds.isEmpty()){
            return true;
        }

        log.info("Conflicting reservations ids = {}", conflictingIds);
        return false;
    }
}
