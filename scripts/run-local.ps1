# .env를 현재 프로세스 환경변수로 불러온 뒤 bootRun을 실행한다.
# 사용법: 프로젝트 루트에서  .\scripts\run-local.ps1

$envFile = Join-Path $PSScriptRoot "..\.env"

if (-not (Test-Path $envFile)) {
    Write-Error ".env 파일이 없습니다. .env.example을 복사해서 .env를 만들고 값을 채워주세요."
    exit 1
}

Get-Content $envFile | ForEach-Object {
    $line = $_.Trim()
    if ($line -and -not $line.StartsWith("#") -and $line.Contains("=")) {
        $key, $value = $line.Split("=", 2)
        [System.Environment]::SetEnvironmentVariable($key.Trim(), $value.Trim(), "Process")
    }
}

Push-Location (Join-Path $PSScriptRoot "..")
try {
    & .\gradlew.bat bootRun
} finally {
    Pop-Location
}
