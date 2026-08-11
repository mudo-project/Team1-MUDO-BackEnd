#!/usr/bin/env python3
"""Validate MUDO deployment manifests and deploy enabled tenants sequentially."""

from __future__ import annotations

import argparse
import json
import re
import subprocess
import sys
import tempfile
import time
import urllib.request
from pathlib import Path
from typing import Any

import yaml


REPOSITORY_ROOT = Path(__file__).resolve().parents[2]
INFRA_ROOT = REPOSITORY_ROOT / "infra"
CODE_PATTERN = re.compile(r"^[a-z0-9]+(?:-[a-z0-9]+)*$")
RUNTIME_PROFILE_FIELDS = (
    "cpu",
    "memory_reservation_mib",
    "memory_limit_mib",
    "tomcat_max_threads",
    "tomcat_queue_capacity",
    "hikari_max_pool_size",
    "hikari_connection_timeout_ms",
    "async_core_pool_size",
    "async_max_pool_size",
    "async_queue_capacity",
    "heavy_job_max_concurrency",
)


class DeploymentError(RuntimeError):
    pass


def load_yaml(path: Path) -> dict[str, Any]:
    with path.open(encoding="utf-8") as stream:
        value = yaml.safe_load(stream)
    if not isinstance(value, dict):
        raise DeploymentError(f"{path} 최상위 값은 객체여야 합니다.")
    return value


def load_manifests() -> tuple[dict[str, Any], list[dict[str, Any]], dict[str, Any]]:
    profiles_document = load_yaml(INFRA_ROOT / "runtime-profiles.yml")
    tenants_document = load_yaml(INFRA_ROOT / "tenants.yml")
    cells_document = load_yaml(INFRA_ROOT / "cells.yml")
    profiles = profiles_document.get("runtime_profiles")
    tenants = tenants_document.get("tenants")
    cells = cells_document.get("cells")
    if not isinstance(profiles, dict) or not profiles:
        raise DeploymentError("infra/runtime-profiles.yml에 runtime_profiles가 필요합니다.")
    if not isinstance(tenants, list):
        raise DeploymentError("infra/tenants.yml에 tenants 배열이 필요합니다.")
    if not isinstance(cells, dict) or not cells:
        raise DeploymentError("infra/cells.yml에 cells가 필요합니다.")
    return profiles, tenants, cells


def positive_integer(value: Any, location: str) -> int:
    if not isinstance(value, int) or isinstance(value, bool) or value <= 0:
        raise DeploymentError(f"{location}은 1 이상의 정수여야 합니다.")
    return value


def ratio(value: Any, location: str) -> float:
    if not isinstance(value, (int, float)) or isinstance(value, bool) or not 0 < value <= 1:
        raise DeploymentError(f"{location}은 0 초과 1 이하 숫자여야 합니다.")
    return float(value)


def has_placeholder(value: str) -> bool:
    upper = value.upper()
    return "REPLACE" in upper or "<" in value or ">" in value


