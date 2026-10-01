package com.demo_hotel_service.services;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.common.dto.demo_hotel_service_dto.BookingUnitDto;
import com.common.dto.demo_resource_service_dto.AllocationResourceDto;
import com.common.enums.BookingStatus;
import com.common.security.AuthPrincipal;
import com.demo_hotel_service.clients.ResourceServiceClient;
import com.demo_hotel_service.data.dto_mappers.BookingUnitMapper;
import com.demo_hotel_service.data.models.bookings.BookingSpaUnit;
import com.demo_hotel_service.data.models.bookings.BookingUnit;
import com.demo_hotel_service.data.models.bookings.GeneralBooking;
import com.demo_hotel_service.data.models.hotel_offerings.abstraction.ServiceUnit;
import com.demo_hotel_service.repositories.BookingUnitRepository;
import com.demo_hotel_service.repositories.GeneralBookingRepository;

import feign.FeignException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BookingUnitService {
    private final BookingUnitRepository bookingRepository;
    private final GeneralBookingRepository gBookingRepository;
    private final BookingUnitMapper bookingMapper;
    private final EntityManager entityManager;
    private final ResourceServiceClient resourceServiceClient;

    @PreAuthorize("@securityCheck.isGeneralBookingOwner(#generalBookingId, principal)")
    public List<BookingUnitDto> getBookingUnits(Long generalBookingId, Pageable pageable) {
        List<BookingUnit> bookingUnits = bookingRepository.findAllByGeneralBookingId(generalBookingId, pageable);
        return bookingMapper.toPolymorphicDtos(bookingUnits);
    }

    @PreAuthorize("@securityCheck.isBookingUnitOwner(#id, principal)")
    public BookingUnitDto getBookingUnitById(Long id) {
        BookingUnit bookingUnit = bookingRepository.findById(id).orElseThrow();
        return bookingMapper.toPolymorphicDto(bookingUnit);
    }

    @Transactional
    @PreAuthorize("@securityCheck.isGeneralBookingOwner(#generalBookingId, principal)")
    public List<BookingUnitDto> addBookingUnits(Long generalBookingId, List<BookingUnitDto> dtos) {
        if (!gBookingRepository.existsById(generalBookingId)) {
            throw new IllegalArgumentException("Загальне бронювання з таким ID не існує");
        }

        Map<Long, ServiceUnit> proxyMap = dtos.stream()
                .map(BookingUnitDto::getServiceUnitId)
                .distinct()
                .collect(Collectors.toMap(id -> id, id -> entityManager.getReference(ServiceUnit.class, id)));

        GeneralBooking generalBookingRef = gBookingRepository.findById(generalBookingId)
                .orElseThrow(() -> new EntityNotFoundException("Замовлення не знайдено"));

        if (generalBookingRef.getStatus() == BookingStatus.COMPLETED ||
                generalBookingRef.getStatus() == BookingStatus.CANCELLED_BY_CLIENT ||
                generalBookingRef.getStatus() == BookingStatus.REJECTED) {
            throw new IllegalStateException("Неможливо додати послуги: це замовлення вже закрите.");
        }

        List<BookingUnit> entities = bookingMapper.toPolymorphicEntities(dtos);
        for (int i = 0; i < dtos.size(); i++) {
            BookingUnit e = entities.get(i);
            BookingUnitDto dto = dtos.get(i);

            e.setGeneralBooking(generalBookingRef);
            e.setServiceUnit(proxyMap.get(dto.getServiceUnitId())); 
            e.setStatus(BookingStatus.PENDING);
            e.setAmount(e.calculateAmount());
        }

        List<BookingUnit> savedEntities = bookingRepository.saveAll(entities);

        List<AllocationResourceDto> allocationRequests = savedEntities.stream()
                .map(bu -> {
                    AllocationResourceDto allocDto = new AllocationResourceDto();
                    allocDto.setBookingUnitId(bu.getId());
                    allocDto.setServiceUnitId(bu.getServiceUnit().getId());
                    allocDto.setGeneralBookingId(generalBookingRef.getId());
                    allocDto.setStart(bu.getStart());
                    allocDto.setEnd(bu.getEnd());
                    allocDto.setClientCount(bu.getClientCount());
                    allocDto.setRequiresWorker(bu instanceof BookingSpaUnit);
                    return allocDto;
                }).collect(Collectors.toList());

        try {
            resourceServiceClient.allocateResources(allocationRequests);

            savedEntities.forEach(bu -> bu.setStatus(BookingStatus.CONFIRMED));
            bookingRepository.saveAll(savedEntities);

            syncParentGeneralBookingStatus(generalBookingRef);

            List<BookingUnitDto> responseDtos = bookingMapper.toPolymorphicDtos(savedEntities);

            return responseDtos;

        } catch (FeignException e) {
            System.err.println("Автоматична алокація додаткових послуг не вдалася. Статус PENDING збережено.");

            syncParentGeneralBookingStatus(generalBookingRef);

            return bookingMapper.toPolymorphicDtos(savedEntities);
        } catch (Exception e) {
            savedEntities.forEach(bu -> {
                try {
                    resourceServiceClient.updateStatusByBookingUnit(bu.getId(), "CANCELLED");
                } catch (Exception ex) {
                }
            });
            throw new RuntimeException("Помилка сервісу: алокації відкочено. " + e.getMessage());
        }
    }

    public void syncParentGeneralBookingStatus(GeneralBooking generalBooking) {
        List<BookingUnit> units = generalBooking.getBookingUnits();
        if (units == null || units.isEmpty())
            return;

        boolean hasPending = false;
        boolean hasConfirmed = false;
        boolean hasCompleted = false;
        boolean allTerminal = true;
        boolean anyClientCancel = false;

        for (BookingUnit unit : units) {
            BookingStatus s = unit.getStatus();
            if (s == BookingStatus.PENDING)
                hasPending = true;
            if (s == BookingStatus.CONFIRMED)
                hasConfirmed = true;
            if (s == BookingStatus.COMPLETED)
                hasCompleted = true;
            if (s == BookingStatus.CANCELLED_BY_CLIENT)
                anyClientCancel = true;

            if (!isTerminal(s)) {
                allTerminal = false;
            }
        }

        //  Якщо всі юніти скасовані/завершені - замовлення закривається
        if (allTerminal) {
            if (hasCompleted) {
                generalBooking.setStatus(BookingStatus.COMPLETED);
            } else if (anyClientCancel) {
                generalBooking.setStatus(BookingStatus.CANCELLED_BY_CLIENT);
            } else {
                generalBooking.setStatus(BookingStatus.REJECTED);
            }
        }
        // Якщо є хоча б один PENDING - все замовлення PENDING
        else if (hasPending) {
            generalBooking.setStatus(BookingStatus.PENDING);
        }
        // Якщо немає PENDING і є активні - CONFIRMED
        else {
            generalBooking.setStatus(BookingStatus.CONFIRMED);
        }

        gBookingRepository.save(generalBooking);
    }

    @Transactional
    @PreAuthorize("@securityCheck.isBookingUnitOwner(#unitId, principal) or hasRole('ADMIN')")
    public BookingUnitDto updateBookingUnitStatusOnly(Long unitId, BookingStatus newStatus, AuthPrincipal principal) {
        BookingUnit existing = bookingRepository.findById(unitId)
                .orElseThrow(() -> new EntityNotFoundException("Послугу не знайдено"));

        BookingStatus oldStatus = existing.getStatus();
        boolean isAdmin = "ADMIN".equals(principal.getRole());

        validateStateTransition(oldStatus, newStatus, isAdmin);

        if (newStatus == BookingStatus.CANCELLED_BY_CLIENT || newStatus == BookingStatus.REJECTED) {
            Long bookingUserId = existing.getGeneralBooking().getUserId();
            
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

        existing.setStatus(newStatus);

        
        if (oldStatus == BookingStatus.PENDING && newStatus == BookingStatus.CONFIRMED) {
            AllocationResourceDto allocDto = new AllocationResourceDto();
            allocDto.setBookingUnitId(existing.getId());
            allocDto.setServiceUnitId(existing.getServiceUnit().getId());
            allocDto.setGeneralBookingId(existing.getGeneralBooking().getId());
            allocDto.setStart(existing.getStart());
            allocDto.setEnd(existing.getEnd());
            allocDto.setClientCount(existing.getClientCount());
            allocDto.setRequiresWorker(existing instanceof BookingSpaUnit);

            if (existing instanceof BookingSpaUnit spaUnit && spaUnit.getPreferedGender() != null) {
                allocDto.setPreferedGender(spaUnit.getPreferedGender());
            }

            try {
                resourceServiceClient.allocateResources(List.of(allocDto));
                
            } catch (FeignException e) {
                throw new IllegalStateException("Не вдалося підтвердити послугу: немає вільних приміщень або майстрів на обраний час.");
            }
        }
        // Логіка скасування для вже підтвердженої послуги
        else if (isTerminalCancellation(newStatus) && oldStatus == BookingStatus.CONFIRMED) {
            resourceServiceClient.updateStatusByBookingUnit(existing.getId(), "CANCELLED");
        } 
        // Логіка успішного завершення
        else if (newStatus == BookingStatus.COMPLETED && oldStatus == BookingStatus.CONFIRMED) {
            resourceServiceClient.updateStatusByBookingUnit(existing.getId(), "COMPLETED");
            
            LocalDateTime now = LocalDateTime.now();
            if (now.isBefore(existing.getEnd())) {
                existing.setEnd(now);
                resourceServiceClient.updateAllocationEndTime(existing.getId(), now);
            }
        }

        bookingRepository.save(existing);

        syncParentGeneralBookingStatus(existing.getGeneralBooking());

        return bookingMapper.toPolymorphicDto(existing);
    }
    
    private void validateStateTransition(BookingStatus oldStatus, BookingStatus newStatus, boolean isAdmin) {
        if (oldStatus == newStatus)
            return;

        if (isTerminal(oldStatus)) {
            throw new IllegalStateException("Неможливо змінити статус: послуга вже " + oldStatus);
        }

        if (newStatus == BookingStatus.EXPIRED) {
            throw new IllegalArgumentException("Статус EXPIRED встановлюється виключно автоматично системою, а не через API.");
        }

        if (!isAdmin && newStatus != BookingStatus.CANCELLED_BY_CLIENT) {
            throw new AccessDeniedException("Клієнт може лише скасувати послугу (CANCELLED_BY_CLIENT).");
        }

        if (oldStatus == BookingStatus.PENDING && newStatus == BookingStatus.COMPLETED) {
            throw new IllegalStateException("Неможливо завершити (COMPLETED) послугу, яка не була підтверджена.");
        }

        if (oldStatus == BookingStatus.CONFIRMED && newStatus == BookingStatus.PENDING) {
            throw new IllegalStateException("Неможливо повернути підтверджену послугу назад у статус PENDING.");
        }
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')") // Зміна дат уже підтверджених ресурсів — це сувора адмінська дія
    public BookingUnitDto updateBookingUnitDates(Long unitId, LocalDateTime newStart, LocalDateTime newEnd) {
        BookingUnit existing = bookingRepository.findById(unitId)
                .orElseThrow(() -> new EntityNotFoundException("Послугу не знайдено"));

        if (existing.getStatus() != BookingStatus.CONFIRMED) {
            throw new IllegalStateException("Зміна часу дозволена тільки для послуг зі статусом CONFIRMED.");
        }

        try {
            resourceServiceClient.updateAllocationDates(unitId, newStart, newEnd);
        } catch (FeignException e) {
            throw new IllegalStateException("Конфлікт розкладу: обраний час перетинається з іншими бронями.");
        }

        // Якщо мікросервіс алокацій підтвердив зміну — фіксуємо в базі готелю
        existing.setStart(newStart);
        existing.setEnd(newEnd);
        
        return bookingMapper.toPolymorphicDto(bookingRepository.save(existing));
    }

    private boolean isTerminal(BookingStatus status) {
        return status == BookingStatus.COMPLETED || isTerminalCancellation(status);
    }

    private boolean isTerminalCancellation(BookingStatus status) {
        return status == BookingStatus.CANCELLED_BY_CLIENT ||
                status == BookingStatus.REJECTED ||
                status == BookingStatus.EXPIRED;
    }

}

