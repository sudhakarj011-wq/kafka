$services = @{
    'account-service' = 8081
    'notification-service' = 8083
    'fraud-service' = 8084
    'audit-service' = 8085
    'analytics-service' = 8086
    'api-gateway' = 8080
    'eureka-server' = 8761
}

$base = "payment-service"
cd C:\Users\Sudhakar\Desktop\kubernetes\kafka\helm-charts\banking-app\charts

foreach ($key in $services.Keys) {
    $svc = $key
    $port = $services[$key]
    
    if (Test-Path $svc) { Remove-Item -Recurse -Force $svc }
    Copy-Item -Recurse $base -Destination $svc
    
    $c = Get-Content "$svc\Chart.yaml" -Raw
    $c = $c -replace $base, $svc
    Set-Content "$svc\Chart.yaml" -Value $c
    
    $v = Get-Content "$svc\values.yaml" -Raw
    $v = $v -replace $base, $svc -replace "8082", "$port"
    Set-Content "$svc\values.yaml" -Value $v
    
    $d = Get-Content "$svc\templates\deployment.yaml" -Raw
    $d = $d -replace $base, $svc
    Set-Content "$svc\templates\deployment.yaml" -Value $d
    
    $s = Get-Content "$svc\templates\service.yaml" -Raw
    $s = $s -replace $base, $svc
    Set-Content "$svc\templates\service.yaml" -Value $s
    
    Write-Host "Created chart for $svc on port $port"
}
