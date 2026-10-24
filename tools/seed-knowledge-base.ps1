param(
    [string]$BaseDir = 'E:\Java\java_code\agent-ticket-platform',
    [string]$MySql = 'C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe',
    [string]$RedisContainer = 'redis-vector',
    [string]$EmbeddingModel = 'text-embedding-v3',
    [ValidateSet('docker', 'wsl')]
    [string]$RedisMode = 'docker',
    [string]$WslDistro = 'Ubuntu',
    [string]$WslRedisCli = '/home/a/redis-stack-demo/redis-stack-server-7.4.0-v8/bin/redis-cli',
    [int]$RedisPort = 6380
)

$ErrorActionPreference = 'Stop'
$docDir = Join-Path $BaseDir 'docs\knowledge-base'
$embeddingUrl = 'https://dashscope.aliyuncs.com/compatible-mode/v1/embeddings'
$apiKey = [Environment]::GetEnvironmentVariable('QW-API-KEY', 'User')
if ([string]::IsNullOrWhiteSpace($apiKey)) {
    $apiKey = [Environment]::GetEnvironmentVariable('QW-API-KEY', 'Process')
}
if ([string]::IsNullOrWhiteSpace($apiKey)) { throw 'QW-API-KEY 未配置' }
if (-not (Test-Path -LiteralPath $MySql)) { throw "mysql.exe 不存在：$MySql" }

function SqlQuote {
    param([AllowNull()][string]$Value)
    if ($null -eq $Value) { return 'NULL' }
    return "'" + $Value.Replace("'", "''") + "'"
}

function Invoke-MySql {
    param([string]$Sql, [switch]$Scalar)
    if ($Scalar) {
        $output = & $MySql --default-character-set=utf8mb4 -uroot -p123456 -N -B -e $Sql 2>$null
        return @($output)
    }
    & $MySql --default-character-set=utf8mb4 -uroot -p123456 -e $Sql 2>$null | Out-Null
}

function Invoke-Redis {
    param([string[]]$Arguments)
    if ($RedisMode -eq 'wsl') {
        $output = & wsl.exe -d $WslDistro -- $WslRedisCli -p $RedisPort @Arguments 2>&1
    } else {
        $output = & docker exec $RedisContainer redis-cli @Arguments 2>&1
    }
    if ($LASTEXITCODE -ne 0) {
        throw "Redis 命令失败：$($Arguments -join ' ') -> $($output -join ' ')"
    }
    return @($output)
}

$redisPing = (Invoke-Redis -Arguments @('PING')) -join '' 
if ($redisPing -notmatch 'PONG') { throw "Redis Stack 不可用，请先启动容器 $RedisContainer" }

function Get-Sha256 {
    param([string]$Text)
    $sha = [System.Security.Cryptography.SHA256]::Create()
    try {
        $bytes = [Text.Encoding]::UTF8.GetBytes($Text)
        return ([BitConverter]::ToString($sha.ComputeHash($bytes))).Replace('-', '').ToLowerInvariant()
    } finally {
        $sha.Dispose()
    }
}

function Split-Text {
    param([string]$Text, [int]$Size = 500, [int]$Overlap = 100)
    $normalized = $Text.Replace("`r`n", "`n").Trim()
    $chunks = [System.Collections.Generic.List[string]]::new()
    $start = 0
    while ($start -lt $normalized.Length) {
        $length = [Math]::Min($Size, $normalized.Length - $start)
        if ($start + $length -lt $normalized.Length) {
            $candidate = $normalized.Substring($start, $length)
            $breakAt = $candidate.LastIndexOf("`n`n")
            if ($breakAt -ge 220) { $length = $breakAt + 2 }
        }
        $piece = $normalized.Substring($start, $length).Trim()
        if ($piece) { $chunks.Add($piece) }
        if ($start + $length -ge $normalized.Length) { break }
        $start = $start + $length - $Overlap
    }
    return $chunks
}

