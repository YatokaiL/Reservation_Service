package ru.prilepskij.reservation;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class ReservationController {


    private static final Logger log = LoggerFactory.getLogger(ReservationController.class);

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @GetMapping("/{id}")
    public Reservation getReservationById(@PathVariable("id") Long id){
        log.info("Called getReservationById: id=" + id);

        return reservationService.getReservationById(id);
    }

    @GetMapping()
    public List<Reservation> getAllReservation(){
        log.info("Called getReservationAllId");
        return reservationService.findAllReservation();
    }

    @PostMapping
    public Reservation createReservation(@RequestBody Reservation reservationToCreate){
        log.info("Called CreateReservation");
        return reservationService.createReservation(reservationToCreate);

    }

}
