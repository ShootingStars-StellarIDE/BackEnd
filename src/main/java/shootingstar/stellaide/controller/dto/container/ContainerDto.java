package shootingstar.stellaide.controller.dto.container;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Data;
import shootingstar.stellaide.entity.container.ContainerType;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class ContainerDto {
    private UUID containerId;
    private ContainerType type;
    private String name;
    private String description;
    private String editUserNickname;
    private LocalDateTime createdTime;
    private LocalDateTime lastModifiedTime;

    @QueryProjection
    public ContainerDto(UUID containerId, ContainerType type, String name, String description, LocalDateTime createdTime, LocalDateTime lastModifiedTime,
                        String editUserNickname) {
        this.containerId = containerId;
        this.type = type;
        this.name = name;
        this.description = description;
        this.createdTime = createdTime;
        this.lastModifiedTime = lastModifiedTime;
        this.editUserNickname = editUserNickname;
    }
}
