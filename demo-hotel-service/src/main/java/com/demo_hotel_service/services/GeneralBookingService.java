package com.demo_hotel_service.services;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.common.dto.demo_hotel_service_dto.BookingUnitDto;
import com.common.dto.demo_hotel_service_dto.GeneralBookingDto;
import com.common.dto.demo_resource_service_dto.AllocationResourceDto;
import com.common.enums.BookingStatus;
import com.common.security.AuthPrincipal;
import com.demo_hotel_service.clients.ResourceServiceClient;
import com.demo_hotel_service.clients.UserServiceClient;
import com.demo_hotel_service.data.dto_mappers.GeneralBookingMapper;
import com.demo_hotel_service.data.models.bookings.BookingSpaUnit;
import com.demo_hotel_service.data.models.bookings.GeneralBooking;
import com.demo_hotel_service.data.models.hotel_offerings.abstraction.ServiceUnit;
import com.demo_hotel_service.repositories.GeneralBookingRepository;

import feign.FeignException;
import jakarta.persistence.EntityManager;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class GeneralBookingService {
    private final GeneralBookingMapper mapper;
    private final GeneralBookingRepository repository;
    private final EntityManager entityManager;
    private final ResourceServiceClient resourceServiceClient;
    private final UserServiceClient userServiceClient;


    public List<GeneralBookingDto> getGeneralBookingsForAdmin(boolean showAll, Pageable pageable) {
        List<GeneralBooking> gBokings = repository.findAllFiltered(showAll, pageable);
        return mapper.toDtos(gBokings);
    }

    public List<GeneralBookingDto> getGeneralBookingsOfUser(AuthPrincipal userDetails, boolean showAll, Pageable pageable) {
        if (userDetails == null)
            return List.of();

        List<GeneralBooking> gBokings = repository.findAllFiltered(
                userDetails.getId(),
                userDetails.getPhoneNumber(),
                showAll,
                pageable);
        return mapper.toDtos(gBokings);
    }

    public GeneralBookingDto getById(@NonNull Long id) {
        return mapper.toDto(repository.findById(id).orElseThrow());
    }

    @Transactional
    public GeneralBookingDto addGeneralBooking(GeneralBookingDto dto, AuthPrincipal principal) {
        validateAndSetupUserContext(dto, principal);

        GeneralBooking savedEntity = saveInitialBooking(dto);

        try {
            List<AllocationResourceDto> allocations = resourceServiceClient.allocateResources(
                prepareAllocationRequests(savedEntity)
            );

            finalizeBooking(savedEntity, allocations);
            return mapToResponseDto(savedEntity, allocations);

        } catch (FeignException e) {
            System.err.println("Автоматична алокація не вдалася (немає ресурсів). Бронювання #" + savedEntity.getId() + " залишено у статусі PENDING.");
            return mapper.toDto(savedEntity);
        } catch (Exception e) {
            compensateAllocation(savedEntity);
            throw new RuntimeException("Локальна помилка збереження. Бронювання скасовано: " + e.getMessage());
        }
    }


    @Transactional
    @PreAuthorize("@securityCheck.isGeneralBookingOwner(#dto.id, principal) or hasRole('ADMIN')")
    public GeneralBookingDto updateGeneralBooking(GeneralBookingDto dto) {
        GeneralBooking existing = repository.findById(dto.getId()).orElseThrow();
        GeneralBooking updated = mapper.updateEntity(dto, existing);
        return mapper.toDto(repository.save(updated));
    }

    
    @Transactional
    @PreAuthorize("@securityCheck.isGeneralBookingOwner(#id, principal) or hasRole('ADMIN')")
    public GeneralBookingDto updateGeneralBookingStatus(Long id, BookingStatus newStatus, List<AllocationResourceDto> manualAllocations, AuthPrincipal principal) {
        GeneralBooking existing = repository.findById(id).orElseThrow();
        BookingStatus oldStatus = existing.getStatus();
        boolean isAdmin = "ADMIN".equals(principal.getRole());

        validateStateTransition(oldStatus, newStatus, isAdmin);

        if (newStatus == BookingStatus.CANCELLED_BY_CLIENT || newStatus == BookingStatus.REJECTED) {
            Long bookingUserId = existing.getUserId();
            
            // Якщо є ID і він збігається з поточним юзером - скасував сам клієнт
            if (bookingUserId != null && bookingUserId.equals(principal.getId())) {
                newStatus = BookingStatus.CANCELLED_BY_CLIENT;
            } 
            // Якщо це анонімне бронювання (null) і дію робить НЕ адмін - скасував сам клієнт
            else if (bookingUserId == null && !isAdmin) {
                newStatus = BookingStatus.CANCELLED_BY_CLIENT;
            } 
            // Якщо це адмін скасовує чуже або анонімне бронювання - відхилено адміном
            else if (isAdmin) {
                newStatus = BookingStatus.REJECTED;
            }
        }

        // МАСОВЕ ОНОВЛЕННЯ СТАТУСІВ ДОЧІРНІХ ЮНІТІВ ТА РЕСУРСІВ
        if (oldStatus == BookingStatus.PENDING && newStatus == BookingStatus.CONFIRMED) {
            try {
                List<AllocationResourceDto> allocationRequests = (manualAllocations != null && !manualAllocations.isEmpty()) 
                        ? manualAllocations : prepareAllocationRequests(existing);
                List<AllocationResourceDto> allocations = resourceServiceClient.allocateResources(allocationRequests);
                finalizeBooking(existing, allocations); 
            } catch (FeignException e) {
                throw new RuntimeException("Не вдалося підтвердити: відсутні ресурси. Призначте їх вручну.");
            }
        } 
        else if (isTerminalCancellation(newStatus)) {
            // Скасовуємо всі нетермінальні юніти і звільняємо ресурси
            resourceServiceClient.updateStatusByGeneralBooking(existing.getId(), "CANCELLED");
            final BookingStatus cancelStatus = newStatus;
            existing.getBookingUnits().forEach(bu -> {
                if (!isTerminal(bu.getStatus())) bu.setStatus(cancelStatus);
            });
        } 
        else if (newStatus == BookingStatus.COMPLETED) {
            // Завершуємо всі CONFIRMED юніти
            resourceServiceClient.updateStatusByGeneralBooking(existing.getId(), "COMPLETED");
            existing.getBookingUnits().forEach(bu -> {
                if (bu.getStatus() == BookingStatus.CONFIRMED) bu.setStatus(BookingStatus.COMPLETED);
            });
        }

        existing.setStatus(newStatus);
        return mapper.toDto(repository.save(existing));
    }
    
    private void validateStateTransition(BookingStatus oldStatus, BookingStatus newStatus, boolean isAdmin) {
        if (oldStatus == newStatus) return;

        if (isTerminal(oldStatus)) {
            throw new IllegalStateException("Неможливо змінити статус: замовлення вже " + oldStatus);
        }

        if (newStatus == BookingStatus.EXPIRED) {
            throw new IllegalArgumentException("Статус EXPIRED встановлюється виключно автоматично системою, а не через API.");
        }

        if (!isAdmin && newStatus != BookingStatus.CANCELLED_BY_CLIENT) {
            throw new AccessDeniedException("Клієнт може лише скасувати замовлення.");
        }

        if (oldStatus == BookingStatus.PENDING && newStatus == BookingStatus.COMPLETED) {
            throw new IllegalStateException("Неможливо завершити (COMPLETED) непідтверджене замовлення.");
        }

        if (oldStatus == BookingStatus.CONFIRMED && newStatus == BookingStatus.PENDING) {
            throw new IllegalStateException("Неможливо повернути підтверджене замовлення в статус PENDING.");
        }
    }

    private boolean isTerminal(BookingStatus status) {
        return status == BookingStatus.COMPLETED || isTerminalCancellation(status);
    }

    private boolean isTerminalCancellation(BookingStatus status) {
        return status == BookingStatus.CANCELLED_BY_CLIENT || 
               status == BookingStatus.REJECTED || 
               status == BookingStatus.EXPIRED;
    }

    @Transactional
    public void claimOrphanBookings(String phoneNumber, Long userId) {
        repository.linkOrphanBookings(phoneNumber, userId);
    }

    private void validateAndSetupUserContext(GeneralBookingDto dto, AuthPrincipal principal) {
        if (principal != null) {
            dto.setUserId(principal.getId());
            if (!dto.getPhoneNumber().equals(principal.getPhoneNumber())) {
                if (userServiceClient.checkPhoneExists(dto.getPhoneNumber())) {
                    throw new IllegalArgumentException(
                            "Вказаний альтернативний номер вже належить іншому користувачу.");
                }
            }
        } else if (userServiceClient.checkPhoneExists(dto.getPhoneNumber())) {
            throw new IllegalArgumentException("Цей номер вже зареєстрований. Будь ласка, увійдіть в акаунт.");
        }
    }

    private GeneralBooking saveInitialBooking(GeneralBookingDto dto) {
        Map<Long, ServiceUnit> proxyMap = dto.getBookingUnits().stream()
                .map(BookingUnitDto::getServiceUnitId)
                .distinct()
                .collect(Collectors.toMap(id -> id, id -> entityManager.getReference(ServiceUnit.class, id)));

        GeneralBooking entity = mapper.toEntity(dto);
        entity.setStatus(BookingStatus.PENDING);
        
        var unitDtos = dto.getBookingUnits();
        var unitEntities = entity.getBookingUnits();

        for (int i = 0; i < unitDtos.size(); i++) {
            var buDto = unitDtos.get(i);
            var buEntity = unitEntities.get(i);

            ServiceUnit realService = proxyMap.get(buDto.getServiceUnitId());
            
            buEntity.setServiceUnit(realService);
            buEntity.setGeneralBooking(entity);
            buEntity.setStatus(BookingStatus.PENDING);
            
            buEntity.setAmount(buEntity.calculateAmount()); 
        }
        
        return repository.save(entity);
    }

    private List<AllocationResourceDto> prepareAllocationRequests(GeneralBooking savedEntity) {
        return savedEntity.getBookingUnits().stream()
                .filter(bu -> bu.getStatus() == BookingStatus.PENDING)
                .map(bu -> {
                    AllocationResourceDto allocDto = new AllocationResourceDto();
                    allocDto.setBookingUnitId(bu.getId());
                    allocDto.setServiceUnitId(bu.getServiceUnit().getId());
                    allocDto.setGeneralBookingId(savedEntity.getId());
                    allocDto.setStart(bu.getStart());
                    allocDto.setEnd(bu.getEnd());
                    allocDto.setClientCount(bu.getClientCount());

                    if (bu instanceof BookingSpaUnit spaUnit) {
                        allocDto.setRequiresWorker(true);
                        if (spaUnit.getPreferedGender() != null) {
                            allocDto.setPreferedGender(spaUnit.getPreferedGender());
                        }
                    } else {
                        allocDto.setRequiresWorker(false);
                    }
                    return allocDto;
                }).collect(Collectors.toList());
    }

   private void finalizeBooking(GeneralBooking entity, List<AllocationResourceDto> allocations) {
        // Якщо алокацій не було (наприклад, всі юніти вже були CONFIRMED), просто міняємо статус батька
        if (allocations == null || allocations.isEmpty()) {
            entity.setStatus(BookingStatus.CONFIRMED);
            repository.save(entity);
            return;
        }

        // Створюємо множину ID тих послуг, яким успішно призначено ресурси
        Set<Long> allocatedUnitIds = allocations.stream()
                .map(AllocationResourceDto::getBookingUnitId)
                .collect(Collectors.toSet());

        entity.setStatus(BookingStatus.CONFIRMED);
        
        entity.getBookingUnits().forEach(bu -> {
            //  Оновлюємо статус тільки для тих послуг, які є у списку успішних алокацій
            if (allocatedUnitIds.contains(bu.getId())) {
                bu.setStatus(BookingStatus.CONFIRMED);
            }
        });
        
        repository.save(entity);
    }

    private GeneralBookingDto mapToResponseDto(GeneralBooking entity, List<AllocationResourceDto> allocations) {
        GeneralBookingDto responseDto = mapper.toDto(entity);
        return responseDto;
    }

    private void compensateAllocation(GeneralBooking entity) {
        try {
            resourceServiceClient.updateStatusByGeneralBooking(entity.getId(), "CANCELLED");
        } catch (Exception rollbackEx) {
            System.err.println("CRITICAL: Failed to rollback resources for booking " + entity.getId());
        }
    }

}