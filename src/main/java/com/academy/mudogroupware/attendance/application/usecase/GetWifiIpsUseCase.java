package com.academy.mudogroupware.attendance.application.usecase;

import java.util.List;

import com.academy.mudogroupware.attendance.domain.model.AcademyWifiIp;

public interface GetWifiIpsUseCase {

    List<AcademyWifiIp> getAll(Long requesterId);
}
