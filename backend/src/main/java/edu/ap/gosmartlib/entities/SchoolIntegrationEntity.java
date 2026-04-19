package edu.ap.gosmartlib.entities;

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

    @Column(name = "oneroster_base_url", nullable = false, length = 255)
    private String onerosterBaseUrl;

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
}