package edu.ap.gosmartlib.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UserRolesFromOneRosterTest {

    @Test
    void givenStudent_whenFromOneRoster_thenReturnsStudent() {
        assertEquals(UserRoles.STUDENT, UserRoles.fromOneRoster("student"));
    }

    @Test
    void givenStudentUppercase_whenFromOneRoster_thenReturnsStudent() {
        assertEquals(UserRoles.STUDENT, UserRoles.fromOneRoster("STUDENT"));
    }

    @Test
    void givenTeacher_whenFromOneRoster_thenReturnsTeacher() {
        assertEquals(UserRoles.TEACHER, UserRoles.fromOneRoster("teacher"));
    }

    @Test
    void givenNull_whenFromOneRoster_thenReturnsOther() {
        assertEquals(UserRoles.OTHER, UserRoles.fromOneRoster(null));
    }
}
