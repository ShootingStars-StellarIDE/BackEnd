package shootingstar.stellaide.entity.container;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import shootingstar.stellaide.entity.BaseTimeTimeEntity;
import shootingstar.stellaide.entity.SharedUserContainer;
import shootingstar.stellaide.entity.chat.ContainerChatRoom;
import shootingstar.stellaide.entity.user.User;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor
public class Container extends BaseTimeTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "BINARY(16)")
    private UUID containerId;

    @NotNull
    @Enumerated(EnumType.STRING)
    private ContainerType type;

    @NotBlank
    private String name;

    @NotBlank
    private String description;

    private String editUserNickname;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;

    @OneToMany(mappedBy = "container", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private final List<SharedUserContainer> sharedUsers = new ArrayList<>();

    @OneToOne(mappedBy = "container", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private ContainerChatRoom containerChatRoom;

    public Container(ContainerType type, String name, String description, User owner) {
        this.type = type;
        this.name = name;
        this.description = description;
        this.owner = owner;
        this.editUserNickname = owner.getNickname();
    }

    public void addSharedUser(SharedUserContainer userContainer) {
        this.sharedUsers.add(userContainer);
    }

    public void changeDescription(String description) {
        this.description = description;
    }
    public void changeEditUserNickname(String editUserNickname) {
        this.editUserNickname = editUserNickname;
    }
}
