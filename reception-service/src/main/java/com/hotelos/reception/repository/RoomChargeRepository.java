package com.hotelos.reception.repository;

import com.hotelos.reception.model.RoomCharge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoomChargeRepository extends JpaRepository<RoomCharge, Long> {
    List<RoomCharge> findByRoomNumber(String roomNumber);
    void deleteByRoomNumber(String roomNumber);
}