function Get-Embeddings {
    param([string[]]$Texts)
    $result = [System.Collections.Generic.List[object]]::new()
    for ($from = 0; $from -lt $Texts.Count; $from += 20) {
        $to = [Math]::Min($from + 20, $Texts.Count)
        $batch = @($Texts[$from..($to - 1)])
        $body = @{
            model = $EmbeddingModel
            input = $batch
            dimensions = 1024
            encoding_format = 'float'
        } | ConvertTo-Json -Depth 5 -Compress
        $response = Invoke-RestMethod -Method Post -Uri $embeddingUrl `
            -Headers @{ Authorization = "Bearer $apiKey"; 'Content-Type' = 'application/json' } `
            -Body ([Text.Encoding]::UTF8.GetBytes($body)) -TimeoutSec 60
        foreach ($item in $response.data) { $result.Add($item.embedding) }
    }
    return $result
}

$metadata = @(
    @{ File = '01-refund-policy.md'; Title = '退款与售后处理规范'; Category = '退款' },
    @{ File = '02-delivery-exceptions.md'; Title = '外卖订单配送异常处理手册'; Category = '配送' },
    @{ File = '03-sla-and-ticket.md'; Title = '工单处理与 SLA 规范'; Category = '工单' },
    @{ File = '04-account-coupon-invoice.md'; Title = '账号、优惠券与会员积分规则'; Category = '账号' },
    @{ File = '05-external-takeout.md'; Title = '智能外卖平台外部系统接入说明'; Category = '外部系统' },
    @{ File = '06-food-safety.md'; Title = '食品安全与投诉升级规范'; Category = '食品安全' }
)

# 只清理本脚本写入的 seed 文档和对应向量，不碰其他知识库数据。
$seedRows = Invoke-MySql -Scalar -Sql "USE agent_ticket; SELECT c.vector_id FROM kb_chunk c JOIN kb_document d ON d.id=c.document_id WHERE d.source_url LIKE 'seed://knowledge/%' AND c.vector_id IS NOT NULL;"
foreach ($vectorId in @($seedRows | Where-Object { $_ })) {
    Invoke-Redis -Arguments @('DEL', ('embedding:' + $vectorId)) | Out-Null
}
Invoke-MySql -Sql "USE agent_ticket; DELETE c FROM kb_chunk c JOIN kb_document d ON d.id=c.document_id WHERE d.source_url LIKE 'seed://knowledge/%'; DELETE FROM kb_document WHERE source_url LIKE 'seed://knowledge/%';"
Invoke-Redis -Arguments @('DEL', 'embedding:probe-refund') | Out-Null

