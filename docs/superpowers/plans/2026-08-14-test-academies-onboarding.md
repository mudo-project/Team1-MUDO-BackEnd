# 테스트 학원(academy-b, academy-c) 온보딩 리허설 구현 계획

> **For agentic workers:** 이 계획은 코드/테스트 작업이 아니라 **운영 AWS 인프라에 실 리소스를 만드는 작업**이다. 프로덕션 블라스트 반경이 크므로 **superpowers:executing-plans(인라인 실행, 사람 체크포인트)를 권장**하며, subagent-driven-development는 권장하지 않는다. 각 태스크는 실행 후 반드시 AWS CLI로 재조회해서 실제로 반영됐는지 확인한다("검증" = "테스트" 역할).

**Goal:** `academy-b`, `academy-c` 테넌트를 실제 운영 환경에 온보딩해서 ECS 바인팩 배치, `runtime-profiles.yml` 설정 반영, S3/RDS 테넌트 격리가 실제로 동작하는지 검증한다.

**Architecture:** `docs/2026-08-03-academy-groupware-multi-tenant-aws-infra-guide.md` 22장 온보딩 절차를 따르되 Google Drive(10단계)는 제외한다. 설계 근거는 `docs/superpowers/specs/2026-08-14-test-academies-onboarding-design.md` 참고.

**Tech Stack:** AWS CLI (`--profile mudo-team04`), MySQL 8, `infra/tenants.yml`/`cells.yml` (YAML), `infra/scripts/deploy_production.py`.

**공통 변수** (아래 모든 명령에서 사용):

```bash
ACCOUNT_ID=635249349258
REGION=ap-northeast-2
CLUSTER=mudo-prod-cluster
CP=mudo-prod-capacity-provider
RDS_ENDPOINT=mudo-prod-rds-cell-1.c3a84ssqgmc2.ap-northeast-2.rds.amazonaws.com
VPC_ID=vpc-09ce24ff61fee7153
ALB_LISTENER_ARN=arn:aws:elasticloadbalancing:ap-northeast-2:635249349258:listener/app/mudo-prod-alb/977e26c3d0b010fd
AWSCLI="aws --profile mudo-team04 --region ap-northeast-2"
```

Windows Git Bash에서 `/`로 시작하는 인자(SSM 파라미터 이름 등)를 쓸 때는 항상 `MSYS_NO_PATHCONV=1`을 앞에 붙인다(경로 자동 변환 방지).

---

### Task 1: `cells.yml` 용량 값 실측 반영

**Files:**
- Modify: `infra/cells.yml`

- [ ] **Step 1: 현재 라이브 용량 재확인**

```bash
$AWSCLI ecs describe-container-instances --cluster $CLUSTER \
  --container-instances $($AWSCLI ecs list-container-instances --cluster $CLUSTER --query "containerInstanceArns" --output text) \
  --query "containerInstances[].{id:ec2InstanceId,cpu:registeredResources[?name=='CPU'].integerValue|[0],mem:registeredResources[?name=='MEMORY'].integerValue|[0]}"
```

기대 출력: 인스턴스 2대, 각 cpu 2048 / mem 1913 (합산 4096/3826).

- [ ] **Step 2: `cells.yml` 갱신**

```yaml
cells:
  cell-1:
    ecs_cluster: mudo-prod-cluster
    capacity_provider: mudo-prod-capacity-provider
    rds_identifier: mudo-prod-rds-cell-1
    rds_app_connection_ratio: 0.70
    ecs_app_capacity_ratio: 0.80

    # 2026-08-14 실측: 라이브 ASG(mudo-prod-asg) 인스턴스 2대 기준으로 갱신.
    # 인스턴스 1대당 cpu 2048 / mem 1913 (t3.small) x 2대.
    ecs_registered_cpu: 4096
    ecs_registered_memory_mib: 3826
    rds_max_connections: 144
```

- [ ] **Step 3: 검증**

```bash
cd "C:/lecture/module06/Team1-MUDO-BackEnd"
python infra/scripts/deploy_production.py --validate-only
```

기대: `매니페스트 검증 완료: 활성 tenant 1개` (아직 b/c 추가 전이므로 1개가 정상).

- [ ] **Step 4: 커밋**