def validate_manifests(
    profiles: dict[str, Any],
    tenants: list[dict[str, Any]],
    cells: dict[str, Any],
    deployment: bool,
) -> list[dict[str, Any]]:
    for profile_name, profile in profiles.items():
        if not CODE_PATTERN.fullmatch(str(profile_name)) or not isinstance(profile, dict):
            raise DeploymentError(f"유효하지 않은 runtime profile: {profile_name}")
        for field in RUNTIME_PROFILE_FIELDS:
            positive_integer(
                profile.get(field), f"runtime_profiles.{profile_name}.{field}"
            )
        if profile["memory_reservation_mib"] > profile["memory_limit_mib"]:
            raise DeploymentError(
                f"runtime_profiles.{profile_name}: memoryReservation이 memory limit보다 큽니다."
            )
        if profile["async_core_pool_size"] > profile["async_max_pool_size"]:
            raise DeploymentError(
                f"runtime_profiles.{profile_name}: async core가 max보다 큽니다."
            )

    for cell_name, cell in cells.items():
        if not CODE_PATTERN.fullmatch(str(cell_name)) or not isinstance(cell, dict):
            raise DeploymentError(f"유효하지 않은 cell: {cell_name}")
        for field in ("ecs_cluster", "capacity_provider", "rds_identifier"):
            if not isinstance(cell.get(field), str) or not cell[field].strip():
                raise DeploymentError(f"cells.{cell_name}.{field}가 필요합니다.")
        ratio(cell.get("rds_app_connection_ratio"), f"cells.{cell_name}.rds_app_connection_ratio")
        ratio(cell.get("ecs_app_capacity_ratio"), f"cells.{cell_name}.ecs_app_capacity_ratio")

    enabled_tenants: list[dict[str, Any]] = []
    codes: set[str] = set()
    for index, tenant in enumerate(tenants):
        location = f"tenants[{index}]"
        if not isinstance(tenant, dict):
            raise DeploymentError(f"{location}은 객체여야 합니다.")
        code = tenant.get("code")
        if not isinstance(code, str) or not CODE_PATTERN.fullmatch(code):
            raise DeploymentError(f"{location}.code 형식이 올바르지 않습니다.")
        if code in codes:
            raise DeploymentError(f"중복 tenant code: {code}")
        codes.add(code)
        billing_plan = tenant.get("billing_plan")
        if not isinstance(billing_plan, str) or not CODE_PATTERN.fullmatch(billing_plan):
            raise DeploymentError(f"{location}.billing_plan 형식이 올바르지 않습니다.")
        if tenant.get("runtime_profile") not in profiles:
            raise DeploymentError(
                f"{location}.runtime_profile이 runtime-profiles.yml에 없습니다."
            )
        if tenant.get("cell") not in cells:
            raise DeploymentError(f"{location}.cell이 cells.yml에 없습니다.")
        if not isinstance(tenant.get("enabled"), bool):
            raise DeploymentError(f"{location}.enabled는 true 또는 false여야 합니다.")
        for field in ("service", "task_family", "health_url", "s3_bucket"):
            if not isinstance(tenant.get(field), str) or not tenant[field].strip():
                raise DeploymentError(f"{location}.{field}가 필요합니다.")
        if tenant["enabled"]:
            for field in ("health_url", "s3_bucket"):
                if has_placeholder(tenant[field]):
                    raise DeploymentError(f"활성 tenant의 {location}.{field}에 자리표시자가 남아 있습니다.")
            enabled_tenants.append(tenant)

    if deployment and not enabled_tenants:
        raise DeploymentError("enabled=true인 tenant가 없어 운영 배포를 중단합니다.")

    validate_capacity(profiles, enabled_tenants, cells, deployment)
    return enabled_tenants


def validate_capacity(
    profiles: dict[str, Any],
    enabled_tenants: list[dict[str, Any]],
    cells: dict[str, Any],
    deployment: bool,
) -> None:
    for cell_name, cell in cells.items():
        members = [tenant for tenant in enabled_tenants if tenant["cell"] == cell_name]
        if not members:
            continue
        capacity_fields = ("ecs_registered_cpu", "ecs_registered_memory_mib", "rds_max_connections")
        if deployment:
            for field in capacity_fields:
                positive_integer(cell.get(field), f"cells.{cell_name}.{field}")
        if any(cell.get(field) is None for field in capacity_fields):
            continue

        selected_profiles = [profiles[tenant["runtime_profile"]] for tenant in members]
        normal_cpu = sum(profile["cpu"] for profile in selected_profiles)
        normal_memory = sum(
            profile["memory_reservation_mib"] for profile in selected_profiles
        )
        normal_connections = sum(
            profile["hikari_max_pool_size"] for profile in selected_profiles
        )
        required_cpu = normal_cpu + max(profile["cpu"] for profile in selected_profiles)
        required_memory = normal_memory + max(
            profile["memory_reservation_mib"] for profile in selected_profiles
        )
        required_connections = normal_connections + max(
            profile["hikari_max_pool_size"] for profile in selected_profiles
        )

        cpu_budget = int(cell["ecs_registered_cpu"] * cell["ecs_app_capacity_ratio"])
        memory_budget = int(cell["ecs_registered_memory_mib"] * cell["ecs_app_capacity_ratio"])
        connection_budget = int(cell["rds_max_connections"] * cell["rds_app_connection_ratio"])
        if required_cpu > cpu_budget:
            raise DeploymentError(f"{cell_name} ECS CPU 배포 여유가 부족합니다: {required_cpu}>{cpu_budget}")
        if required_memory > memory_budget:
            raise DeploymentError(
                f"{cell_name} ECS 메모리 배포 여유가 부족합니다: {required_memory}>{memory_budget}"
            )
        if required_connections > connection_budget:
            raise DeploymentError(
                f"{cell_name} RDS 연결 예산이 부족합니다: {required_connections}>{connection_budget}"
            )


