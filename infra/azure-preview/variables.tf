variable "subscription_id" {
  description = "Azure subscription ID used for the SamaySetu preview infrastructure."
  type        = string
  default     = "01841f56-5dc1-486f-b327-618601cde029"
}

variable "resource_group_name" {
  description = "Resource group for preview-only Azure infrastructure."
  type        = string
  default     = "rg-samaysetu-preview"
}

variable "app_location" {
  description = "Azure region for App Service resources."
  type        = string
  default     = "Central India"
}

variable "database_location" {
  description = "Azure region for PostgreSQL Flexible Server."
  type        = string
  default     = "Southeast Asia"
}

variable "app_service_plan_name" {
  description = "Linux App Service plan name."
  type        = string
  default     = "asp-samaysetu-preview"
}

variable "web_app_name" {
  description = "Globally unique Azure Linux Web App name."
  type        = string
  default     = "samaysetu-preview-api"
}

variable "postgres_server_name" {
  description = "Globally unique Azure PostgreSQL Flexible Server name."
  type        = string
  default     = "psql-samaysetu-preview"
}

variable "postgres_database_name" {
  description = "Preview database name created inside PostgreSQL Flexible Server."
  type        = string
  default     = "samaysetu_preview"
}

variable "postgres_admin_username" {
  description = "PostgreSQL administrator username. Do not use reserved names such as admin, azure_superuser, or root."
  type        = string
  default     = "samaysetuadmin"
}

variable "postgres_admin_password" {
  description = "PostgreSQL administrator password. Prefer setting with TF_VAR_postgres_admin_password."
  type        = string
  sensitive   = true
}

variable "postgres_version" {
  description = "PostgreSQL major version for Flexible Server."
  type        = string
  default     = "16"
}

variable "postgres_sku_name" {
  description = "Minimum practical burstable PostgreSQL SKU for preview."
  type        = string
  default     = "B_Standard_B1ms"
}

variable "postgres_storage_mb" {
  description = "Minimum PostgreSQL storage size in MB."
  type        = number
  default     = 32768
}

variable "frontend_url" {
  description = "Already deployed Vercel frontend URL."
  type        = string
  default     = "https://samaysetu.vercel.app"
}

variable "allowed_client_ip_ranges" {
  description = "Optional client IP ranges allowed to connect directly to PostgreSQL, for local psql verification. Leave empty for Azure-services-only access."
  type = list(object({
    name     = string
    start_ip = string
    end_ip   = string
  }))
  default = []
}

variable "tags" {
  description = "Tags applied to preview Azure resources."
  type        = map(string)
  default = {
    project     = "SamaySetu"
    environment = "preview"
    managed_by  = "terraform"
  }
}
