# MediHelp - Start Azure VM and connect
# Run from Windows PowerShell
Write-Host "Starting MediHelp Azure VM..." -ForegroundColor Yellow
az vm start --resource-group medihelp-rg --name medihelp-server
$ip = az vm show -d --resource-group medihelp-rg --name medihelp-server --query publicIps -o tsv
Write-Host "VM started! IP: $ip" -ForegroundColor Green
Write-Host ""
Write-Host "Connect with: ssh -i .\medihelp-server_key.pem azureuser@$ip" -ForegroundColor Cyan
Write-Host "Then run:      cd medihelp-src && bash deploy/scripts/start-all.sh" -ForegroundColor Cyan
Write-Host ""
Write-Host "Frontend will be at: http://$ip" -ForegroundColor Green
