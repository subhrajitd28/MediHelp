# MediHelp - Stop Azure VM (saves credits)
# Run from Windows PowerShell
Write-Host "Stopping MediHelp Azure VM (saves credits)..." -ForegroundColor Yellow
az vm deallocate --resource-group medihelp-rg --name medihelp-server
Write-Host "VM stopped. Credits are now being saved." -ForegroundColor Green
Write-Host "To restart: .\azure-start.ps1" -ForegroundColor Cyan
