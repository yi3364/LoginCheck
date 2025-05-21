# 1. 获取最新版本号
$changelog = Get-Content ../CHANGELOG.md
$versionLine = $changelog | Where-Object { $_ -match '^##\s*V([\d\.]+)' } | Select-Object -First 1
if ($versionLine -match '^##\s*V([\d\.]+)') {
    $version = $Matches[1]
}
else {
    Write-Host "未找到版本号，脚本终止"
    exit 1
}

# 2. 修改 plugin.yml
$pluginYmlPath = "../src/main/resources/plugin.yml"
$pluginYml = Get-Content $pluginYmlPath
$pluginYml = $pluginYml -replace '^(version:\s*).+$', "`$1$version"
Set-Content $pluginYmlPath $pluginYml

# 3. 修改 pom.xml
$pomPath = "../pom.xml"
$pom = Get-Content $pomPath
$pom = $pom -replace '(<version>)[^<]+(</version>)', "`$1$version`$2"
Set-Content $pomPath $pom

# 4. 获取该版本的更新内容
$body = ""
$start = $false
foreach ($line in $changelog) {
    if ($line -match "^##\s*V$version") {
        $start = $true
        continue
    }
    if ($start -and $line -match "^##\s*V") {
        break
    }
    if ($start) {
        $body += "$line`n"
    }
}

# 5. 自动提交版本号变更
git add $pluginYmlPath $pomPath
git commit -m "chore: 自动同步版本号到 $version"

# 6. 创建带注释的 tag
git tag -a "v$version" -m "v$version`n$body"

# 7. 推送代码和 tag
git push
git push origin "v$version"