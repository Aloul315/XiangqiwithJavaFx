$ErrorActionPreference = "Stop"

# 1. 定义路径
$buildDir = "build/install/XiangqiwithJavaFX"
$outputDir = "build/distribution/XiangqiGame"
$runtimeDir = "$outputDir/runtime"

# 获取当前 Java 路径
$javaExe = (Get-Command java).Source
$javaHome = (Get-Item $javaExe).Directory.Parent
Write-Host "Detected Java Home: $javaHome"

# 2. 清理并创建输出目录
if (Test-Path $outputDir) {
    Remove-Item -Path $outputDir -Recurse -Force
}
New-Item -ItemType Directory -Path $outputDir | Out-Null

# 3. 复制应用程序文件 (installDist 的结果)
Write-Host "Copying application files..."
Copy-Item -Path "$buildDir/*" -Destination $outputDir -Recurse

# 4. 复制 JDK 到 runtime 目录
Write-Host "Copying JDK to runtime folder (this may take a while)..."
# 排除一些不必要的文件以减小体积 (可选)
$excludeList = @("src.zip", "demo", "man", "sample")
New-Item -ItemType Directory -Path $runtimeDir | Out-Null
Copy-Item -Path "$javaHome/*" -Destination $runtimeDir -Recurse -Force

# 5. 创建启动脚本 StartGame.bat
Write-Host "Creating startup script..."
$batContent = @"
@echo off
set "DIR=%~dp0"
set "JAVA_HOME=%DIR%runtime"
set "PATH=%JAVA_HOME%\bin;%PATH%"
call "%DIR%bin\XiangqiwithJavaFX.bat"
"@
Set-Content -Path "$outputDir/StartGame.bat" -Value $batContent

Write-Host "Done! Distribution created at: $outputDir"
