package com.demo_hotel_service.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.demo_hotel_service.repositories.ServiceUnitRepository;

import jakarta.persistence.EntityNotFoundException;

import com.common.dto.demo_hotel_service_dto.ImageRecordDto;
import com.common.dto.demo_hotel_service_dto.ServiceUnitDto;
import com.common.dto.demo_hotel_service_dto.ServiceUnitShortDto;
import com.common.security.AuthPrincipal;
import com.demo_hotel_service.data.dto_mappers.ServiceUnitMapper;
import com.demo_hotel_service.data.models.hotel_offerings.abstraction.ServiceUnit;
import com.demo_hotel_service.data.models.images.ImageRecord;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class ServiceUnitService {
    private final ServiceUnitRepository serviceUnitRepository;
    private final ServiceUnitMapper serviceUnitMapper;
    private final ImageService imageService;

    public List<ServiceUnitDto> getServiceUnits(AuthPrincipal principal, Class<? extends ServiceUnit> classType, Pageable pageable) {
        List<ServiceUnit> serviceUnits = serviceUnitRepository.findByTypeFiltered(classType, isAdmin(principal), pageable);
        List<ServiceUnitDto> serviceUnitDtos = serviceUnitMapper.toPolymorphicDtos(serviceUnits);
        sortImageRecordsOfServiceUnits(serviceUnitDtos);
        return serviceUnitDtos;
    }

    public ServiceUnitDto getById(long id) {
        ServiceUnit existing = serviceUnitRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Service with not found"));
        ServiceUnitDto existingDto = serviceUnitMapper.toPolymorphicDto(existing);
        sortImageRecordsOfServiceUnit(existingDto);
        return existingDto;
    }

    public ServiceUnitDto addServiceUnit(ServiceUnitDto serviceUnitDto, List<MultipartFile> imageFiles) {
        if (imageFiles != null && !imageFiles.isEmpty()) {
            List<ImageRecordDto> imageDtos = imageService.prepareInitialImageDtos(imageFiles);
            serviceUnitDto.setImageRecords(imageDtos);
        }

        var entity = serviceUnitMapper.toPolymorphicEntity(serviceUnitDto);
        for (ImageRecord imageRecord : entity.getImageRecords()) {
            imageRecord.setServiceUnit(entity);
        }

        ServiceUnitDto savedDto = serviceUnitMapper
                .toPolymorphicDto(serviceUnitRepository.save(entity));

        return savedDto;
    }

    @Transactional
    public ServiceUnitDto updateServiceUnit(ServiceUnitDto updatingData, List<MultipartFile> imageFiles) {
        ServiceUnit toUpdate = serviceUnitRepository.findById(updatingData.getId())
                .orElseThrow(() -> new EntityNotFoundException("Service not found"));

        if (imageFiles != null && !imageFiles.isEmpty()) {
            imageService.addImages(updatingData.getId(), imageFiles);
        }

        serviceUnitMapper.updateEntity(updatingData, toUpdate);
        ServiceUnitDto updatedDto = serviceUnitMapper.toPolymorphicDto(serviceUnitRepository.save(toUpdate));
        return updatedDto;
    }

    @Transactional
    public Boolean deleteServiceUnit(Long serviceId) {
        if (serviceId == null) {
            throw new IllegalArgumentException();
        }
        if (!serviceUnitRepository.existsById(serviceId)) {
            throw new EntityNotFoundException("Service not found");
        }

        imageService.removeImages(serviceId);
        serviceUnitRepository.deleteById(serviceId);
        return serviceUnitRepository.existsById(serviceId);
    }

    private void sortImageRecordsOfServiceUnits(List<ServiceUnitDto> serviceUnitDtos) {
        serviceUnitDtos.forEach(su -> {
            su.getImageRecords().sort((img1, img2) -> img1.getPosition() > img2.getPosition() ? 1 : -1);
        });
    }

    private void sortImageRecordsOfServiceUnit(ServiceUnitDto serviceUnitDto) {
        serviceUnitDto.getImageRecords().sort((img1, img2) -> img1.getPosition() < img2.getPosition() ? 1 : -1);
    }

    @Transactional(readOnly = true)
    public List<ServiceUnitShortDto> searchShortServiceUnits(String searchTerm, String category, int limit) {
        String term = (searchTerm == null) ? "" : searchTerm;
        Pageable pageable = PageRequest.of(0, limit);

        if ("SPA".equalsIgnoreCase(category)) {
            return serviceUnitRepository.searchSpaUnitsShort(term, pageable);

        } else if ("ROOM".equalsIgnoreCase(category)) {
            return serviceUnitRepository.searchRoomUnitsShort(term, pageable);

        } else {
            List<ServiceUnitShortDto> combined = new ArrayList<>();
            combined.addAll(serviceUnitRepository.searchSpaUnitsShort(term, PageRequest.of(0, limit / 2)));
            combined.addAll(serviceUnitRepository.searchRoomUnitsShort(term, PageRequest.of(0, limit / 2)));
            return combined;
        }
    }

    public List<ServiceUnitShortDto> getShortSpaUnitsByIds(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return serviceUnitRepository.findServiceUnitsShortByIds(ids);
    }

    private boolean isAdmin(AuthPrincipal principal){
        boolean isAdmin = false;
        if(principal != null && "ADMIN".equals(principal.getRole())){
            isAdmin = true;
        }

        return isAdmin;
    }

}
