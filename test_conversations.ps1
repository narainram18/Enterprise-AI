$baseUrl = "http://localhost:8080/api"
$global:testResults = [System.Collections.ArrayList]::new()
$totalTests = 0
$passedTests = 0

function Add-TestResult($name, $expectedStatus, $actualStatus, $passed, $details) {
    $global:totalTests++
    if ($passed) { $global:passedTests++ }
    $null = $global:testResults.Add([PSCustomObject]@{
        TestName = $name
        Expected = $expectedStatus
        Actual = $actualStatus
        Passed = $passed
        Details = $details
    })
}

function Invoke-Api {
    param(
        [string]$Uri,
        [string]$Method,
        [hashtable]$Headers = @{},
        [string]$Body = $null
    )
    $params = @{
        Uri = $Uri
        Method = $Method
        Headers = $Headers
    }
    if ($Body) {
        $params.Body = $Body
        $params.ContentType = "application/json"
    }

    try {
        $response = Invoke-WebRequest @params -UseBasicParsing
        $data = $null
        if ($response.Content) {
            $data = $response.Content | ConvertFrom-Json
        }
        return @{ Status = [int]$response.StatusCode; Data = $data }
    } catch {
        $status = 500
        $data = $null
        if ($_.Exception.Response) {
            $status = [int]$_.Exception.Response.StatusCode
            try {
                $stream = $_.Exception.Response.GetResponseStream()
                $reader = New-Object System.IO.StreamReader($stream)
                $content = $reader.ReadToEnd()
                if ($content) {
                    $data = $content | ConvertFrom-Json
                }
            } catch {}
        }
        return @{ Status = $status; Data = $data }
    }
}

# 1. Register User 1
$user1 = "user1_$([guid]::NewGuid().ToString().Substring(0,8))@example.com"
$reg1Body = @{ name = "User One"; email = $user1; password = "password123" } | ConvertTo-Json
$resReg1 = Invoke-Api -Uri "$baseUrl/auth/register" -Method Post -Body $reg1Body

# 2. Login User 1
$login1Body = @{ email = $user1; password = "password123" } | ConvertTo-Json
$resLog1 = Invoke-Api -Uri "$baseUrl/auth/login" -Method Post -Body $login1Body
$token1 = $resLog1.Data.data.token
$headers1 = @{ Authorization = "Bearer $token1" }

# 3. Test unprotected access
$resNoAuth = Invoke-Api -Uri "$baseUrl/conversations" -Method Get
Add-TestResult "No JWT Access" 401 $resNoAuth.Status ($resNoAuth.Status -eq 401 -or $resNoAuth.Status -eq 403) "Expected 401/403 for missing JWT"

# 4. Create conversation
$createBody = @{ title = "My Test Chat" } | ConvertTo-Json
$resCreate = Invoke-Api -Uri "$baseUrl/conversations" -Method Post -Headers $headers1 -Body $createBody
$convId = $resCreate.Data.data.id
Add-TestResult "Create Conversation" 201 $resCreate.Status ($resCreate.Status -eq 201) "Created conv ID: $convId"

# 5. List conversations
$resList = Invoke-Api -Uri "$baseUrl/conversations" -Method Get -Headers $headers1
$found = ($resList.Data.data.content | Where-Object { $_.id -eq $convId }) -ne $null
Add-TestResult "List Conversations" 200 $resList.Status (($resList.Status -eq 200) -and $found) "Found created conversation in list"

# 6. Get conversation
$resGet = Invoke-Api -Uri "$baseUrl/conversations/$convId" -Method Get -Headers $headers1
Add-TestResult "Get Conversation" 200 $resGet.Status ($resGet.Status -eq 200) "Retrieved conversation ID: $($resGet.Data.data.id)"

# 7. Rename conversation
$renameBody = @{ title = "Renamed Chat" } | ConvertTo-Json
$resRename = Invoke-Api -Uri "$baseUrl/conversations/$convId" -Method Patch -Headers $headers1 -Body $renameBody
Add-TestResult "Rename Conversation" 200 $resRename.Status (($resRename.Status -eq 200) -and ($resRename.Data.data.title -eq "Renamed Chat")) "New title: $($resRename.Data.data.title)"

# 8. Add message
$msgBody = @{ content = "Hello AI" } | ConvertTo-Json
$resMsg = Invoke-Api -Uri "$baseUrl/conversations/$convId/messages" -Method Post -Headers $headers1 -Body $msgBody
$msgId = $resMsg.Data.data.id
Add-TestResult "Add Message" 201 $resMsg.Status ($resMsg.Status -eq 201) "Created message ID: $msgId"

# 9. Get messages
$resMsgs = Invoke-Api -Uri "$baseUrl/conversations/$convId/messages" -Method Get -Headers $headers1
$foundMsg = ($resMsgs.Data.data | Where-Object { $_.id -eq $msgId }) -ne $null
Add-TestResult "Get Messages" 200 $resMsgs.Status (($resMsgs.Status -eq 200) -and $foundMsg) "Found message in conversation"

# 10. Register User 2
$user2 = "user2_$([guid]::NewGuid().ToString().Substring(0,8))@example.com"
$reg2Body = @{ name = "User Two"; email = $user2; password = "password123" } | ConvertTo-Json
Invoke-Api -Uri "$baseUrl/auth/register" -Method Post -Body $reg2Body | Out-Null

# 11. Login User 2
$login2Body = @{ email = $user2; password = "password123" } | ConvertTo-Json
$resLog2 = Invoke-Api -Uri "$baseUrl/auth/login" -Method Post -Body $login2Body
$token2 = $resLog2.Data.data.token
$headers2 = @{ Authorization = "Bearer $token2" }

# 12. Ownership security tests
$resGetSec = Invoke-Api -Uri "$baseUrl/conversations/$convId" -Method Get -Headers $headers2
Add-TestResult "Security: GET other conv" 404 $resGetSec.Status ($resGetSec.Status -eq 404) "Expected 404 for User 2 accessing User 1 conv"

$resPatchSec = Invoke-Api -Uri "$baseUrl/conversations/$convId" -Method Patch -Headers $headers2 -Body $renameBody
Add-TestResult "Security: PATCH other conv" 404 $resPatchSec.Status ($resPatchSec.Status -eq 404) "Expected 404 for User 2 updating User 1 conv"

$resDelSec = Invoke-Api -Uri "$baseUrl/conversations/$convId" -Method Delete -Headers $headers2
Add-TestResult "Security: DELETE other conv" 404 $resDelSec.Status ($resDelSec.Status -eq 404) "Expected 404 for User 2 deleting User 1 conv"

$resMsgSec = Invoke-Api -Uri "$baseUrl/conversations/$convId/messages" -Method Post -Headers $headers2 -Body $msgBody
Add-TestResult "Security: POST msg to other conv" 404 $resMsgSec.Status ($resMsgSec.Status -eq 404) "Expected 404 for User 2 messaging User 1 conv"

# 13. Delete Conversation (User 1)
$resDelete = Invoke-Api -Uri "$baseUrl/conversations/$convId" -Method Delete -Headers $headers1
Add-TestResult "Delete Conversation" 200 $resDelete.Status ($resDelete.Status -eq 200) "Deleted conversation"

# 14. Verify Deletion
$resGetDel = Invoke-Api -Uri "$baseUrl/conversations/$convId" -Method Get -Headers $headers1
Add-TestResult "Verify Deletion" 404 $resGetDel.Status ($resGetDel.Status -eq 404) "Expected 404 after deletion"

# Output Report
$testResults | ConvertTo-Json -Depth 4
