package com.demo_hotel_service.services;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import com.common.dto.demo_hotel_service_dto.ImageRecordDto;
import com.demo_hotel_service.data.dto_mappers.ImageMapper;
import com.demo_hotel_service.data.models.hotel_offerings.abstraction.ServiceUnit;
import com.demo_hotel_service.data.models.images.ImageRecord;
import com.demo_hotel_service.repositories.ImageRepository;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ImageService {
    private final ImageStorageService imageStorageService;
    private final ImageMapper imageMapper;
    private final ImageRepository imageRepository;
    private final EntityManager em;

    @Transactional
    public ImageRecordDto addImage(Long serviceUnitId, MultipartFile image) throws IOException {
        if (serviceUnitId == null) {
            throw new IllegalArgumentException("id cant be null");
        }

        String imageUrl = imageStorageService.store(image);
        boolean success = false;

        try {
            int position = imageRepository.findMaxPosition(serviceUnitId) + 1;
            ServiceUnit ref = em.getReference(ServiceUnit.class, serviceUnitId);

            ImageRecord imageRecord = new ImageRecord();
            imageRecord.setUrl(imageUrl);
            imageRecord.setPosition(position);
            imageRecord.setServiceUnit(ref);

            ImageRecord saved = imageRepository.save(imageRecord);
            success = true;
            return imageMapper.toDto(saved);
        } finally {
            if (!success) {
                imageStorageService.remove(imageUrl);
            }
        }
    }

    @Transactional
    public List<ImageRecordDto> addImages(Long serviceUnitId, List<MultipartFile> images) {
        if (serviceUnitId == null) {
            throw new IllegalArgumentException("id cant be null");
        }

        List<String> imageUrls = imageStorageService.storeAll(images);
        boolean success = false;
        try {
            int position = imageRepository.findMaxPosition(serviceUnitId) + 1;
            ServiceUnit ref = em.getReference(ServiceUnit.class, serviceUnitId);

            List<ImageRecord> imageRecords = new ArrayList<>();
            for (String imageUrl : imageUrls) {

                ImageRecord imageRecord = new ImageRecord();
                imageRecord.setUrl(imageUrl);
                imageRecord.setServiceUnit(ref);
                imageRecord.setPosition(position);
                position++;

                imageRecords.add(imageRecord);
            }

            List<ImageRecord> savedImageRecords = imageRepository.saveAll(imageRecords);
            success = true;

            return imageMapper.toListDtos(savedImageRecords);
        } finally {
            if (!success) {
                imageStorageService.remove(imageUrls);
            }
        }

    }



    @Transactional
    public Boolean removeImage(Long imageId, String imageUrl) {
        if (imageId == null || imageUrl == null || imageUrl.isBlank()) {
            throw new IllegalArgumentException("Params cant be null or empty");
        }

        imageRepository.deleteById(imageId);

        TransactionSynchronizationManager.registerSynchronization(
            new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        imageStorageService.remove(imageUrl);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        );

        return imageRepository.existsById(imageId);
    }

    @Transactional
    public void removeImages(List<Long> imageIds, List<String> imageUrls) {
        imageRepository.deleteAllById(imageIds);
        TransactionSynchronizationManager.registerSynchronization(
            new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        imageStorageService.remove(imageUrls);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        );
    }

    @Transactional
    public void removeImages(Long serviceUnitId){
        if (serviceUnitId == null){
            throw new IllegalArgumentException("Params cant be null or empty");
        }

        List<String> imageUrls = imageRepository.findImageUrls(serviceUnitId);
        imageRepository.deleteByServiceUnitId(serviceUnitId);

        TransactionSynchronizationManager.registerSynchronization(
            new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        imageStorageService.remove(imageUrls);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        );
    }

    @Transactional
    public void swapPositions(Long id1, Long id2) {
        if (id1 == null || id2 == null){
            throw new IllegalArgumentException("Params cant be null or empty");
        }
        Integer pos1 = imageRepository.findPositionById(id1);
        Integer pos2 = imageRepository.findPositionById(id2);

        imageRepository.updatePosition(id2, -1);
        imageRepository.updatePosition(id1, pos2);
        imageRepository.updatePosition(id2, pos1);
    }

    public List<ImageRecordDto> prepareInitialImageDtos(List<MultipartFile> imageFiles) {
        if (imageFiles == null || imageFiles.isEmpty()) {
            return List.of();
        }

        List<String> storedImageUrls = imageStorageService.storeAll(imageFiles);

        List<ImageRecordDto> imageDtos = new ArrayList<>();
        int position = 1;
        for (String imageUrl : storedImageUrls) {

            ImageRecordDto dto = new ImageRecordDto();
            dto.setPosition(position);
            dto.setUrl(imageUrl);

            imageDtos.add(dto);

            position++;
        }

        return imageDtos;
    }

}
