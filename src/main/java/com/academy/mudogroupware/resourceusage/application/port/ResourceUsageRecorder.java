package com.academy.mudogroupware.resourceusage.application.port;

import com.academy.mudogroupware.resourceusage.application.command.RecordAiTokenUsageCommand;
import com.academy.mudogroupware.resourceusage.application.command.RecordMailUsageCommand;
import com.academy.mudogroupware.resourceusage.application.command.RecordS3StorageUsageCommand;
import com.academy.mudogroupware.resourceusage.application.command.RecordSmsUsageCommand;

public interface ResourceUsageRecorder {

    void recordAiTokens(RecordAiTokenUsageCommand command);

    void recordSmsMessages(RecordSmsUsageCommand command);

    void recordS3Storage(RecordS3StorageUsageCommand command);

    void recordMailUsage(RecordMailUsageCommand command);
}
