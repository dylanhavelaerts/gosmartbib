package edu.ap.gosmartlib.entities;

import jakarta.persistence.*;
import lombok.*;
import edu.ap.gosmartlib.security.SecretCryptoConverter;

@Entity
@Table(name = "school_integrations")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
public class SchoolIntegrationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "school_id", nullable = false, unique = true)
    private SchoolEntity school;

    private String onerosterBaseUrl;
    private String onerosterClientId;

    @Convert(converter = SecretCryptoConverter.class)
    private String onerosterClientSecret;

    private boolean onerosterEnabled;
}