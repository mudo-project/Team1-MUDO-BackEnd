import copy
import unittest

from deploy_production import (
    DeploymentError,
    load_manifests,
    render_app_task,
    render_migration_task,
    render_platform_tenant_registry,
    validate_manifests,
)


class DeploymentManifestTest(unittest.TestCase):
    def setUp(self):
        self.profiles, self.tenants, self.cells = load_manifests()

    def enabled_configuration(self):
        tenants = copy.deepcopy(self.tenants)
        cells = copy.deepcopy(self.cells)
        tenants[0]["enabled"] = True
        tenants[0]["platform_dashboard_host"] = True
        tenants[0]["s3_bucket"] = "mudo-prod-staff-123456789012"
        tenants[0]["finance_s3_bucket"] = "mudo-prod-finance-123456789012"
        cells["cell-1"]["ecs_registered_cpu"] = 4096
        cells["cell-1"]["ecs_registered_memory_mib"] = 8192
        cells["cell-1"]["rds_max_connections"] = 100
        return tenants, cells

    def test_repository_manifests_are_valid_with_enabled_tenants(self):
        enabled = validate_manifests(
            self.profiles, self.tenants, self.cells, deployment=False
        )
        self.assertEqual(
            ["academy-a", "academy-d"], [tenant["code"] for tenant in enabled]
        )

    def test_capacity_includes_one_sequential_deployment_surge(self):
        tenants, cells = self.enabled_configuration()
        enabled = validate_manifests(self.profiles, tenants, cells, deployment=True)
        self.assertEqual(
            ["academy-a", "academy-d"], [tenant["code"] for tenant in enabled]
        )

        cells["cell-1"]["rds_max_connections"] = 10
        with self.assertRaises(DeploymentError):
            validate_manifests(self.profiles, tenants, cells, deployment=True)

    def test_renders_tenant_billing_plan_and_runtime_profile_into_app_task(self):
        tenants, cells = self.enabled_configuration()
        task = render_app_task(
            tenants[0],
            self.profiles["shared-default"],
            "123456789012",
            "ap-northeast-2",
            "registry/mudo:abc123",
            "abc123",
            render_platform_tenant_registry(tenants, cells),
        )
        container = task["containerDefinitions"][0]
        environment = {item["name"]: item["value"] for item in container["environment"]}
        secrets = {item["name"]: item["valueFrom"] for item in container["secrets"]}
        secret_names = set(secrets)

        self.assertEqual("academy-a", environment["TENANT_ID"])
        self.assertEqual("mudo-prod-staff-123456789012", environment["AWS_S3_STAFF_BUCKET_NAME"])
        self.assertEqual("mudo-prod-finance-123456789012", environment["AWS_S3_FINANCE_BUCKET_NAME"])
        self.assertEqual("basic", environment["TENANT_PLAN"])
        self.assertEqual("true", environment["PLATFORM_DASHBOARD_ENABLED"])
        self.assertIn('"code":"academy-a"', environment["PLATFORM_DASHBOARD_TENANT_REGISTRY_JSON"])
        self.assertEqual("30", environment["SERVER_TOMCAT_THREADS_MAX"])
        self.assertEqual(500, container["cpu"])
        self.assertEqual(640, container["memoryReservation"])
        self.assertEqual(768, container["memory"])
        self.assertEqual("academy-a", container["dockerLabels"]["mudo.tenant"])
        self.assertIn("DB_PASSWORD", secret_names)
        self.assertIn("GOOGLE_TOKEN_ENCRYPTION_KEY", secret_names)
        self.assertIn("GOOGLE_CLIENT_ID", secret_names)
        self.assertIn("GOOGLE_CLIENT_SECRET", secret_names)
        self.assertIn("GOOGLE_REDIRECT_URI", secret_names)
        self.assertIn("GOOGLE_OAUTH_FRONTEND_REDIRECT_URI", secret_names)
        self.assertIn("MAILGUN_API_KEY", secret_names)
        self.assertIn("MAIL_FROM", secret_names)
        self.assertIn("MAILGUN_WEBHOOK_SIGNING_KEY", secret_names)
        self.assertIn("GEMINI_API_KEY", secret_names)
        self.assertIn("CLIENT_IP_SIGNING_SECRET", secret_names)
        self.assertEqual(
            "arn:aws:ssm:ap-northeast-2:123456789012:parameter/mudo/prod/shared/SENTRY_DSN",
            secrets["SENTRY_DSN"],
        )
        self.assertNotIn("DB_PASSWORD", environment)

    def test_non_dashboard_tenant_does_not_receive_platform_registry(self):
        tenants, _ = self.enabled_configuration()
        tenants[0]["platform_dashboard_host"] = False
        task = render_app_task(
            tenants[0], self.profiles["shared-default"], "123456789012", "ap-northeast-2",
            "registry/mudo:abc123", "abc123")
        environment = {item["name"]: item["value"] for item in task["containerDefinitions"][0]["environment"]}
        self.assertNotIn("PLATFORM_DASHBOARD_ENABLED", environment)
        self.assertNotIn("PLATFORM_DASHBOARD_TENANT_REGISTRY_JSON", environment)

    def test_billing_plan_does_not_change_runtime_resources(self):
        tenants, _ = self.enabled_configuration()
        basic_task = render_app_task(
            tenants[0],
            self.profiles["shared-default"],
            "123456789012",
            "ap-northeast-2",
            "registry/mudo:abc123",
            "abc123",
        )
        tenants[0]["billing_plan"] = "premium"
        premium_task = render_app_task(
            tenants[0],
            self.profiles["shared-default"],
            "123456789012",
            "ap-northeast-2",
            "registry/mudo:abc123",
            "abc123",
        )

        basic_container = basic_task["containerDefinitions"][0]
        premium_container = premium_task["containerDefinitions"][0]
        self.assertEqual(basic_container["cpu"], premium_container["cpu"])
        self.assertEqual(
            basic_container["memoryReservation"], premium_container["memoryReservation"]
        )
        self.assertEqual(basic_container["memory"], premium_container["memory"])

    def test_migration_task_uses_separate_migrator_parameters(self):
        tenants, _ = self.enabled_configuration()
        task = render_migration_task(
            tenants[0], "123456789012", "ap-northeast-2", "registry/mudo:migration-abc123"
        )
        secrets = {
            item["name"]: item["valueFrom"]
            for item in task["containerDefinitions"][0]["secrets"]
        }
        self.assertTrue(secrets["FLYWAY_USER"].endswith("/MIGRATOR_DB_USERNAME"))
        self.assertTrue(secrets["FLYWAY_PASSWORD"].endswith("/MIGRATOR_DB_PASSWORD"))


if __name__ == "__main__":
    unittest.main()