$totalChunks = 0
foreach ($docMeta in $metadata) {
    $path = (Join-Path $docDir $docMeta.File).Replace('\', '/')
    if (-not (Test-Path -LiteralPath $path)) { throw "知识库文件不存在：$path" }
    $text = [IO.File]::ReadAllText($path, [Text.Encoding]::UTF8)
    $chunks = @(Split-Text -Text $text)
    if ($chunks.Count -eq 0) { continue }

    $sourceUrl = 'seed://knowledge/' + $docMeta.File
    $insertDoc = "USE agent_ticket; INSERT INTO kb_document (title, doc_type, source_url, file_path, status, chunk_count, version, uploaded_by) VALUES (" +
        (SqlQuote $docMeta.Title) + ", 'file', " + (SqlQuote $sourceUrl) + ", " + (SqlQuote $path) + ", 'processing', 0, 1, 1); SELECT LAST_INSERT_ID();"
    $rows = Invoke-MySql -Scalar -Sql $insertDoc
    $documentId = [long]($rows | Select-Object -Last 1)

    $embeddings = @(Get-Embeddings -Texts $chunks)
    if ($embeddings.Count -ne $chunks.Count) { throw "向量数量不一致：$($docMeta.Title)" }

    for ($i = 0; $i -lt $chunks.Count; $i++) {
        $seq = $i + 1
        $content = [string]$chunks[$i]
        $hash = Get-Sha256 -Text $content
        $vectorId = "seed-$documentId-$seq"
        $vectorKey = "embedding:$vectorId"
        $record = @{
            text = $content
            vector = $embeddings[$i]
            document_id = $documentId
            doc_title = $docMeta.Title
            seq = $seq
        } | ConvertTo-Json -Depth 6 -Compress

        $writeResult = (Invoke-Redis -Arguments @('JSON.SET', $vectorKey, '$', $record)) -join ''
        if ($writeResult -notmatch 'OK') { throw "向量写入失败：$vectorKey -> $writeResult" }
        $tokenCount = [Math]::Max(1, [int]($content.Length / 4))
        $chunkSql = "USE agent_ticket; INSERT INTO kb_chunk (document_id, seq, content, token_count, vector_id, embedding_model, content_hash, category) VALUES (" +
            "$documentId, $seq, " + (SqlQuote $content) + ", $tokenCount, " + (SqlQuote $vectorId) + ", " +
            (SqlQuote $EmbeddingModel) + ", " + (SqlQuote $hash) + ", " + (SqlQuote $docMeta.Category) + ");"
        Invoke-MySql -Sql $chunkSql
        $totalChunks++
    }

    Invoke-MySql -Sql "USE agent_ticket; UPDATE kb_document SET status='ready', chunk_count=$($chunks.Count), error_msg=NULL WHERE id=$documentId;"
    Write-Host ("document ready: {0} (id={1}, chunks={2})" -f $docMeta.Title, $documentId, $chunks.Count)
}

# FAQ 作为后续 FAQ 检索的数据源；当前 FAQ 工具仍复用知识库召回。
$faqSeed = @(
    @{ Q = '退款多久能到账'; S = @('退款什么时候到账','申请退款后多久收到钱'); A = '银行卡通常1到3个工作日，支付宝和微信通常几分钟到2小时；超过预计时间需核对退款单号和支付渠道。'; C = '退款' },
    @{ Q = '订单超时未送达怎么办'; S = @('外卖一直没送到','配送超时如何处理'); A = '先查询订单状态并联系配送站点；超过预计时间20分钟且位置不更新时，可继续等待、取消退款或改派骑手。'; C = '配送' },
    @{ Q = '账号被冻结如何解封'; S = @('账号无法登录','登录提示有安全风险'); A = '先核对冻结原因和最近登录设备。临时风险冻结在身份校验后可解除，资金风险必须转安全管理员。'; C = '账号' },
    @{ Q = '优惠券可以叠加使用吗'; S = @('优惠券为什么不能用','满减和优惠券能一起用吗'); A = '同一订单通常只能使用一张平台券，是否可与商家券、会员折扣叠加以结算页展示为准；显示可用但失败时按系统异常升级。'; C = '优惠券' },
    @{ Q = '电子发票抬头如何修改'; S = @('发票抬头写错了','怎么重开发票'); A = '需要先红冲原发票再重新开具，客服应核对新抬头、税号、邮箱和金额，并说明预计处理周期。'; C = '发票' },
    @{ Q = '会员积分为什么没到账'; S = @('积分未增加','订单完成没有积分'); A = '积分通常在订单完成且超过售后期后发放；退款或取消订单会重新计算，需核对订单完成状态和退款记录。'; C = '会员积分' }
)

$questions = @($faqSeed | ForEach-Object { SqlQuote $_.Q }) -join ','
Invoke-MySql -Sql "USE agent_ticket; DELETE FROM kb_faq WHERE question IN ($questions);"
foreach ($faq in $faqSeed) {
    $similarJson = (@($faq.S | ForEach-Object { SqlQuote $_ }) -join ',')
    $hash = Get-Sha256 -Text ($faq.Q + '|' + $faq.A)
    $sql = "USE agent_ticket; INSERT INTO kb_faq (question, similar_questions, answer, category, content_hash, status) VALUES (" +
        (SqlQuote $faq.Q) + ", JSON_ARRAY($similarJson), " + (SqlQuote $faq.A) + ", " + (SqlQuote $faq.C) + ", " + (SqlQuote $hash) + ", 1);"
    Invoke-MySql -Sql $sql
}

Write-Host "knowledge seed completed: documents=$($metadata.Count), chunks=$totalChunks, faqs=$($faqSeed.Count)"








