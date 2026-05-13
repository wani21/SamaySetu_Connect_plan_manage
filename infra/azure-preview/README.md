# SamaySetu Azure Preview Infrastructure

This Terraform folder provisions only the preview Azure infrastructure:

- Resource group in Central India
- Linux App Service Plan, Basic B1
- Linux Web App for Java 17 Spring Boot JAR hosting
- Azure Database for PostgreSQL Flexible Server in Southeast Asia
- Preview PostgreSQL database
- PostgreSQL firewall rules for Azure App Service access, with optional local client IP access
- Non-secret App Service setting placeholders

AWS production files and workflows are intentionally outside this folder and must remain untouched.

## State and Secrets

Terraform local state is ignored by Git. The PostgreSQL administrator password is required by Azure during server creation and will exist in local Terraform state after apply. Keep `terraform.tfstate` local/private and do not commit it.

Runtime app secrets such as JWT, Gmail SMTP, Redis, admin bootstrap password, and the real datasource password are not meant to be committed. Terraform creates placeholders, then you set the real values directly in Azure App Service application settings.

The Web App ignores future `app_settings` drift so secret rotation through Azure CLI or the Azure Portal is not overwritten by later Terraform runs.

## Expected Frontend

The frontend is already deployed at:

```text
https://samaysetu.vercel.app
```

Terraform uses that URL for `APP_FRONTEND_URL` and `APP_CORS_ALLOWED_ORIGINS`.
