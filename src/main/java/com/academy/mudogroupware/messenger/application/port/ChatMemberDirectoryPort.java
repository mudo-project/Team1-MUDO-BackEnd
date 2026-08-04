package com.academy.mudogroupware.messenger.application.port;

import java.util.List;
import java.util.Map;

public interface ChatMemberDirectoryPort {

    ChatMemberInfo getMember(Long userId);

    Map<Long, ChatMemberInfo> getMembers(List<Long> userIds);
}
