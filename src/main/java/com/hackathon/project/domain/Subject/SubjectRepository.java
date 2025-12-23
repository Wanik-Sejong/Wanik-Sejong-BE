package com.hackathon.project.domain.Subject;

import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubjectRepository extends JpaRepository<Subject, Long> {

    @Query("select s from Subject s where s.id in "
        + "(select min(s2.id) from Subject s2 group by s2.courseName)")
    List<Subject> findDistinctByCourseName();
}
