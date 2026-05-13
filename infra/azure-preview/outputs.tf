output "resource_group_name" {
  description = "Preview resource group name."
  value       = azurerm_resource_group.preview.name
}

output "app_service_plan_name" {
  description = "Preview App Service plan name."
  value       = azurerm_service_plan.preview.name
}

output "web_app_name" {
  description = "Preview backend Azure Web App name."
  value       = azurerm_linux_web_app.preview.name
}

output "backend_url" {
  description = "Preview backend base URL."
  value       = "https://${azurerm_linux_web_app.preview.default_hostname}"
}

output "health_check_url" {
  description = "Public health check URL for Azure and deployment verification."
  value       = "https://${azurerm_linux_web_app.preview.default_hostname}/actuator/health"
}

output "postgres_server_name" {
  description = "Preview PostgreSQL Flexible Server name."
  value       = azurerm_postgresql_flexible_server.preview.name
}

output "postgres_fqdn" {
  description = "Preview PostgreSQL Flexible Server FQDN."
  value       = azurerm_postgresql_flexible_server.preview.fqdn
}

output "postgres_database_name" {
  description = "Preview PostgreSQL database name."
  value       = azurerm_postgresql_flexible_server_database.preview.name
}

output "spring_datasource_url" {
  description = "JDBC URL to set in Azure App Service settings."
  value       = "jdbc:postgresql://${azurerm_postgresql_flexible_server.preview.fqdn}:5432/${azurerm_postgresql_flexible_server_database.preview.name}?sslmode=require"
}

output "publish_profile_command" {
  description = "Command to download the Azure App Service publish profile for the GitHub secret."
  value       = "az webapp deployment list-publishing-profiles --resource-group ${azurerm_resource_group.preview.name} --name ${azurerm_linux_web_app.preview.name} --xml"
}
