$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
$excelPath = "D:\other\AI\re\linux_do_interesting_posts_readable.xlsx"
$maxReplies = if ($args.Count -gt 0) { [int]$args[0] } else { 5 }
$bootstrapUrl = "https://linux.do/t/topic/2348278"
$avatarStorageDir = Join-Path $projectRoot "src\main\webapp\static\uploads\avatars"
$publicContextPath = "/ai-tef-forum"

function Get-AgentBrowserJson {
    param(
        [Parameter(Mandatory = $true)]
        [string[]]$Arguments
    )

    $output = & agent-browser-cli @Arguments
    if ([string]::IsNullOrWhiteSpace($output)) {
        throw "agent-browser-cli 返回为空：$($Arguments -join ' ')"
    }
    return $output | ConvertFrom-Json
}

Push-Location $projectRoot
try {
    if ([string]::IsNullOrWhiteSpace($bootstrapUrl)) {
        $bootstrapUrl = "https://linux.do/t/topic/2348278"
    }

    $openResult = Get-AgentBrowserJson @("open", $bootstrapUrl.Trim())
    if (-not $openResult.ok) {
        throw "无法打开 Linux.do 来源帖子"
    }

    $tabId = [string]$openResult.result.opened_tab_id
    if ([string]::IsNullOrWhiteSpace($tabId)) {
        throw "未获取到 Linux.do 标签页 ID"
    }

    $cookiesPayload = "{""cmd"":""cookies"",""tabId"":$tabId}"
    $cookiesResult = Get-AgentBrowserJson @("exec", $cookiesPayload)
    $cookies = @($cookiesResult.result.js_return) | Where-Object {
        $_.domain -like "*linux.do*" -and -not [string]::IsNullOrWhiteSpace($_.name)
    }
    $userAgentResult = Get-AgentBrowserJson @("exec", "--tab", "$tabId", "return navigator.userAgent")

    $hasSession = @($cookies | Where-Object { $_.name -eq "_forum_session" -or $_.name -eq "cf_clearance" }).Count -gt 0
    if (-not $hasSession) {
        throw "未从浏览器拿到可用的 linux.do 登录态 Cookie，请确认 Chrome 中该帖子页已经正常打开"
    }

    $cookieHeader = ($cookies | ForEach-Object { "$($_.name)=$($_.value)" }) -join "; "
    $userAgent = [string]$userAgentResult.result.js_return

    $cookieBase64 = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($cookieHeader))
    $uaBase64 = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($userAgent))

    mvn -q -DskipTests compile
    $classpathFile = Join-Path $env:TEMP "ai_tef_forum_import_classpath.txt"
    mvn -q -DskipTests dependency:build-classpath "-Dmdep.outputFile=$classpathFile"
    $dependencyClasspath = Get-Content $classpathFile -Raw
    $classpath = "target\classes;$dependencyClasspath"
    & java `
        "-Dlinuxdo.avatar.storage.dir=$avatarStorageDir" `
        "-Dlinuxdo.public.context.path=$publicContextPath" `
        -cp $classpath `
        com.aitef.forum.tools.LinuxDoExcelImporter `
        $excelPath `
        $cookieBase64 `
        $uaBase64 `
        $maxReplies
}
finally {
    Pop-Location
}
