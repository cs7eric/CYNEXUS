param(
    [string]$BaseUrl = "https://api.xhhai.online",
    [string]$Model = "gpt-5.4-mini",
    [string]$ApiKey = $env:XHHAI_API_KEY,
    [string]$Prompt = "Reply with exactly: websocket-ok",
    [ValidateRange(5, 300)]
    [int]$TimeoutSeconds = 60
)

$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($ApiKey)) {
    throw "Missing API key. Set XHHAI_API_KEY or pass -ApiKey."
}

$base = $BaseUrl.TrimEnd("/")
$httpEndpoint = if ($base.EndsWith("/v1", [StringComparison]::OrdinalIgnoreCase)) {
    "$base/responses"
} else {
    "$base/v1/responses"
}
$endpointBuilder = [UriBuilder]::new($httpEndpoint)
$endpointBuilder.Scheme = if ($endpointBuilder.Scheme -eq "https") { "wss" } else { "ws" }
$endpointBuilder.Port = -1
$endpoint = $endpointBuilder.Uri

$socket = [Net.WebSockets.ClientWebSocket]::new()
$socket.Options.SetRequestHeader("Authorization", "Bearer $ApiKey")
$timeout = [Threading.CancellationTokenSource]::new([TimeSpan]::FromSeconds($TimeoutSeconds))
$outputText = [Text.StringBuilder]::new()

try {
    Write-Host "CONNECT $endpoint"
    [void]$socket.ConnectAsync($endpoint, $timeout.Token).GetAwaiter().GetResult()
    Write-Host "CONNECTED state=$($socket.State)"

    $payload = [ordered]@{
        type = "response.create"
        model = $Model
        store = $false
        input = $Prompt
    } | ConvertTo-Json -Compress -Depth 10
    $payloadBytes = [Text.Encoding]::UTF8.GetBytes($payload)
    $payloadSegment = [ArraySegment[byte]]::new($payloadBytes)
    [void]$socket.SendAsync(
        $payloadSegment,
        [Net.WebSockets.WebSocketMessageType]::Text,
        $true,
        $timeout.Token
    ).GetAwaiter().GetResult()
    Write-Host "SENT type=response.create model=$Model"

    $buffer = [byte[]]::new(65536)
    while ($socket.State -eq [Net.WebSockets.WebSocketState]::Open) {
        $message = [IO.MemoryStream]::new()
        try {
            do {
                $segment = [ArraySegment[byte]]::new($buffer)
                $received = $socket.ReceiveAsync($segment, $timeout.Token).GetAwaiter().GetResult()
                if ($received.MessageType -eq [Net.WebSockets.WebSocketMessageType]::Close) {
                    $statusCode = if ($null -eq $received.CloseStatus) { "unknown" } else {
                        [int]$received.CloseStatus
                    }
                    Write-Host "CLOSED code=$statusCode reason=$($received.CloseStatusDescription)"
                    exit 2
                }
                $message.Write($buffer, 0, $received.Count)
            } while (-not $received.EndOfMessage)

            $eventJson = [Text.Encoding]::UTF8.GetString($message.ToArray())
            $event = $eventJson | ConvertFrom-Json
            Write-Host "EVENT type=$($event.type)"

            if ($event.type -eq "response.output_text.delta" -and $null -ne $event.delta) {
                [void]$outputText.Append([string]$event.delta)
            }
            if ($event.type -eq "error" -or $event.type -eq "response.failed") {
                Write-Host "ERROR $eventJson"
                exit 3
            }
            if ($event.type -eq "response.completed") {
                Write-Host "COMPLETED response_id=$($event.response.id)"
                Write-Host "OUTPUT $outputText"
                if ($null -ne $event.response.usage) {
                    Write-Host "USAGE $($event.response.usage | ConvertTo-Json -Compress -Depth 10)"
                }
                break
            }
        } finally {
            $message.Dispose()
        }
    }
} finally {
    if ($socket.State -eq [Net.WebSockets.WebSocketState]::Open) {
        [void]$socket.CloseAsync(
            [Net.WebSockets.WebSocketCloseStatus]::NormalClosure,
            "demo-complete",
            [Threading.CancellationToken]::None
        ).GetAwaiter().GetResult()
    }
    $timeout.Dispose()
    $socket.Dispose()
}
