package ru.prilepskij.reservation;


import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class ReservationService {

    private static final Logger log = LoggerFactory.getLogger(ReservationService.class);

    private final ReservationRepository repository;


    public ReservationService(ReservationRepository repository) {
        this.repository = repository;
    }


    public Reservation getReservationById(Long id) {



       ReservationEntity reservationEntity = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Not found reservation by id = " + id));

        return toDomainReservation(reservationEntity);
    }

    public List<Reservation> findAllReservation() {

        List<ReservationEntity> allEntities = repository.findAll();

        return allEntities.stream()
                .map(this::toDomainReservation)
                .toList();
    }

    public Reservation createReservation(Reservation reservationToCreate) {

        if (reservationToCreate.id() != null){
            throw new IllegalArgumentException("Id should be empty");
        }

        if (reservationToCreate.status() != null){
            throw new IllegalArgumentException("Status should be empty");
        }


        var entityToSave = new ReservationEntity(
                null,
                reservationToCreate.userId(),
                reservationToCreate.roomId(),
                reservationToCreate.startDate(),
                reservationToCreate.endDate(),
                ReservationStatus.PENDING
        );

        var savedEntity = repository.save(entityToSave);

        return toDomainReservation(savedEntity);
    }

    public Reservation updateReservation(Long id, Reservation reservationToUpdate) {


        var reservationEntity = repository.findById(id)
                .orElseThrow(
                () -> new EntityNotFoundException("Not found reservation by id = " + id));


        if (reservationEntity.getStatus() !=  ReservationStatus.PENDING){
            throw new IllegalStateException("Cannot modify reservation with status " + reservationEntity.getStatus());
        }

        var reservationToSave = new ReservationEntity(
                reservationEntity.getId(),
                reservationToUpdate.userId(),
                reservationToUpdate.roomId(),
                reservationToUpdate.startDate(),
                reservationToUpdate.endDate(),
                ReservationStatus.PENDING
        );

        var updatedReservation = repository.save(reservationToSave);


        return toDomainReservation(updatedReservation);

    }

    @Transactional
    public void cancelReservation(Long id) {
        if (!repository.existsById(id)){
            throw  new EntityNotFoundException("Not found reservation by id = " + id);
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

        var isConflict = isReservationConflict(reservationEntity);

        if (isConflict){
            throw new IllegalStateException("Cannot approve reservation because of conflict ");
        }

        reservationEntity.setStatus(ReservationStatus.APPROVED);
        repository.save(reservationEntity);

        return toDomainReservation(reservationEntity);

    }

    private boolean isReservationConflict(ReservationEntity reservation){
        var allreservations = repository.findAll();
        for (ReservationEntity existingReservation: allreservations){
            if(reservation.getId().equals(existingReservation.getId())){
                continue;
            }
            if (!reservation.getRoomId().equals(existingReservation.getRoomId())){
                continue;
            }
            if (!existingReservation.getStatus().equals(ReservationStatus.APPROVED)){
                continue;
            }
            if (reservation.getStartDate().isBefore(existingReservation.getEndDate())
                    && existingReservation.getStartDate().isBefore(reservation.getEndDate())){
                return true;
            }
        }
        return false;
    }

    private Reservation toDomainReservation(ReservationEntity reservationEntity){

        return new Reservation(
                reservationEntity.getId(),
                reservationEntity.getUserId(),
                reservationEntity.getRoomId(),
                reservationEntity.getStartDate(),
                reservationEntity.getEndDate(),
                reservationEntity.getStatus()
        );

    }

}
