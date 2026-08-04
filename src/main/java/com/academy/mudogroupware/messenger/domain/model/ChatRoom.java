package com.academy.mudogroupware.messenger.domain.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.academy.mudogroupware.messenger.domain.exception.MessengerErrorCode;
import com.academy.mudogroupware.messenger.domain.exception.MessengerException;

public final class ChatRoom {

    private final Long id;
    private final Long academyId;
    private String name;
    private final ChatRoomType type;
    private final Long createdBy;
    private final List<ChatRoomMember> members;
    private final LocalDateTime createdAt;

    private ChatRoom(Long id, Long academyId, String name, ChatRoomType type, Long createdBy,
                      List<ChatRoomMember> members, LocalDateTime createdAt) {
        if (academyId == null) {
            throw new IllegalArgumentException("academyId must not be null");
        }
        if (createdBy == null) {
            throw new IllegalArgumentException("createdBy must not be null");
        }
        if (members == null || members.isEmpty()) {
            throw new MessengerException(MessengerErrorCode.PARTICIPANT_REQUIRED);
        }
        this.id = id;
        this.academyId = academyId;
        this.name = name;
        this.type = type;
        this.createdBy = createdBy;
        this.members = new ArrayList<>(members);
        this.createdAt = createdAt;
    }

    public static ChatRoom create(Long academyId, Long createdBy, List<Long> participantIds, String name) {
        if (participantIds == null || participantIds.isEmpty()) {
            throw new MessengerException(MessengerErrorCode.INVITEE_REQUIRED);
        }

        Set<Long> inviteeIds = new LinkedHashSet<>(participantIds);
        inviteeIds.remove(createdBy);
        if (inviteeIds.isEmpty()) {
            throw new MessengerException(MessengerErrorCode.SELF_INVITE_ONLY);
        }

        ChatRoomType type = inviteeIds.size() == 1 ? ChatRoomType.DM : ChatRoomType.GROUP;
        String roomName = name;
        if (type == ChatRoomType.GROUP) {
            if (roomName == null || roomName.isBlank()) {
                throw new MessengerException(MessengerErrorCode.GROUP_NAME_REQUIRED);
            }
        } else {
            roomName = null;
        }

        List<ChatRoomMember> members = new ArrayList<>();
        members.add(ChatRoomMember.create(createdBy));
        inviteeIds.forEach(userId -> members.add(ChatRoomMember.create(userId)));

        return new ChatRoom(null, academyId, roomName, type, createdBy, members, LocalDateTime.now());
    }

    public static ChatRoom restore(Long id, Long academyId, String name, ChatRoomType type, Long createdBy,
                                    List<ChatRoomMember> members, LocalDateTime createdAt) {
        return new ChatRoom(id, academyId, name, type, createdBy, members, createdAt);
    }

    public boolean isMember(Long userId) {
        return members.stream().anyMatch(member -> member.getUserId().equals(userId));
    }

    public Optional<ChatRoomMember> findMember(Long userId) {
        return members.stream().filter(member -> member.getUserId().equals(userId)).findFirst();
    }

    public void markRead(Long userId, LocalDateTime readAt) {
        ChatRoomMember member = findMember(userId)
                .orElseThrow(() -> new MessengerException(MessengerErrorCode.NOT_ROOM_MEMBER));
        member.markRead(readAt);
    }

    public Long getId() {
        return id;
    }

    public Long getAcademyId() {
        return academyId;
    }

    public String getName() {
        return name;
    }

    public ChatRoomType getType() {
        return type;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public List<ChatRoomMember> getMembers() {
        return Collections.unmodifiableList(members);
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