def parameter_arn(region: str, account_id: str, tenant: str, name: str) -> str:
    return f"arn:aws:ssm:{region}:{account_id}:parameter/mudo/prod/tenants/{tenant}/{name}"


def render_app_task(
    tenant: dict[str, Any],
    profile: dict[str, Any],
    account_id: str,
    region: str,
    app_image: str,
    deployment_sha: str,
) -> dict[str, Any]:
    with (INFRA_ROOT / "ecs/app-task-definition.template.json").open(encoding="utf-8") as stream:
        task = json.load(stream)
    code = tenant["code"]
    task["family"] = tenant["task_family"]
    task["executionRoleArn"] = f"arn:aws:iam::{account_id}:role/mudo-prod-ecs-task-execution-role"
    task["taskRoleArn"] = f"arn:aws:iam::{account_id}:role/mudo-prod-tenant-{code}-task-role"
    container = task["containerDefinitions"][0]
    container["image"] = app_image
    container["cpu"] = profile["cpu"]
    container["memoryReservation"] = profile["memory_reservation_mib"]
    container["memory"] = profile["memory_limit_mib"]
    container["dockerLabels"] = {
        "mudo.observe": "true",
        "mudo.tenant": code,
        "mudo.plan": tenant["billing_plan"],
        "mudo.service": "backend",
        "mudo.environment": "prod",
        "mudo.deployment_sha": deployment_sha,
        "prometheus.scrape": "true",
    }
    container["environment"] = [
        {"name": "SPRING_PROFILES_ACTIVE", "value": "prod"},
        {"name": "TENANT_ID", "value": code},
        {"name": "TENANT_PLAN", "value": tenant["billing_plan"]},
        {"name": "DEPLOYMENT_SHA", "value": deployment_sha},
        {
            "name": "SERVER_TOMCAT_THREADS_MAX",
            "value": str(profile["tomcat_max_threads"]),
        },
        {
            "name": "SERVER_TOMCAT_THREADS_MAX_QUEUE_CAPACITY",
            "value": str(profile["tomcat_queue_capacity"]),
        },
        {
            "name": "SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE",
            "value": str(profile["hikari_max_pool_size"]),
        },
        {
            "name": "SPRING_DATASOURCE_HIKARI_CONNECTION_TIMEOUT",
            "value": str(profile["hikari_connection_timeout_ms"]),
        },
        {
            "name": "APP_ASYNC_CORE_POOL_SIZE",
            "value": str(profile["async_core_pool_size"]),
        },
        {
            "name": "APP_ASYNC_MAX_POOL_SIZE",
            "value": str(profile["async_max_pool_size"]),
        },
        {
            "name": "APP_ASYNC_QUEUE_CAPACITY",
            "value": str(profile["async_queue_capacity"]),
        },
        {
            "name": "APP_HEAVY_JOB_MAX_CONCURRENCY",
            "value": str(profile["heavy_job_max_concurrency"]),
        },
        {"name": "AWS_S3_BUCKET_NAME", "value": tenant["s3_bucket"]},
        {
            "name": "JAVA_TOOL_OPTIONS",
            "value": "-XX:MaxRAMPercentage=65.0 -XX:InitialRAMPercentage=25.0 -Dfile.encoding=UTF-8",
        },
    ]
    secret_names = (
        "DB_URL",
        "DB_USERNAME",
        "DB_PASSWORD",
        "JWT_SECRET",
        "CORS_ALLOWED_ORIGINS",
        "WEBSOCKET_ALLOWED_ORIGINS",
        "GOOGLE_TOKEN_ENCRYPTION_KEY",
        "GOOGLE_CLIENT_ID",
        "GOOGLE_CLIENT_SECRET",
        "GOOGLE_REDIRECT_URI",
        "GOOGLE_OAUTH_FRONTEND_REDIRECT_URI",
        # 결재 첨부파일 AI 요약/구조화 추출(approval.GeminiSummarizerAdapter,
        # GeminiFieldExtractionAdapter)이 쓰는 Gemini API 키. 없으면 코드 기본값(빈 문자열)으로
        # 떨어져 Gemini 호출이 계속 실패한다.
        "GEMINI_API_KEY",
    )
    container["secrets"] = [
        {"name": name, "valueFrom": parameter_arn(region, account_id, code, name)}
        for name in secret_names
    ]
    return task


