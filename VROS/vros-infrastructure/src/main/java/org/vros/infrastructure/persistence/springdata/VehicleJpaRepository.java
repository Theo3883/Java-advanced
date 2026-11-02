package org.vros.infrastructure.persistence.springdata;

import org.vros.infrastructure.persistence.entity.VehicleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface VehicleJpaRepository extends JpaRepository<VehicleEntity, Long> {

    List<VehicleEntity> findByAvailableTrue();

    List<VehicleEntity> findByDepotId(Long depotId);

    @Query("SELECT v FROM VehicleEntity v WHERE v.available = true AND v.capacity >= :minCapacity")
    List<VehicleEntity> findAvailableWithMinCapacity(int minCapacity);

    @Modifying
    @Transactional
    @Query("UPDATE VehicleEntity v SET v.available = :available WHERE v.id = :id")
    int updateAvailability(Long id, boolean available);
}