```bash
git add infra/cells.yml
git commit -m "chore: cells.yml ECS 등록 용량을 라이브 인스턴스 2대 기준으로 갱신"
```

---

### Task 2: finance 버킷 생성

**Files:** 없음 (AWS 리소스만 생성)

- [ ] **Step 1: 버킷 생성**

```bash
$AWSCLI s3api create-bucket --bucket mudo-prod-finance-635249349258 \
  --create-bucket-configuration LocationConstraint=$REGION
```

- [ ] **Step 2: Public access 차단**

```bash
$AWSCLI s3api put-public-access-block --bucket mudo-prod-finance-635249349258 \
  --public-access-block-configuration BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true
```

- [ ] **Step 3: Versioning 활성화**

```bash
$AWSCLI s3api put-bucket-versioning --bucket mudo-prod-finance-635249349258 \
  --versioning-configuration Status=Enabled
```

- [ ] **Step 4: 기본 암호화(SSE-S3) 활성화**

```bash
$AWSCLI s3api put-bucket-encryption --bucket mudo-prod-finance-635249349258 \
  --server-side-encryption-configuration '{"Rules":[{"ApplyServerSideEncryptionByDefault":{"SSEAlgorithm":"AES256"}}]}'
```

- [ ] **Step 5: HTTPS-only 버킷 정책**

```bash
$AWSCLI s3api put-bucket-policy --bucket mudo-prod-finance-635249349258 --policy '{
  "Version": "2012-10-17",
  "Statement": [{
    "Sid": "DenyInsecureTransport",
    "Effect": "Deny",
    "Principal": "*",
    "Action": "s3:*",
    "Resource": ["arn:aws:s3:::mudo-prod-finance-635249349258", "arn:aws:s3:::mudo-prod-finance-635249349258/*"],
    "Condition": {"Bool": {"aws:SecureTransport": "false"}}
  }]
}'
```

- [ ] **Step 6: 검증**

```bash
$AWSCLI s3api get-bucket-versioning --bucket mudo-prod-finance-635249349258
$AWSCLI s3api get-public-access-block --bucket mudo-prod-finance-635249349258
$AWSCLI s3api get-bucket-encryption --bucket mudo-prod-finance-635249349258
```

기대: Versioning `Enabled`, 4개 항목 모두 `true`, SSE `AES256`.

---

### Task 3: `tenants.yml`에 academy-b, academy-c 추가 (enabled: false)

**Files:**
- Modify: `infra/tenants.yml`

- [ ] **Step 1: 두 테넌트를 `enabled: false`로 추가**

```yaml
tenants:
  - code: academy-a
    enabled: true
    platform_dashboard_host: true
    billing_plan: basic
    runtime_profile: shared-default
    cell: cell-1
    service: mudo-prod-svc-academy-a
    task_family: mudo-prod-tenant-academy-a
    health_url: https://academy-a.ieum.store/actuator/health
    s3_bucket: mudo-prod-staff-635249349258
    finance_s3_bucket: mudo-prod-finance-635249349258
  - code: academy-b
    # 2026-08-14 온보딩 리허설. AWS 리소스 준비 완료 후 enabled: true로 전환(Task 10).
    enabled: false
    billing_plan: basic
    runtime_profile: shared-default
    cell: cell-1
    service: mudo-prod-svc-academy-b
    task_family: mudo-prod-tenant-academy-b
    health_url: https://academy-b.ieum.store/actuator/health
    s3_bucket: mudo-prod-staff-635249349258
    finance_s3_bucket: mudo-prod-finance-635249349258
  - code: academy-c
    enabled: false
    billing_plan: basic
    runtime_profile: shared-default
    cell: cell-1
    service: mudo-prod-svc-academy-c
    task_family: mudo-prod-tenant-academy-c
    health_url: https://academy-c.ieum.store/actuator/health
    s3_bucket: mudo-prod-staff-635249349258
    finance_s3_bucket: mudo-prod-finance-635249349258
```

- [ ] **Step 2: 검증**

```bash
python infra/scripts/deploy_production.py --validate-only
```

기대: `활성 tenant 1개` (b/c는 `enabled: false`라 카운트 안 됨, 스키마 오류 없이 통과해야 함).

- [ ] **Step 3: 커밋**

```bash
git add infra/tenants.yml
git commit -m "feat: academy-b, academy-c 테넌트 매니페스트 추가(enabled: false)"
```