def render_migration_task(
    tenant: dict[str, Any], account_id: str, region: str, migration_image: str
) -> dict[str, Any]:
    with (INFRA_ROOT / "ecs/migration-task-definition.template.json").open(
        encoding="utf-8"
    ) as stream:
        task = json.load(stream)
    code = tenant["code"]
    task["family"] = f"mudo-prod-migration-{code}"
    task["executionRoleArn"] = f"arn:aws:iam::{account_id}:role/mudo-prod-ecs-task-execution-role"
    container = task["containerDefinitions"][0]
    container["image"] = migration_image
    container["logConfiguration"]["options"]["awslogs-region"] = region
    container["secrets"] = [
        {"name": "FLYWAY_URL", "valueFrom": parameter_arn(region, account_id, code, "DB_URL")},
        {
            "name": "FLYWAY_USER",
            "valueFrom": parameter_arn(region, account_id, code, "MIGRATOR_DB_USERNAME"),
        },
        {
            "name": "FLYWAY_PASSWORD",
            "valueFrom": parameter_arn(region, account_id, code, "MIGRATOR_DB_PASSWORD"),
        },
    ]
    return task


class AwsDeployment:
    def __init__(self, region: str):
        self.region = region

    def run_json(self, *arguments: str) -> dict[str, Any]:
        command = ["aws", *arguments, "--region", self.region, "--output", "json"]
        result = subprocess.run(command, check=False, capture_output=True, text=True)
        if result.returncode != 0:
            raise DeploymentError(result.stderr.strip() or "AWS CLI 명령이 실패했습니다.")
        return json.loads(result.stdout or "{}")

    def register_task(self, task: dict[str, Any]) -> str:
        with tempfile.NamedTemporaryFile(mode="w", suffix=".json", encoding="utf-8") as stream:
            json.dump(task, stream, ensure_ascii=False)
            stream.flush()
            response = self.run_json(
                "ecs", "register-task-definition", "--cli-input-json", f"file://{stream.name}"
            )
        return response["taskDefinition"]["taskDefinitionArn"]

    def migrate(self, tenant: dict[str, Any], cell: dict[str, Any], task_arn: str) -> None:
        response = self.run_json(
            "ecs",
            "run-task",
            "--cluster",
            cell["ecs_cluster"],
            "--task-definition",
            task_arn,
            "--capacity-provider-strategy",
            f"capacityProvider={cell['capacity_provider']},weight=1",
            "--count",
            "1",
            "--started-by",
            "github-actions-production-deploy",
        )
        if response.get("failures") or not response.get("tasks"):
            raise DeploymentError(f"{tenant['code']} Migration Task 실행에 실패했습니다.")
        running_task_arn = response["tasks"][0]["taskArn"]
        subprocess.run(
            [
                "aws",
                "ecs",
                "wait",
                "tasks-stopped",
                "--cluster",
                cell["ecs_cluster"],
                "--tasks",
                running_task_arn,
                "--region",
                self.region,
            ],
            check=True,
        )
        description = self.run_json(
            "ecs",
            "describe-tasks",
            "--cluster",
            cell["ecs_cluster"],
            "--tasks",
            running_task_arn,
        )
        task = description["tasks"][0]
        container = next(item for item in task["containers"] if item["name"] == "migration")
        if container.get("exitCode") != 0:
            raise DeploymentError(
                f"{tenant['code']} Migration 실패: {task.get('stoppedReason', '원인 미상')}"
            )

    def current_task(self, cluster: str, service: str) -> str:
        response = self.run_json(
            "ecs", "describe-services", "--cluster", cluster, "--services", service
        )
        services = response.get("services", [])
        if not services or services[0].get("status") == "INACTIVE":
            raise DeploymentError(f"ECS Service를 찾을 수 없습니다: {service}")
        return services[0]["taskDefinition"]

    def update_service(self, cluster: str, service: str, task_definition: str) -> None:
        self.run_json(
            "ecs",
            "update-service",
            "--cluster",
            cluster,
            "--service",
            service,
            "--task-definition",
            task_definition,
            "--force-new-deployment",
        )
        subprocess.run(
            [
                "aws",
                "ecs",
                "wait",
                "services-stable",
                "--cluster",
                cluster,
                "--services",
                service,
                "--region",
                self.region,
            ],
            check=True,
        )


