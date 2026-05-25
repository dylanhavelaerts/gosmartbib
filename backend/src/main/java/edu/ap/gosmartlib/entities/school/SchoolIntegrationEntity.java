<<<<<<<< HEAD:backend/src/main/java/edu/ap/gosmartlib/entities/schoolEntities/SchoolIntegrationEntity.java
package edu.ap.gosmartlib.entities.schoolEntities;
========
package edu.ap.gosmartlib.entities.school;
>>>>>>>> 4f936deee088529c0230b4241700c7fa8a61f9ca:backend/src/main/java/edu/ap/gosmartlib/entities/school/SchoolIntegrationEntity.java

import edu.ap.gosmartlib.security.SecretCryptoConverter;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "tblSchoolIntegrations")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = "onerosterClientSecret")
public class SchoolIntegrationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "school_id", nullable = false, unique = true)
    private SchoolEntity school;

    @Column(name = "school_base_url", nullable = false, length = 255)
    private String schoolBaseUrl;

    @Column(name = "oneroster_client_id", nullable = false, length = 255)
    private String onerosterClientId;

    @Convert(converter = SecretCryptoConverter.class)
    @Column(name = "oneroster_client_secret", nullable = false, length = 4000)
    private String onerosterClientSecret;

    @Column(name = "oneroster_enabled", nullable = false)
    private boolean onerosterEnabled = false;

    @Column(name = "last_test_successful_at")
    private LocalDateTime lastTestSuccessfulAt;

    @Column(name = "last_sync_at")
    private LocalDateTime lastSyncAt;

    @Column(name = "last_error", length = 2000)
    private String lastError;

    @Column(name = "smartschool_accesscode", length = 4000)
    @Convert(converter = SecretCryptoConverter.class)
    private String smartschoolAccesscode;
    @Column(name = "smartschool_sender_identifier", length = 4000)
    @Convert(converter = SecretCryptoConverter.class)
    private String smartschoolSenderIdentifier;
}