package com.academy.mudogroupware.attendance.application.usecase;

import com.academy.mudogroupware.attendance.application.command.RegisterWifiIpCommand;
import com.academy.mudogroupware.attendance.application.result.RegisterWifiIpResult;

public interface RegisterWifiIpUseCase {
    RegisterWifiIpResult register(RegisterWifiIpCommand command);
}