---

### Task 4: RDS DB·계정 생성 — academy-b, academy-c

**Files:** 없음 (RDS 데이터만 생성)

- [ ] **Step 1: SSM 포트포워딩으로 RDS 접속 터널 연다** (별도 터미널에서 백그라운드로 유지)

```bash
$AWSCLI ssm start-session --target i-0e2ddcd93faa9ae6d \
  --document-name AWS-StartPortForwardingSessionToRemoteHost \
  --parameters "{\"host\":[\"$RDS_ENDPOINT\"],\"portNumber\":[\"3306\"],\"localPortNumber\":[\"13306\"]}"
```

- [ ] **Step 2: 강한 비밀번호 4개 생성 (academy-b/c × app/migrator)**

```bash
B_APP_PW=$(openssl rand -base64 24) 
B_MIG_PW=$(openssl rand -base64 24)
C_APP_PW=$(openssl rand -base64 24)
C_MIG_PW=$(openssl rand -base64 24)
```

- [ ] **Step 3: DB·계정 생성 (마스터 계정으로, `mudo_admin` 비밀번호는 안전한 채널에서 가져온다)**

```bash
mysql -h 127.0.0.1 -P 13306 -u mudo_admin -p <<SQL
CREATE DATABASE mudo_tenant_academy_b CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'academy_b_app'@'%' IDENTIFIED BY '$B_APP_PW';
GRANT SELECT, INSERT, UPDATE, DELETE ON mudo_tenant_academy_b.* TO 'academy_b_app'@'%';
ALTER USER 'academy_b_app'@'%' WITH MAX_USER_CONNECTIONS 10;
CREATE USER 'academy_b_migrator'@'%' IDENTIFIED BY '$B_MIG_PW';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, DROP, REFERENCES ON mudo_tenant_academy_b.* TO 'academy_b_migrator'@'%';

CREATE DATABASE mudo_tenant_academy_c CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'academy_c_app'@'%' IDENTIFIED BY '$C_APP_PW';
GRANT SELECT, INSERT, UPDATE, DELETE ON mudo_tenant_academy_c.* TO 'academy_c_app'@'%';
ALTER USER 'academy_c_app'@'%' WITH MAX_USER_CONNECTIONS 10;
CREATE USER 'academy_c_migrator'@'%' IDENTIFIED BY '$C_MIG_PW';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, DROP, REFERENCES ON mudo_tenant_academy_c.* TO 'academy_c_migrator'@'%';

FLUSH PRIVILEGES;
SQL
```

- [ ] **Step 4: 검증 — 다른 학원 DB 접근이 막히는지 확인 (이게 나중에 Task 13의 RDS 격리 검증과 동일한 방식)**

```bash
mysql -h 127.0.0.1 -P 13306 -u academy_b_app -p"$B_APP_PW" -e "USE mudo_tenant_academy_a;" 2>&1
```

기대: `ERROR 1044 (42000): Access denied for user 'academy_b_app'@'%' to database 'mudo_tenant_academy_a'`

- [ ] **Step 5: SSM 포트포워딩 세션 종료**

포트포워딩용 터미널에서 Ctrl+C.

---

### Task 5: SSM 파라미터 생성 — academy-b, academy-c

**Files:** 없음 (SSM Parameter Store만)

academy-a와 동일한 19개 키. 아래는 academy-b 예시(academy-c는 `b`→`c`, 위에서 만든 비밀번호로 치환해서 동일하게 반복).

- [ ] **Step 1: 계정별 신규 시크릿 생성**

```bash
B_JWT_SECRET=$(openssl rand -base64 48)
B_TOKEN_ENC_KEY=$(openssl rand -base64 32)
```

- [ ] **Step 2: SSM 파라미터 생성 (academy-b)**

