package edu.ap.gosmartlib.entities.book;

import edu.ap.gosmartlib.util.BookCopyCondition;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter @Setter
@NoArgsConstructor
@Table(name = "tblBookCopies")
public class BookCopyEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inventory_id", nullable = false)
    private BookInventoryEntity inventory;

    // Barcode kan nullable zijn in geval van scholen die geen barcodes willen
    @Column(unique = true)
    private String barcode;

    @Enumerated(EnumType.STRING)
    @Column(name = "copy_condition")
    private BookCopyCondition copyCondition = BookCopyCondition.GOOD;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(nullable = false)
    private Integer copyNumber;
}