def smoke_test(url: str) -> None:
    last_error: Exception | None = None
    for _ in range(12):
        try:
            with urllib.request.urlopen(url, timeout=10) as response:
                if response.status == 200:
                    return
                last_error = DeploymentError(f"Smoke Test HTTP {response.status}")
        except Exception as error:  # noqa: BLE001 - 마지막 오류를 배포 실패 원인으로 전달한다.
            last_error = error
        time.sleep(10)
    raise DeploymentError(f"Smoke Test 실패: {last_error}")


def deploy(
    args: argparse.Namespace,
    profiles: dict[str, Any],
    tenants: list[dict[str, Any]],
    cells: dict[str, Any],
) -> None:
    aws = AwsDeployment(args.region)
    for tenant in tenants:
        code = tenant["code"]
        cell = cells[tenant["cell"]]
        print(f"[{code}] Migration Task 시작", flush=True)
        migration_task = render_migration_task(
            tenant, args.account_id, args.region, args.migration_image
        )
        migration_task_arn = aws.register_task(migration_task)
        aws.migrate(tenant, cell, migration_task_arn)

        print(f"[{code}] ECS Service 순차 배포 시작", flush=True)
        previous_task_arn = aws.current_task(cell["ecs_cluster"], tenant["service"])
        app_task = render_app_task(
            tenant,
            profiles[tenant["runtime_profile"]],
            args.account_id,
            args.region,
            args.app_image,
            args.deployment_sha,
        )
        new_task_arn = aws.register_task(app_task)
        try:
            aws.update_service(cell["ecs_cluster"], tenant["service"], new_task_arn)
            smoke_test(tenant["health_url"])
        except Exception:
            print(f"[{code}] 배포 실패, 이전 Task Definition으로 롤백", file=sys.stderr, flush=True)
            aws.update_service(cell["ecs_cluster"], tenant["service"], previous_task_arn)
            raise
        print(f"[{code}] 배포 및 Smoke Test 완료", flush=True)


def parse_arguments() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--validate-only", action="store_true")
    parser.add_argument("--region")
    parser.add_argument("--account-id")
    parser.add_argument("--app-image")
    parser.add_argument("--migration-image")
    parser.add_argument("--deployment-sha")
    return parser.parse_args()


def main() -> int:
    try:
        args = parse_arguments()
        profiles, tenants, cells = load_manifests()
        enabled_tenants = validate_manifests(
            profiles, tenants, cells, not args.validate_only
        )
        if args.validate_only:
            print(f"매니페스트 검증 완료: 활성 tenant {len(enabled_tenants)}개")
            return 0
        for field in ("region", "account_id", "app_image", "migration_image", "deployment_sha"):
            if not getattr(args, field):
                raise DeploymentError(f"--{field.replace('_', '-')} 값이 필요합니다.")
        deploy(args, profiles, enabled_tenants, cells)
        return 0
    except (DeploymentError, subprocess.CalledProcessError) as error:
        print(f"운영 배포 중단: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
