package org.vros.infrastructure.persistence.adapter;

import lombok.RequiredArgsConstructor;
import org.vros.domain.model.Delivery;
import org.vros.domain.port.DeliveryRepository;
import org.vros.infrastructure.persistence.mapper.EntityMapper;
import org.vros.infrastructure.persistence.springdata.DeliveryJpaRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeliveryRepositoryAdapter implements DeliveryRepository {

    private final DeliveryJpaRepository jpaRepo;

    @Override
    @Transactional
    @CacheEvict(value = "deliveries", allEntries = true)
    public Delivery save(Delivery delivery) {
        return EntityMapper.toDomain(jpaRepo.save(EntityMapper.toEntity(delivery)));
    }

    @Override
    @Cacheable("deliveries")
    public List<Delivery> findAll() {
        return EntityMapper.toDeliveryDomainList(jpaRepo.findAll());
    }

    @Override
    public Optional<Delivery> findById(Long id) {
        return jpaRepo.findById(id).map(EntityMapper::toDomain);
    }

    @Override
    public List<Delivery> findByCustomerId(Long customerId) {
        return EntityMapper.toDeliveryDomainList(jpaRepo.findByCustomerId(customerId));
    }

    @Override
    public List<Delivery> findByTimestampAfter(LocalDateTime since) {
        return EntityMapper.toDeliveryDomainList(jpaRepo.findByTimestampAfter(since));
    }

    @Override
    public Integer sumQuantityByCustomerId(Long customerId) {
        return jpaRepo.sumQuantityByCustomerId(customerId);
    }

    @Override
    @Transactional
    @CacheEvict(value = "deliveries", allEntries = true)
    public void deleteById(Long id) {
        jpaRepo.deleteById(id);
    }
}
