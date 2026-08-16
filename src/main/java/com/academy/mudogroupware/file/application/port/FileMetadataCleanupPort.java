package com.academy.mudogroupware.file.application.port;

import java.util.List;

public interface FileMetadataCleanupPort {

    List<FileMetadataCleanupTarget> findAllByIds(List<Long> fileIds);

    void deleteById(Long fileId);
}