```bash
P=/mudo/prod/tenants/academy-b
MSYS_NO_PATHCONV=1 $AWSCLI ssm put-parameter --name "$P/DB_URL" --type String \
  --value "jdbc:mysql://$RDS_ENDPOINT:3306/mudo_tenant_academy_b?useSSL=true&serverTimezone=Asia/Seoul&characterEncoding=UTF-8"
MSYS_NO_PATHCONV=1 $AWSCLI ssm put-parameter --name "$P/DB_USERNAME" --type String --value "academy_b_app"
MSYS_NO_PATHCONV=1 $AWSCLI ssm put-parameter --name "$P/DB_PASSWORD" --type SecureString --value "$B_APP_PW"
MSYS_NO_PATHCONV=1 $AWSCLI ssm put-parameter --name "$P/MIGRATOR_DB_USERNAME" --type String --value "academy_b_migrator"
MSYS_NO_PATHCONV=1 $AWSCLI ssm put-parameter --name "$P/MIGRATOR_DB_PASSWORD" --type SecureString --value "$B_MIG_PW"
MSYS_NO_PATHCONV=1 $AWSCLI ssm put-parameter --name "$P/JWT_SECRET" --type String --value "$B_JWT_SECRET"
MSYS_NO_PATHCONV=1 $AWSCLI ssm put-parameter --name "$P/APP_FRONTEND_URL" --type String --value "https://academy-b.ieum.store"
MSYS_NO_PATHCONV=1 $AWSCLI ssm put-parameter --name "$P/CORS_ALLOWED_ORIGINS" --type String --value "https://academy-b.ieum.store"
MSYS_NO_PATHCONV=1 $AWSCLI ssm put-parameter --name "$P/WEBSOCKET_ALLOWED_ORIGINS" --type String --value "https://academy-b.ieum.store"
MSYS_NO_PATHCONV=1 $AWSCLI ssm put-parameter --name "$P/GOOGLE_TOKEN_ENCRYPTION_KEY" --type String --value "$B_TOKEN_ENC_KEY"
MSYS_NO_PATHCONV=1 $AWSCLI ssm put-parameter --name "$P/GOOGLE_REDIRECT_URI" --type String \
  --value "https://academy-b.ieum.store/api/google/connections/callback"
MSYS_NO_PATHCONV=1 $AWSCLI ssm put-parameter --name "$P/GOOGLE_OAUTH_FRONTEND_REDIRECT_URI" --type String \
  --value "http://localhost:3000/setting/google"
MSYS_NO_PATHCONV=1 $AWSCLI ssm put-parameter --name "$P/MAIL_FROM" --type String --value "no-reply@mg.market-app.org"
```

- [ ] **Step 3: 계정 공용 시크릿(Google OAuth Client, Gemini, Mailgun) — academy-a 값을 그대로 복사** (값을 화면에 출력하지 않고 파이프로만 전달)

```bash
for NAME in GOOGLE_CLIENT_ID GOOGLE_CLIENT_SECRET GEMINI_API_KEY \
            MAILGUN_SMTP_USERNAME MAILGUN_SMTP_PASSWORD MAILGUN_WEBHOOK_SIGNING_KEY; do
  TYPE=$(MSYS_NO_PATHCONV=1 $AWSCLI ssm get-parameter --name "/mudo/prod/tenants/academy-a/$NAME" --with-decryption --query "Parameter.Type" --output text)
  VALUE=$(MSYS_NO_PATHCONV=1 $AWSCLI ssm get-parameter --name "/mudo/prod/tenants/academy-a/$NAME" --with-decryption --query "Parameter.Value" --output text)
  MSYS_NO_PATHCONV=1 $AWSCLI ssm put-parameter --name "/mudo/prod/tenants/academy-b/$NAME" --type "$TYPE" --value "$VALUE"
done
```

- [ ] **Step 4: academy-c도 Step 1~3을 동일하게 반복** (`b`→`c`, `$C_APP_PW`/`$C_MIG_PW` 사용)

- [ ] **Step 5: 검증 — 파라미터 개수 확인 (값은 확인하지 않음)**

```bash
MSYS_NO_PATHCONV=1 $AWSCLI ssm get-parameters-by-path --path "/mudo/prod/tenants/academy-b" --query "length(Parameters)"
MSYS_NO_PATHCONV=1 $AWSCLI ssm get-parameters-by-path --path "/mudo/prod/tenants/academy-c" --query "length(Parameters)"
```

기대: 둘 다 `19`.

> **알려진 제약**: `GOOGLE_REDIRECT_URI`를 Google Cloud Console의 OAuth 클라이언트 "승인된 리디렉션 URI"에 추가로 등록하지 않으면 academy-b/c에서 Google 계정 연동은 실패한다. 이번 리허설의 검증 목표(바인팩·설정 반영·S3/RDS 격리)에는 영향 없으므로 별도 태스크로 분리하지 않았고, Google Cloud Console 접근 권한이 있는 사람이 나중에 등록해야 한다.

