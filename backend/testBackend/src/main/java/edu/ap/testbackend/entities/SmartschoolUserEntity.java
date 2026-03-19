package edu.ap.testbackend.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "tblSmartschoolUsers")
public class SmartschoolUserEntity {

    // We gebruiken het unieke ID van Smartschool als Primary Key
    @Id
    @Column(nullable = false, unique = true)
    private String smartschoolUserId;

    @Column(nullable = false)
    private String classGroup;

    @Column(nullable = false)
    private String school;

    @Column(nullable = false)
    private String schoolId;

    public SmartschoolUserEntity() {
    }

    public SmartschoolUserEntity(String smartschoolUserId, String classGroup, String school, String schoolId) {
        this.smartschoolUserId = smartschoolUserId;
        this.classGroup = classGroup;
        this.school = school;
        this.schoolId = schoolId;
    }
}