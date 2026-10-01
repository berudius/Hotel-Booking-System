package com.demo_resource_service.services;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.common.enums.AllocationStatus;
import com.common.dto.demo_resource_service_dto.PhysicalServiceUnitDto;
import com.demo_resource_service.data.models.PhysicalServiceUnit;
import com.demo_resource_service.exceptions.ResourceInUseException;
import com.demo_resource_service.repositories.AllocationResourceRepository;
import com.demo_resource_service.repositories.PhysicalServiceUnitRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PhysicalServiceUnitService {
    private final PhysicalServiceUnitRepository repository;
    private final AllocationResourceRepository allocationRepository;

    @Transactional(readOnly = true)
    public List<PhysicalServiceUnitDto> getAll(Pageable pageable) {
        return repository.findAll(pageable)
                .getContent()
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public PhysicalServiceUnitDto getById(Long id) {
        return mapToDto(getEntityById(id));
    }

    private PhysicalServiceUnit getEntityById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Фізичну одиницю не знайдено: " + id));
    }

    @Transactional
    public PhysicalServiceUnitDto create(PhysicalServiceUnitDto dto) {
        PhysicalServiceUnit entity = mapToEntity(dto);
        PhysicalServiceUnit saved = repository.save(entity);
        return mapToDto(saved);
    }

    @Transactional
    public PhysicalServiceUnitDto update(Long id, PhysicalServiceUnitDto details) {
        PhysicalServiceUnit existing = getEntityById(id);
        
        existing.setPremisesNumber(details.getPremisesNumber());
        existing.setClientCapacity(details.getClientCapacity());
        existing.setCleaningTimeInMinutes(details.getCleaningTimeInMinutes());
        existing.setOutOfService(details.getOutOfService());
        existing.setServiceUnitIds(details.getServiceUnitIds());

        return mapToDto(repository.save(existing));
    }

    @Transactional
    public void delete(Long id) {
        boolean inUse = allocationRepository.existsByPhysicalServiceUnitIdAndTechnicalEndAfterAndStatus(
                id, LocalDateTime.now(), AllocationStatus.ACTIVE);

        if (inUse) {
            throw new ResourceInUseException("Неможливо видалити кімнату. Для неї існують активні бронювання.");
        }
        repository.deleteById(id);
    }

    private PhysicalServiceUnitDto mapToDto(PhysicalServiceUnit entity) {
        PhysicalServiceUnitDto dto = new PhysicalServiceUnitDto();
        dto.setId(entity.getId());
        dto.setServiceUnitIds(entity.getServiceUnitIds());
        dto.setPremisesNumber(entity.getPremisesNumber());
        dto.setClientCapacity(entity.getClientCapacity());
        dto.setCleaningTimeInMinutes(entity.getCleaningTimeInMinutes());
        dto.setOutOfService(entity.isOutOfService());
        return dto;
    }

    private PhysicalServiceUnit mapToEntity(PhysicalServiceUnitDto dto) {
        PhysicalServiceUnit entity = new PhysicalServiceUnit();
        if(dto.getId() != null){
            entity.setId(dto.getId());
        }
        entity.setServiceUnitIds(dto.getServiceUnitIds());
        entity.setPremisesNumber(dto.getPremisesNumber());
        entity.setClientCapacity(dto.getClientCapacity());
        entity.setCleaningTimeInMinutes(dto.getCleaningTimeInMinutes());
        entity.setOutOfService(dto.getOutOfService());
        return entity;
    }
}