package co.cetad.umas.operation.domain.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.domain.Persistable;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Setter
@Getter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "kml_route")
public class KmlRouteEntity implements Serializable, Persistable<UUID> {

    @Id
    private UUID id = UUID.randomUUID();

    @Column(name = "name")
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private RouteStatus status;

    @Column(name = "original_filename")
    private String originalFilename;

    @Column(name = "kml_content")
    private String kmlContent;

    @Column(name = "geojson")
    private String geojson;

    @Column(name = "geom")
    private String geom;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Transient
    private boolean isNew;

    @Override
    public boolean isNew() {
        return isNew;
    }

}