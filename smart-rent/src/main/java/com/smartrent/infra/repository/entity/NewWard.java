import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity(name = "new_wards")
@Table(
        name = "new_wards",
        indexes = {
                @Index(name = "idx_new_wards_province_id", columnList = "province_id"),
                @Index(name = "idx_new_wards_name", columnList = "name"),
                @Index(name = "idx_new_wards_is_active", columnList = "is_active"),
                @Index(name = "idx_effective_period", columnList = "effective_from, effective_to")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "unique_province_new_ward_code", columnNames = {"province_id", "code"})
        }
)
public class NewWard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer newWardId;

    @Column(nullable = false, length = 100)
    String name;

    @Column(length = 10)
    String code;

    @Column(nullable = false, length = 20)
    String type;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "province_id", nullable = false)
    Province province;

    @Column(name = "is_active", nullable = false)
    Boolean isActive = true;

    @Column(name = "effective_from", nullable = false)
    LocalDate effectiveFrom;

    @Column(name = "effective_to")
    LocalDate effectiveTo;

    @OneToMany(mappedBy = "newWard", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    List<Ward> mergedWards;

    @Column(name = "created_at", nullable = false, updatable = false)
    LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

}
