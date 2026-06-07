package com.hotelos.reception.repository;

import com.hotelos.reception.model.Guest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GuestRepository extends JpaRepository<Guest, Long> {
    Optional<Guest> findFirstByRoomNumberAndCheckOutAtIsNull(String roomNumber);
}
