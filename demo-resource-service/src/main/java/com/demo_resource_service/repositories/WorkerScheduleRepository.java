package com.demo_resource_service.repositories;

import java.time.DayOfWeek;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.demo_resource_service.data.models.WorkerSchedule;

public interface WorkerScheduleRepository extends JpaRepository<WorkerSchedule, Long> {
    List<WorkerSchedule> findByWorkerIdOrderByDayOfWeekAsc(Long workerId);
    void deleteByWorkerIdAndDayOfWeek(long workerId, DayOfWeek dayOfWeek);

    List<WorkerSchedule> findByWorkerIdOrderByDayOfWeekAsc(long workerId);
}