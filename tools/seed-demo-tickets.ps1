param(
    [string]$BaseUrl = 'http://127.0.0.1:18096'
)

$ErrorActionPreference = 'Stop'

function Invoke-AtpApi {
    param(
        [string]$Method,
        [string]$Path,
        [string]$Token,
        $Body = $null
    )
    $headers = @{ token = $Token }
    $args = @{
        Method = $Method
        Uri = $BaseUrl + $Path
        Headers = $headers
        TimeoutSec = 20
    }
    if ($null -ne $Body) {
        $args.ContentType = 'application/json; charset=utf-8'
        $args.Body = ($Body | ConvertTo-Json -Depth 10 -Compress)
    }
    $response = Invoke-RestMethod @args
    if ($response.code -ne 1) {
        throw "API failed: $Method $Path -> $($response.msg)"
    }
    return $response.data
}

function Get-AtpToken {
    param([string]$Username, [string]$Password = '123456')
    $response = Invoke-RestMethod -Method Post -Uri ($BaseUrl + '/login') `
        -ContentType 'application/json; charset=utf-8' `
        -Body (@{ username = $Username; password = $Password } | ConvertTo-Json -Compress)
    if ($response.code -ne 1) { throw "Login failed: $Username" }
    return $response.data.token
}

function Find-Ticket {
    param([string]$Token, [string]$Title)
    $keyword = [uri]::EscapeDataString($Title)
    $page = Invoke-AtpApi -Method Get -Path "/tickets?page=1&pageSize=20&keyword=$keyword" -Token $Token
    return @($page.records | Where-Object { $_.title -eq $Title } | Select-Object -First 1)
}

function Set-TicketStatus {
    param([string]$Token, [long]$TicketId, [int]$Target, [string]$Reason)
    $detail = Invoke-AtpApi -Method Get -Path "/tickets/$TicketId" -Token $Token
    if ([int]$detail.status -eq $Target) { return }
    if (-not (@($detail.allowedNextStatus) -contains $Target)) { return }
    Invoke-AtpApi -Method Put -Path "/tickets/$TicketId/status" -Token $Token -Body @{
        targetStatus = $Target
        reason = $Reason
    } | Out-Null
}

function Ensure-Reply {
    param([string]$Token, [long]$TicketId, [string]$Content, [string]$Visible)
    $detail = Invoke-AtpApi -Method Get -Path "/tickets/$TicketId" -Token $Token
    $exists = @($detail.messages) | Where-Object { $_.content -eq $Content -and $_.visible -eq $Visible }
    if ($exists) { return }
    Invoke-AtpApi -Method Post -Path "/tickets/$TicketId/replies" -Token $Token -Body @{
        content = $Content
        visible = $Visible
    } | Out-Null
}

$adminToken = Get-AtpToken -Username 'admin'
$requesterToken = Get-AtpToken -Username 'zhaoliu'

