package edu.ap.gosmartlib.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tblSchools")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
public class SchoolEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false)
    private String name;

    // Zelfe scholendomein????
    @Column(nullable = false, unique = true)
    private String domain; // !!! "go-antwerpen.smartschool.be" of "school1.smartschool.be"

    // Als scholen niet in dezelfde groep zitten, kunnen ze verschillende
    // client_id's en client_secrets hebben.
    // Daarom moeten we deze per school opslaan.
    // @Column(name = "oauth_client_id")
    // private String oauthClientId;
    //
    // @Column(name = "oauth_client_secret")
    // private String oauthClientSecret;
}
