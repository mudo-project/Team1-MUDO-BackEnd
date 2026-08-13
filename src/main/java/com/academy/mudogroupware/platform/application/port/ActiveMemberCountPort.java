package com.academy.mudogroupware.platform.application.port;

/** 플랫폼 대시보드가 현재 학원 DB의 활성 회원 수를 읽기 위한 포트다. */
public interface ActiveMemberCountPort {
  long countActiveMembers();
}