$specs = @(
    @{
        title = '外卖订单超时40分钟仍未送达'
        content = '订单预计18:20送达，现在19:00仍未送达，骑手电话无人接听，希望确认配送情况。'
        categoryId = 2; priority = 1; channel = 'ai'; assigneeId = 4
        transitions = @(2)
        reply = '已联系配送站点核实，当前骑手车辆故障，站点正在改派。预计20分钟内恢复配送，您也可以选择取消订单并全额退款。'
        note = '站点调度回复：原骑手车辆故障，已安排备用骑手，20:05前到店取餐。'
    },
    @{
        title = '申请退款后银行卡迟迟未到账'
        content = '订单退款页面显示审核通过已经两天，但银行卡还没有收到退款。'
        categoryId = 6; priority = 2; channel = 'web'; assigneeId = 3
        transitions = @(2)
        reply = '已核对退款单，退款金额已由平台提交至银行卡渠道，银行通常需要1到3个工作日。当前状态为银行处理中，请留意到账短信。'
        note = '支付流水核对完成：退款单 RF202610240018，渠道状态 success，银行侧尚未回执。'
    },
    @{
        title = '优惠券无法与满减活动叠加'
        content = '结算页提示优惠券可用，但提交订单后变成原价，优惠券也没有被使用。'
        categoryId = 2; priority = 3; channel = 'web'; assigneeId = 3
        transitions = @(2, 4)
        reply = '已记录订单号和优惠券编号，系统显示该券可叠加但结算校验异常。我们正在核实优惠分摊，确认后会补发差额或恢复优惠券。'
        note = '疑似结算服务优惠互斥配置不一致，已复现，等待交易组反馈。'
    },
    @{
        title = '账号登录提示已被冻结'
        content = '今天登录时提示账号存在安全风险并被冻结，无法查看历史订单。'
        categoryId = 1; priority = 2; channel = 'phone'; assigneeId = $null
        transitions = @()
        reply = ''
        note = ''
    },
    @{
        title = '收到的餐品缺少饮料'
        content = '订单中的可乐没有收到，餐品包装完整，商家电话一直占线。'
        categoryId = 3; priority = 2; channel = 'ai'; assigneeId = 4
        transitions = @(2, 3)
        reply = '已联系商家核实，商家正在调取出餐监控。为避免耽误您的时间，我们先为您发起缺失商品的部分退款。'
        note = '商家表示需调取监控，预计30分钟反馈；已同步用户可走部分退款。'
    },
    @{
        title = '希望修改电子发票抬头'
        content = '发票已经开具，但公司抬头写错了，需要修改为公司全称并补充税号。'
        categoryId = 4; priority = 4; channel = 'web'; assigneeId = 3
        transitions = @(2, 4, 5)
        reply = '已为您处理发票红冲并重新开具。新发票会在1个工作日内发送到账户绑定邮箱。'
        note = '已红冲原发票，新发票号 FP202610240091，等待邮件发送。'
    },
    @{
        title = '骑手联系不上且订单位置不更新'
        content = '订单显示配送中，但骑手电话关机，地图位置20分钟没有变化。'
        categoryId = 5; priority = 1; channel = 'ai'; assigneeId = 4
        transitions = @(2)
        reply = '已升级配送调度处理。调度确认正在联系备用骑手，我们会在10分钟内给您进一步反馈。'
        note = '站点确认原骑手失联，已启用异常配送预案。'
    },
    @{
        title = '会员积分在订单完成后未到账'
        content = '订单昨天已经完成，但会员积分一直没有增加，希望核对。'
        categoryId = 1; priority = 3; channel = 'web'; assigneeId = $null
        transitions = @()
        reply = ''
        note = ''
    }
)

$created = 0
$reused = 0
foreach ($spec in $specs) {
    $ticket = Find-Ticket -Token $requesterToken -Title $spec.title
    if ($ticket) {
        $reused++
        $ticketId = [long]$ticket.id
    } else {
        $ticketId = [long](Invoke-AtpApi -Method Post -Path '/tickets' -Token $requesterToken -Body @{
            title = $spec.title
            content = $spec.content
            categoryId = $spec.categoryId
            priority = $spec.priority
            channel = $spec.channel
        })
        $created++
    }

    if ($null -ne $spec.assigneeId) {
        $detail = Invoke-AtpApi -Method Get -Path "/tickets/$ticketId" -Token $adminToken
        if ($null -eq $detail.assigneeId) {
            Invoke-AtpApi -Method Put -Path "/tickets/$ticketId/assignee" -Token $adminToken -Body @{
                assigneeId = $spec.assigneeId
                reason = '演示数据初始化：按业务类型分派处理人'
            } | Out-Null
        }
    }

    foreach ($target in $spec.transitions) {
        Set-TicketStatus -Token $adminToken -TicketId $ticketId -Target $target -Reason '演示数据初始化：推进工单处理状态'
    }

    if ($spec.reply) {
        Ensure-Reply -Token $adminToken -TicketId $ticketId -Content $spec.reply -Visible 'public'
    }
    if ($spec.note) {
        Ensure-Reply -Token $adminToken -TicketId $ticketId -Content $spec.note -Visible 'internal'
    }
    Write-Host ("ticket {0} -> id={1}" -f $spec.title, $ticketId)
}

Write-Host "ticket seed completed: created=$created reused=$reused"
