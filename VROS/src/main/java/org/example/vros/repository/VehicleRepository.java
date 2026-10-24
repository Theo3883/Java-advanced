package org.example.vros.repository;

import org.example.vros.model.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    List<Vehicle> findByAvailableTrue();

    List<Vehicle> findByDepotId(Long depotId);

    // find vehicles with capacity >= minCapacity that are available
    @Query("SELECT v FROM Vehicle v WHERE v.available = true AND v.capacity >= :minCapacity")
    List<Vehicle> findAvailableWithMinCapacity(int minCapacity);

    // mark vehicle as unavailable
    @Modifying
    @Transactional
    @Query("UPDATE Vehicle v SET v.available = :available WHERE v.id = :id")
    int updateAvailability(Long id, boolean available);
}
