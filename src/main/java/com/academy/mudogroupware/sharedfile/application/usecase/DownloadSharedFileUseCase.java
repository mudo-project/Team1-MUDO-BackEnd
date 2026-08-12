package com.academy.mudogroupware.sharedfile.application.usecase;

import com.academy.mudogroupware.sharedfile.application.port.DriveBinary;
import com.academy.mudogroupware.sharedfile.application.query.ExportTargetFormat;

public interface DownloadSharedFileUseCase {

    DriveBinary download(String itemId, ExportTargetFormat format);
}
