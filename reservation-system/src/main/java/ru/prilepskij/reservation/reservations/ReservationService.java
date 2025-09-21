package ru.prilepskij.reservation.reservations;


import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.prilepskij.reservation.reservations.availabitily.ReservationAvailabilityService;

import java.time.LocalDate;
import java.util.*;

@Service
public class ReservationService {

    private static final Logger log = LoggerFactory.getLogger(ReservationService.class);

    private final ReservationRepository repository;

    private final ReservationMapper mapper;

    private final ReservationAvailabilityService reservationAvailabilityService;

    public ReservationService(ReservationRepository repository, ReservationMapper mapper, ReservationAvailabilityService reservationAvailabilityService) {
        this.repository = repository;
        this.mapper = mapper;
        this.reservationAvailabilityService = reservationAvailabilityService;
    }

    public Reservation getReservationById(Long id) {
       ReservationEntity reservationEntity = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Not found reservation by id = " + id));

        return mapper.toDomain(reservationEntity);
    }

    public List<Reservation> searchAllByFilter(
            ReservationSearchFilter filter
    ) {
        int pageSize = filter.pageSize() != null ? filter.pageSize() : 10;
        int pageNumber = filter.pageNumber() != null ? filter.pageNumber() : 0;

        var pageable = PageRequest
                .ofSize(pageSize)
                .withPage(pageNumber);

        List<ReservationEntity> allEntities = repository.searchAllByFilter(filter.roomId(),
                filter.userId(),
                pageable);

        return allEntities.stream()
                .map(mapper::toDomain)
                .toList();
    }

    public Reservation createReservation(Reservation reservationToCreate) {
        if (reservationToCreate.status() != null){
            throw new IllegalArgumentException("Status should be empty");
        }
        if (!reservationToCreate.endDate().isAfter(reservationToCreate.startDate())){
            throw new IllegalArgumentException("Start date must be 1 day earlier tham end date");
        }

        var entityToSave = mapper.toEntity(reservationToCreate);
        entityToSave.setStatus(ReservationStatus.PENDING);

        var savedEntity = repository.save(entityToSave);
        return mapper.toDomain(savedEntity);
    }

    public Reservation updateReservation(Long id, Reservation reservationToUpdate) {
        var reservationEntity = repository.findById(id)
                .orElseThrow(
                () -> new EntityNotFoundException("Not found reservation by id = " + id));

        if (reservationEntity.getStatus() !=  ReservationStatus.PENDING){
            throw new IllegalStateException("Cannot modify reservation with status " + reservationEntity.getStatus());
        }

        if (!reservationToUpdate.endDate().isAfter(reservationToUpdate.startDate())){
            throw new IllegalArgumentException("Start date must be 1 day earlier tham end date");
        }

        var reservationToSave = mapper.toEntity(reservationToUpdate);
        reservationToSave.setId(reservationEntity.getId());
        reservationToSave.setStatus(ReservationStatus.PENDING);

        var updatedReservation = repository.save(reservationToSave);
        return mapper.toDomain(updatedReservation);
    }

    @Transactional
    public void cancelReservation(Long id) {

        var reservationEntity = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Not found reservation by id = " + id));

        if(reservationEntity.getStatus().equals(ReservationStatus.APPROVED)){
            throw new IllegalStateException("Cannot cancel approved reservation. Contact with manager please");
        }

        if(reservationEntity.getStatus().equals(ReservationStatus.CANCELLED)){
            throw new IllegalStateException("Cannot cancel canceled reservation" );
        }

        repository.setStatus(id, ReservationStatus.CANCELLED);
        log.info("Successfully cancelled reservation id = {}", id);
    }

    public Reservation approveReservation(Long id) {
        var reservationEntity = repository.findById(id)
                .orElseThrow(
                        () -> new EntityNotFoundException("Not found reservation by id = " + id));

        if (reservationEntity.getStatus() !=  ReservationStatus.PENDING){
            throw new IllegalStateException("Cannot approve reservation with status " + reservationEntity.getStatus());
        }


        var isAvailableToApprove = reservationAvailabilityService.isReservationAvailable(
                reservationEntity.getRoomId(),
                reservationEntity.getStartDate(),
                reservationEntity.getEndDate()
        );

        if (isAvailableToApprove){
            throw new IllegalStateException("Cannot approve reservation because of conflict ");
        }

        reservationEntity.setStatus(ReservationStatus.APPROVED);
        repository.save(reservationEntity);

        return mapper.toDomain(reservationEntity);
    }


}
