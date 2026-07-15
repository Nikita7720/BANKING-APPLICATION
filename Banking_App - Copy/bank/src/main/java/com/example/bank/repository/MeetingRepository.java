package com.example.bank.repository;

import com.example.bank.entity.Meeting;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MeetingRepository extends JpaRepository<Meeting, Long> {
    List<Meeting> findAllByOrderByMeetingDateDescMeetingTimeDesc();
    List<Meeting> findByMeetingDateAfterOrderByMeetingDateAscMeetingTimeAsc(java.time.LocalDate date);
}
