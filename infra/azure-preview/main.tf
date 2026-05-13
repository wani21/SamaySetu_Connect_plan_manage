locals {
  backend_url = "https://${var.web_app_name}.azurewebsites.net"

  # Runtime secrets are intentionally placeholders here. Set real values with
  # `az webapp config appsettings set` after Terraform creates the Web App.
  # The Web App ignores future app_settings drift so secret rotation is not
  # pushed into Terraform state.
  preview_app_settings = {
    SPRING_PROFILES_ACTIVE         = "preview"
    SPRING_DATASOURCE_URL          = "jdbc:postgresql://${azurerm_postgresql_flexible_server.preview.fqdn}:5432/${azurerm_postgresql_flexible_server_database.preview.name}?sslmode=require"
    SPRING_DATASOURCE_USERNAME     = var.postgres_admin_username
    SPRING_DATASOURCE_PASSWORD     = "__SET_IN_AZURE_APP_SETTINGS__"
    JWT_SECRET_KEY                 = "__SET_IN_AZURE_APP_SETTINGS__"
    EMAIL_USERNAME                 = "__SET_IN_AZURE_APP_SETTINGS__"
    EMAIL_PASSWORD                 = "__SET_IN_AZURE_APP_SETTINGS__"
    APP_BASE_URL                   = local.backend_url
    APP_FRONTEND_URL               = var.frontend_url
    APP_CORS_ALLOWED_ORIGINS       = var.frontend_url
    APP_ADMIN_EMAIL                = "__SET_IN_AZURE_APP_SETTINGS__"
    APP_ADMIN_PASSWORD             = "__SET_IN_AZURE_APP_SETTINGS__"
    APP_ADMIN_NAME                 = "Preview Admin"
    APP_ADMIN_EMPLOYEE_ID          = "PREVIEWADMIN001"
    REDIS_HOST                     = "__SET_IN_AZURE_APP_SETTINGS__"
    REDIS_PORT                     = "6379"
    REDIS_PASSWORD                 = "__SET_IN_AZURE_APP_SETTINGS__"
    REDIS_SSL_ENABLED              = "true"
    PORT                           = "8080"
    WEBSITE_RUN_FROM_PACKAGE       = "1"
    SCM_DO_BUILD_DURING_DEPLOYMENT = "false"
  }
}

resource "azurerm_resource_group" "preview" {
  name     = var.resource_group_name
  location = var.app_location
  tags     = var.tags
}

resource "azurerm_service_plan" "preview" {
  name                = var.app_service_plan_name
  resource_group_name = azurerm_resource_group.preview.name
  location            = var.app_location
  os_type             = "Linux"
  sku_name            = "B1"
  tags                = var.tags
}

resource "azurerm_linux_web_app" "preview" {
  name                = var.web_app_name
  resource_group_name = azurerm_resource_group.preview.name
  location            = var.app_location
  service_plan_id     = azurerm_service_plan.preview.id
  https_only          = true
  tags                = var.tags

  site_config {
    always_on                         = true
    ftps_state                        = "Disabled"
    health_check_eviction_time_in_min = 10
    health_check_path                 = "/actuator/health"
    minimum_tls_version               = "1.2"
    scm_minimum_tls_version           = "1.2"

    application_stack {
      java_server         = "JAVA"
      java_server_version = "17"
      java_version        = "17"
    }
  }

  app_settings = local.preview_app_settings

  lifecycle {
    ignore_changes = [
      app_settings
    ]
  }
}

resource "azurerm_postgresql_flexible_server" "preview" {
  name                          = var.postgres_server_name
  resource_group_name           = azurerm_resource_group.preview.name
  location                      = var.database_location
  version                       = var.postgres_version
  administrator_login           = var.postgres_admin_username
  administrator_password        = var.postgres_admin_password
  sku_name                      = var.postgres_sku_name
  storage_mb                    = var.postgres_storage_mb
  public_network_access_enabled = true
  zone                          = "1"
  tags                          = var.tags

  backup_retention_days        = 7
  geo_redundant_backup_enabled = false
}

resource "azurerm_postgresql_flexible_server_database" "preview" {
  name      = var.postgres_database_name
  server_id = azurerm_postgresql_flexible_server.preview.id
  charset   = "UTF8"
  collation = "en_US.utf8"
}

resource "azurerm_postgresql_flexible_server_firewall_rule" "allow_azure_services" {
  name             = "allow-azure-services"
  server_id        = azurerm_postgresql_flexible_server.preview.id
  start_ip_address = "0.0.0.0"
  end_ip_address   = "0.0.0.0"
}

resource "azurerm_postgresql_flexible_server_firewall_rule" "client_ranges" {
  for_each = {
    for range in var.allowed_client_ip_ranges : range.name => range
  }

  name             = each.value.name
  server_id        = azurerm_postgresql_flexible_server.preview.id
  start_ip_address = each.value.start_ip
  end_ip_address   = each.value.end_ip
}
