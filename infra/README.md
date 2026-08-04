# MUDO 인프라 배포 파일

## 브랜치별 배포 경계

- `develop`: 기존 GitHub Actions가 테스트 후 Docker Hub `mudo-groupware-backend:develop` 이미지를 게시한다. 프론트 로컬 연동 흐름을 유지한다.
- `main`: 운영 이미지를 ECR에 게시하고 활성 학원을 ECS에 순차 배포한다.

## 운영 배포 안전장치

현재 운영 배포는 두 단계로 잠겨 있다.

1. GitHub Repository variable `PRODUCTION_DEPLOY_ENABLED`가 `true`여야 한다.
2. `infra/tenants.yml`에서 준비가 끝난 학원만 `enabled: true`여야 한다.

AWS 리소스가 준비되지 않은 상태에서 이 값을 임의로 변경하지 않는다.

## 매니페스트

- `runtime-profiles.yml`: 결제 플랜과 분리된 공통 ECS·Tomcat·Hikari·비동기 작업 상한
- `cells.yml`: ECS/RDS Cell과 실제 수용 가능량
- `tenants.yml`: 학원별 결제 플랜 메타데이터·런타임 프로필·Cell·ECS Service·Health URL

`billing_plan`은 유료 엔드포인트·기능·쿼터를 나타내며 CPU·메모리·스레드·DB Pool을 결정하지 않는다. 모든 학원은 기본적으로 `shared-default` 런타임 프로필을 사용한다.

검증:

```bash
python -m pip install -r infra/requirements.txt
python infra/scripts/test_deploy_production.py
python infra/scripts/deploy_production.py --validate-only
```

## 운영 활성화 전에 필요한 AWS 리소스

- ECR `mudo-groupware-backend`
- GitHub OIDC Provider와 `mudo-prod-ci-deploy-role`
- ECS Cluster, Capacity Provider와 학원별 ECS Service
- `/mudo/prod/migration` CloudWatch Log Group
- 학원별 app/migrator DB 계정
- 학원별 SSM DB/JWT/CORS Parameter
- 학원별 S3 Prefix 권한과 Task Role
- ALB Target Group, Host 규칙과 Health Check

실제 AWS 값은 `cells.yml`, `tenants.yml`과 GitHub `production` Environment에 기록하며 시크릿 값 자체는 Git에 넣지 않는다.