---

### Task 6: S3 Task Role 생성 — academy-b, academy-c

**Files:** 없음 (IAM만)

- [ ] **Step 1: academy-b Task Role 생성**

```bash
$AWSCLI iam create-role --role-name mudo-prod-tenant-academy-b-task-role \
  --assume-role-policy-document '{"Version":"2012-10-17","Statement":[{"Effect":"Allow","Principal":{"Service":"ecs-tasks.amazonaws.com"},"Action":"sts:AssumeRole"}]}'
```

- [ ] **Step 2: S3 Prefix 정책 연결 (staff + finance 버킷 모두)**

```bash
$AWSCLI iam put-role-policy --role-name mudo-prod-tenant-academy-b-task-role \
  --policy-name mudo-prod-tenant-academy-b-s3-access --policy-document '{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "AllowTenantPrefixObjectAccess",
      "Effect": "Allow",
      "Action": ["s3:GetObject", "s3:PutObject", "s3:DeleteObject"],
      "Resource": [
        "arn:aws:s3:::mudo-prod-staff-635249349258/tenants/academy-b/*",
        "arn:aws:s3:::mudo-prod-finance-635249349258/tenants/academy-b/*"
      ]
    },
    {
      "Sid": "AllowListOwnPrefixOnly",
      "Effect": "Allow",
      "Action": "s3:ListBucket",
      "Resource": [
        "arn:aws:s3:::mudo-prod-staff-635249349258",
        "arn:aws:s3:::mudo-prod-finance-635249349258"
      ],
      "Condition": {"StringLike": {"s3:prefix": ["tenants/academy-b/*"]}}
    }
  ]
}'
```

- [ ] **Step 3: academy-c도 Step 1~2를 동일하게 반복** (`b`→`c`)

- [ ] **Step 4: 검증**

```bash
$AWSCLI iam list-role-policies --role-name mudo-prod-tenant-academy-b-task-role
$AWSCLI iam list-role-policies --role-name mudo-prod-tenant-academy-c-task-role
```

---

### Task 7: Flyway Migration Task 실행 — academy-b, academy-c

**Files:** 없음 (ECS Task 실행)

- [ ] **Step 1: Migration Task Definition 렌더링 스크립트 재사용해서 등록**

`infra/scripts/deploy_production.py`의 `render_migration_task`/`register_task`를 그대로 쓰기 위해, 로컬에서 파이썬으로 직접 호출한다(아직 `deploy()` 전체 파이프라인은 안 씀 — b/c가 `enabled:false`라 `deploy()`가 건드리지 않기 때문).

```bash
python3 - <<'PY'
import sys
sys.path.insert(0, "infra/scripts")
from deploy_production import render_migration_task, AwsDeployment
import yaml

tenants = yaml.safe_load(open("infra/tenants.yml"))["tenants"]
aws = AwsDeployment("ap-northeast-2")
migration_image = "mudo-groupware-backend-migrator:develop"  # develop 이미지로 스모크, 실제 배포 시 태그 확정값으로 교체

for code in ("academy-b", "academy-c"):
    tenant = next(t for t in tenants if t["code"] == code)
    task = render_migration_task(tenant, "635249349258", "ap-northeast-2", migration_image)
    task_arn = aws.register_task(task)
    print(code, task_arn)
    aws.migrate(tenant, {"ecs_cluster": "mudo-prod-cluster", "capacity_provider": "mudo-prod-capacity-provider"}, task_arn)
    print(code, "migration 완료")
PY
```

- [ ] **Step 2: 검증 — 각 학원 DB에 Flyway 스키마 히스토리 테이블이 생겼는지 확인**

```bash
mysql -h 127.0.0.1 -P 13306 -u academy_b_migrator -p"$B_MIG_PW" \
  -e "SELECT version, description FROM mudo_tenant_academy_b.flyway_schema_history ORDER BY installed_rank DESC LIMIT 5;"
```

(RDS 포트포워딩 세션을 다시 열어야 할 수 있음 — Task 4 Step 1 참고)

---

### Task 8: ALB Target Group + Host Rule 생성 — academy-b, academy-c

