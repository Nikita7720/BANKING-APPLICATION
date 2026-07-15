package com.example.bank.repository;

import com.example.bank.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    List<Attendance> findByStaffId(Long staffId);
    List<Attendance> findByAttendanceDate(LocalDate date);
    java.util.Optional<Attendance> findFirstByStaffIdAndAttendanceDateOrderByCheckInTimeDesc(Long staffId, LocalDate date);
}
