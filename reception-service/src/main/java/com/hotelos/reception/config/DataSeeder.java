package com.hotelos.reception.config;

import com.hotelos.reception.model.Room;
import com.hotelos.reception.model.RoomStatus;
import com.hotelos.reception.model.RoomType;
import com.hotelos.reception.repository.RoomRepository;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Seeds the 10-room demo inventory across two floors per BTEC assignment
 * guidance ("120 rooms not required; 10 rooms across 2 floors is fine").
 *
 * Rooms 101-105 on floor 1, 201-205 on floor 2. Mix of types so every test
 * scenario has a viable candidate.
 */
@Configuration
public class DataSeeder {

    @Bean
    public ApplicationRunner seedRooms(RoomRepository rooms) {
        return args -> {
            if (rooms.count() > 0) return;
            Instant now = Instant.now();

            // Floor 1
            rooms.save(new Room("101", 1, RoomType.SINGLE,     RoomStatus.CLEAN,  5,  new BigDecimal("80.00"),  now.minus(3, ChronoUnit.DAYS)));
            rooms.save(new Room("102", 1, RoomType.DOUBLE,     RoomStatus.CLEAN, 12,  new BigDecimal("120.00"), now.minus(5, ChronoUnit.DAYS)));
            rooms.save(new Room("103", 1, RoomType.DOUBLE,     RoomStatus.CLEAN, 18,  new BigDecimal("120.00"), now.minus(2, ChronoUnit.DAYS)));
            rooms.save(new Room("104", 1, RoomType.SUITE,      RoomStatus.CLEAN,  8,  new BigDecimal("250.00"), now.minus(4, ChronoUnit.DAYS)));
            rooms.save(new Room("105", 1, RoomType.ACCESSIBLE, RoomStatus.CLEAN,  3,  new BigDecimal("100.00"), now.minus(1, ChronoUnit.DAYS)));

            // Floor 2
            rooms.save(new Room("201", 2, RoomType.SINGLE,     RoomStatus.CLEAN,  5,  new BigDecimal("85.00"),  now.minus(6, ChronoUnit.DAYS)));
            rooms.save(new Room("202", 2, RoomType.DOUBLE,     RoomStatus.CLEAN, 12,  new BigDecimal("125.00"), now.minus(7, ChronoUnit.DAYS)));
            rooms.save(new Room("203", 2, RoomType.DOUBLE,     RoomStatus.CLEAN, 18,  new BigDecimal("125.00"), now.minus(4, ChronoUnit.DAYS)));
            rooms.save(new Room("204", 2, RoomType.SUITE,      RoomStatus.CLEAN,  8,  new BigDecimal("260.00"), now.minus(8, ChronoUnit.DAYS)));
            rooms.save(new Room("205", 2, RoomType.ACCESSIBLE, RoomStatus.CLEAN,  3,  new BigDecimal("105.00"), now.minus(2, ChronoUnit.DAYS)));
        };
    }
}