**Files:** 없음

- [ ] **Step 1: Target Group 생성 (academy-b)**

```bash
TG_B=$($AWSCLI elbv2 create-target-group --name mudo-prod-tg-academy-b \
  --protocol HTTP --port 8080 --vpc-id $VPC_ID --target-type instance \
  --health-check-path /actuator/health --health-check-port traffic-port \
  --query "TargetGroups[0].TargetGroupArn" --output text)
echo "TG_B=$TG_B"
```

- [ ] **Step 2: ALB Host Rule 추가 (기존 규칙 우선순위 100=academy-a, 50=monitoring과 겹치지 않게 101, 102 사용)**

```bash
$AWSCLI elbv2 create-rule --listener-arn $ALB_LISTENER_ARN --priority 101 \
  --conditions Field=host-header,Values=academy-b.ieum.store \
  --actions Type=forward,TargetGroupArn=$TG_B
```

- [ ] **Step 3: academy-c도 Step 1~2 반복 (priority 102)**

- [ ] **Step 4: 검증**

```bash
$AWSCLI elbv2 describe-rules --listener-arn $ALB_LISTENER_ARN --query "Rules[].{Priority:Priority,Host:Conditions[0].Values}"
```

기대: priority 100(academy-a), 101(academy-b), 102(academy-c) 모두 보임.

---

### Task 9: ECS Task Definition 등록 + Service 생성 — academy-b, academy-c

**Files:** 없음

- [ ] **Step 1: App Task Definition 렌더링 + 등록 (Task 7과 같은 방식)**

```bash
python3 - <<'PY'
import sys
sys.path.insert(0, "infra/scripts")
from deploy_production import render_app_task, AwsDeployment
import yaml, json

tenants = yaml.safe_load(open("infra/tenants.yml"))["tenants"]
profiles = yaml.safe_load(open("infra/runtime-profiles.yml"))["runtime_profiles"]
aws = AwsDeployment("ap-northeast-2")
app_image = "mudo-groupware-backend:develop"  # 리허설이므로 develop 이미지 사용

for code in ("academy-b", "academy-c"):
    tenant = next(t for t in tenants if t["code"] == code)
    profile = profiles[tenant["runtime_profile"]]
    task = render_app_task(tenant, profile, "635249349258", "ap-northeast-2", app_image, "rehearsal")
    task_arn = aws.register_task(task)
    print(code, task_arn)
PY
```

- [ ] **Step 2: ECS Service 생성 (academy-b) — binpack 배치 전략 academy-a와 동일하게 지정**

```bash
$AWSCLI ecs create-service --cluster $CLUSTER --service-name mudo-prod-svc-academy-b \
  --task-definition mudo-prod-tenant-academy-b --desired-count 1 \
  --capacity-provider-strategy capacityProvider=$CP,weight=1,base=0 \
  --placement-strategy type=binpack,field=MEMORY \
  --load-balancers targetGroupArn=$TG_B,containerName=app,containerPort=8080
```

- [ ] **Step 3: academy-c도 Step 2 반복**

- [ ] **Step 4: 서비스 안정화 대기 + 검증**

```bash
$AWSCLI ecs wait services-stable --cluster $CLUSTER --services mudo-prod-svc-academy-b mudo-prod-svc-academy-c
$AWSCLI ecs describe-services --cluster $CLUSTER --services mudo-prod-svc-academy-b mudo-prod-svc-academy-c \
  --query "services[].{name:serviceName,running:runningCount,desired:desiredCount}"
```

기대: 둘 다 running=desired=1.

---

### Task 10: `tenants.yml` enabled 전환 + 배포 파이프라인 스모크

**Files:**
- Modify: `infra/tenants.yml` (academy-b, academy-c의 `enabled: false` → `true`)

- [ ] **Step 1: 파일 수정 후 검증**

```bash
python infra/scripts/deploy_production.py --validate-only
```

기대: `활성 tenant 3개`, 용량 검사(Task 1에서 갱신한 값 기준) 통과.

- [ ] **Step 2: Smoke Test — 실제 HTTPS 헬스체크**

```bash
curl -sf https://academy-b.ieum.store/actuator/health
curl -sf https://academy-c.ieum.store/actuator/health
```

기대: 둘 다 `{"status":"UP"}` 계열 200 응답.

