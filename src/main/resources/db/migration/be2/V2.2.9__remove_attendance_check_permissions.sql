DELETE rp
FROM role_permission rp
JOIN permission p ON p.permission_id = rp.permission_id
WHERE p.code IN ('ATTENDANCE:CHECK_IN', 'ATTENDANCE:CHECK_OUT');

DELETE FROM permission
WHERE code IN ('ATTENDANCE:CHECK_IN', 'ATTENDANCE:CHECK_OUT');
