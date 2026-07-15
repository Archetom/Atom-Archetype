# HTTP API reference

This page documents the bundled User example. OpenAPI JSON is available at `/v3/api-docs` and Swagger UI at `/swagger-ui/index.html` in development. Production disables both by default.

## Authentication

All `/api/v1/users` operations require an authenticated principal and a tenant. The development adapter accepts `X-Dev-User-Id` and `X-Dev-Tenant-Id` only when the `dev` or `test` profile is active and `ATOM_SECURITY_TRUSTED_HEADER_ENABLED=true`.

These headers are development credentials, not a production authentication protocol. See [configuration](configuration.md) for production authentication requirements.

## Operations

| Method and path | Authority | Input | Success body |
| --- | --- | --- | --- |
| `POST /api/v1/users` | `users:write` | `UserCreateRequest` JSON | `UserResponse` |
| `GET /api/v1/users/{userId}` | `users:read` | Positive numeric path ID | `UserResponse` |
| `GET /api/v1/users` | `users:read` | Query parameters below | Page of `UserResponse` |
| `PUT /api/v1/users/{userId}/status` | `users:write` | `status` query parameter | Empty success body |
| `DELETE /api/v1/users/{userId}` | `users:delete` | Positive numeric path ID | Empty success body |

## Create request

```json
{
  "username": "alice",
  "email": "alice@example.com",
  "phoneNumber": "+12025550123",
  "password": "correct horse battery staple",
  "realName": "Alice Example"
}
```

Passwords are write-only, contain 12–64 Unicode characters, remain within BCrypt's 72-byte UTF-8 input limit, and never appear in responses or logs. Phone numbers use E.164 form, including the leading `+` and country code.

## Query parameters

| Name | Type | Default | Constraint |
| --- | --- | --- | --- |
| `page` | integer | `1` | `1` through `1,000,000` |
| `size` | integer | `20` | `1` through `200` |
| `username` | string | — | At most 50 characters |
| `email` | string | — | Valid email address |
| `status` | string | — | One of the status values below |

The page response contains `pageNum`, `pageSize`, `totalNum`, and `objectList`. `totalNum` is always a non-negative integer. Any other serialized property (such as `meta`) is a reserved internal carrier, always `null`, and not part of the public contract — clients must not depend on it.

## User status

Public status values are `ACTIVE`, `INACTIVE`, and `LOCKED`. `DELETED` represents soft deletion and is changed only through the delete operation. List queries exclude deleted users unless a dedicated administrative use case explicitly requests them.

## Error response

Errors have a stable public shape:

```json
{
  "errCode": "DE0311000101",
  "errMsg": "Invalid request"
}
```

| HTTP status | Meaning |
| --- | --- |
| `400` | Invalid syntax, validation failure, or unsupported status transition |
| `401` | Authentication is missing or invalid |
| `403` | The authenticated caller lacks the required authority |
| `404` | The tenant-scoped resource does not exist |
| `409` | A unique value or aggregate version conflicts |
| `422` | Another stable domain rule rejected the operation |
| `500` | Unexpected internal failure; internal details are not exposed |
