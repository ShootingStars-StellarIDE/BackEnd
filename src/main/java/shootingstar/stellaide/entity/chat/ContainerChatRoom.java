package shootingstar.stellaide.entity.chat;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import shootingstar.stellaide.entity.container.Container;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ContainerChatRoom {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long chatRoomId;

    private String chatRoomName;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "container_id")
    private Container container;

    @OneToMany(mappedBy = "containerChatRoom", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private final List<ContainerChatRoomMessage> messageList = new ArrayList<>();

    @Builder
    public ContainerChatRoom(Container container, String chatRoomName) {
        this.container = container;
        this.chatRoomName = chatRoomName;
    }

    public void addChatMessage(ContainerChatRoomMessage message) {
        this.messageList.add(message);
    }
}