package com.academy.mudogroupware.file.application.port;

import java.util.Collection;
import java.util.Set;

public interface FileReferenceChecker {

    Set<Long> findReferencedFileIds(Collection<Long> fileIds);
}