- [ ] **Step 3: WAF·Alloy·Grafana 확인 (22장 16~18단계, 새 설정 불필요 — 자동 적용 확인만)**

```bash
# WAF: 기존 Web ACL이 ALB 레벨에서 자동 적용되는지
$AWSCLI wafv2 get-web-acl-for-resource --resource-arn arn:aws:elasticloadbalancing:$REGION:$ACCOUNT_ID:loadbalancer/app/mudo-prod-alb/977e26c3d0b010fd
# Alloy: academy-b/c Task의 Prometheus 지표가 실제로 수집되는지
curl -s https://monitoring.ieum.store/api/v1/query --data-urlencode 'query=up{tenant="academy-b"}'
curl -s https://monitoring.ieum.store/api/v1/query --data-urlencode 'query=up{tenant="academy-c"}'
```

기대: WAF 응답에 `mudo-prod-web-acl` ARN 포함, Prometheus 쿼리 결과 `value: [.., "1"]`. Grafana 대시보드는 브라우저로 접속해서 테넌트 변수 드롭다운에 academy-b/c가 보이는지 육안 확인.

- [ ] **Step 4: 커밋**

```bash
git add infra/tenants.yml
git commit -m "feat: academy-b, academy-c 활성화(enabled: true)"
```

---

### Task 11: 검증 — 바인팩 배치

- [ ] **Step 1: academy-b/c Task가 어느 EC2에 배치됐는지 확인**

```bash
$AWSCLI ecs describe-tasks --cluster $CLUSTER \
  --tasks $($AWSCLI ecs list-tasks --cluster $CLUSTER --service-name mudo-prod-svc-academy-b --query "taskArns[0]" --output text) \
  --query "tasks[0].containerInstanceArn"
# academy-c도 동일하게 확인
```

- [ ] **Step 2: 설계 문서 §설계 결정 3-2 예측(academy-a+b가 인스턴스1, c가 인스턴스2)과 실제 배치를 비교, 결과를 설계 문서에 기록**

---

### Task 12: 검증 — 설정 반영

- [ ] **Step 1: 등록된 Task Definition의 실제 값이 `shared-default` 프로필과 일치하는지 확인**

```bash
$AWSCLI ecs describe-task-definition --task-definition mudo-prod-tenant-academy-b \
  --query "taskDefinition.containerDefinitions[0].{cpu:cpu,memRes:memoryReservation,mem:memory,env:environment}"
```

기대: cpu=500, memRes=640, mem=768, 환경변수 중 `SERVER_TOMCAT_THREADS_MAX=30`, `SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE=6` 등이 `runtime-profiles.yml`의 `shared-default` 값과 정확히 일치.

---

### Task 13: 검증 — S3/RDS 테넌트 격리

- [ ] **Step 1: S3 격리 — academy-b Task Role로 academy-a Prefix 접근 시도**

academy-b Task 컨테이너 안에서(ECS Exec) 또는 academy-b Task Role을 임시로 assume해서:

```bash
$AWSCLI sts assume-role --role-arn arn:aws:iam::$ACCOUNT_ID:role/mudo-prod-tenant-academy-b-task-role \
  --role-session-name isolation-test --query "Credentials"
# 발급받은 임시 자격증명으로 아래 명령 실행 (AWS_ACCESS_KEY_ID 등 환경변수로 설정)
aws s3 ls s3://mudo-prod-staff-635249349258/tenants/academy-a/
```

기대: `AccessDenied`.

- [ ] **Step 2: RDS 격리 — Task 4 Step 4에서 이미 확인함 (academy_b_app이 academy-a DB 접근 거부됨). 여기서는 academy-c 방향도 추가 확인**

```bash
mysql -h 127.0.0.1 -P 13306 -u academy_b_app -p"$B_APP_PW" -e "USE mudo_tenant_academy_c;" 2>&1
```

기대: `Access denied`.

---

### Task 14: PR 생성

- [ ] **Step 1: 브랜치 push, PR 생성** (AGENTS.md 규칙: 직접 push하지 않고 사용자에게 명령어 전달)

```bash
git push -u origin test-academies-onboarding
```

- [ ] **Step 2: PR 본문에 Task 11~13 검증 결과(바인팩 배치 스크린샷/로그, 설정 반영 diff, 격리 테스트 출력)를 첨부**
