$ErrorActionPreference = 'Stop'

function Write-SrsLog {
    param([string]$Message)

    if (-not [string]::IsNullOrWhiteSpace($script:SrsLogPath)) {
        $timestamp = Get-Date -Format 'yyyy-MM-dd HH:mm:ss'
        Add-Content -LiteralPath $script:SrsLogPath -Encoding UTF8 -Value "[$timestamp] $Message"
    }
}

function Normalize-ParagraphText {
    param([string]$Text)
    return ($Text -replace "`r", '' -replace [char]7, '' -replace "`t", ' ').Trim()
}

function Get-ParagraphIndexByText {
    param(
        $Doc,
        [string]$Text,
        [int]$Occurrence = 1
    )

    $count = 0
    for ($i = 1; $i -le $Doc.Paragraphs.Count; $i++) {
        $current = Normalize-ParagraphText $Doc.Paragraphs.Item($i).Range.Text
        if ($current -eq $Text) {
            $count++
            if ($count -eq $Occurrence) {
                return $i
            }
        }
    }

    throw "Paragraph not found: $Text (occurrence $Occurrence)"
}

function Get-ParagraphIndexByTextOrNull {
    param(
        $Doc,
        [string]$Text,
        [int]$Occurrence = 1
    )

    $count = 0
    for ($i = 1; $i -le $Doc.Paragraphs.Count; $i++) {
        $current = Normalize-ParagraphText $Doc.Paragraphs.Item($i).Range.Text
        if ($current -eq $Text) {
            $count++
            if ($count -eq $Occurrence) {
                return $i
            }
        }
    }

    return $null
}

function Get-ParagraphIndexByPattern {
    param(
        $Doc,
        [string]$Pattern,
        [int]$Occurrence = 1
    )

    $count = 0
    for ($i = 1; $i -le $Doc.Paragraphs.Count; $i++) {
        $current = Normalize-ParagraphText $Doc.Paragraphs.Item($i).Range.Text
        if ($current -match $Pattern) {
            $count++
            if ($count -eq $Occurrence) {
                return $i
            }
        }
    }

    throw "Paragraph pattern not found: $Pattern (occurrence $Occurrence)"
}

function Set-ParagraphTextByExactMatch {
    param(
        $Doc,
        [string]$FindText,
        [string]$ReplaceText,
        [int]$Occurrence = 1
    )

    $index = Get-ParagraphIndexByText -Doc $Doc -Text $FindText -Occurrence $Occurrence
    $paragraph = $Doc.Paragraphs.Item($index)
    $paragraph.Range.Text = "$ReplaceText`r"
}

function Set-ParagraphTextByPattern {
    param(
        $Doc,
        [string]$Pattern,
        [string]$ReplaceText,
        [int]$Occurrence = 1
    )

    $index = Get-ParagraphIndexByPattern -Doc $Doc -Pattern $Pattern -Occurrence $Occurrence
    $paragraph = $Doc.Paragraphs.Item($index)
    $paragraph.Range.Text = "$ReplaceText`r"
}

function Set-ParagraphStyleByExactMatch {
    param(
        $Doc,
        [string]$Text,
        [string]$Style,
        [int]$Occurrence = 1
    )

    $index = Get-ParagraphIndexByText -Doc $Doc -Text $Text -Occurrence $Occurrence
    $paragraph = $Doc.Paragraphs.Item($index)
    $paragraph.Range.Style = $Style
}

function Set-HeadingText {
    param(
        $Doc,
        [string]$CurrentText,
        [string]$NewText,
        [int]$Occurrence = 1
    )

    $index = Get-ParagraphIndexByText -Doc $Doc -Text $CurrentText -Occurrence $Occurrence
    $paragraph = $Doc.Paragraphs.Item($index)
    $paragraph.Range.Text = "$NewText`r"
}

function Set-SectionContent {
    param(
        $Doc,
        [string]$HeadingText,
        [string]$NextHeadingText,
        [object[]]$Blocks,
        [int]$HeadingOccurrence = 1,
        [int]$NextHeadingOccurrence = 1
    )

    $startIndex = Get-ParagraphIndexByText -Doc $Doc -Text $HeadingText -Occurrence $HeadingOccurrence
    $startParagraph = $Doc.Paragraphs.Item($startIndex)
    $sectionStart = $startParagraph.Range.End
    if ([string]::IsNullOrWhiteSpace($NextHeadingText)) {
        $sectionEnd = $Doc.Content.End
    }
    else {
        $nextIndex = Get-ParagraphIndexByText -Doc $Doc -Text $NextHeadingText -Occurrence $NextHeadingOccurrence
        $nextParagraph = $Doc.Paragraphs.Item($nextIndex)
        $sectionEnd = $nextParagraph.Range.Start
    }

    $text = (($Blocks | ForEach-Object { $_.Text }) -join "`r") + "`r"
    $Doc.Range($sectionStart, $sectionEnd).Text = $text

    if ([string]::IsNullOrWhiteSpace($NextHeadingText)) {
        $newRange = $Doc.Range($sectionStart, $Doc.Content.End)
    }
    else {
        $nextIndexAfter = Get-ParagraphIndexByText -Doc $Doc -Text $NextHeadingText -Occurrence $NextHeadingOccurrence
        $newRange = $Doc.Range($sectionStart, $Doc.Paragraphs.Item($nextIndexAfter).Range.Start)
    }
    $paragraphs = @()
    for ($i = 1; $i -le $newRange.Paragraphs.Count; $i++) {
        $textValue = Normalize-ParagraphText $newRange.Paragraphs.Item($i).Range.Text
        if ($textValue.Length -gt 0) {
            $paragraphs += $newRange.Paragraphs.Item($i)
        }
    }

    if ($paragraphs.Count -ne $Blocks.Count) {
        throw "Section paragraph count mismatch for heading [$HeadingText]. Expected $($Blocks.Count), actual $($paragraphs.Count)"
    }

    for ($i = 0; $i -lt $Blocks.Count; $i++) {
        $paragraphs[$i].Range.Style = $Blocks[$i].Style
    }
}

function Ensure-TableRows {
    param(
        $Table,
        [int]$RowCount
    )

    while ($Table.Rows.Count -lt $RowCount) {
        $Table.Rows.Add() | Out-Null
    }
}

function Set-TableCell {
    param(
        $Table,
        [int]$Row,
        [int]$Column,
        [string]$Text
    )

    $Table.Cell($Row, $Column).Range.Text = $Text
}

function Set-TableData {
    param(
        $Table,
        [string[][]]$Rows
    )

    Ensure-TableRows -Table $Table -RowCount ($Rows.Count + 1)
    for ($r = 0; $r -lt $Rows.Count; $r++) {
        for ($c = 0; $c -lt $Rows[$r].Count; $c++) {
            Set-TableCell -Table $Table -Row ($r + 2) -Column ($c + 1) -Text $Rows[$r][$c]
        }
    }
}

function New-PlainUmlFallbackPng {
    param(
        [string]$Path,
        [string]$Title,
        [string]$Source
    )

    Add-Type -AssemblyName System.Drawing
    $width = 1400
    $height = 900
    $bitmap = New-Object System.Drawing.Bitmap($width, $height)
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
    $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $graphics.Clear([System.Drawing.Color]::FromArgb(255, 255, 255))

    $titleFont = New-Object System.Drawing.Font('Microsoft YaHei', 26, [System.Drawing.FontStyle]::Bold)
    $bodyFont = New-Object System.Drawing.Font('Consolas', 16, [System.Drawing.FontStyle]::Regular)
    $smallFont = New-Object System.Drawing.Font('Microsoft YaHei', 14, [System.Drawing.FontStyle]::Regular)
    $darkBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(15, 23, 42))
    $mutedBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(71, 85, 105))
    $linePen = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(203, 213, 225), 2)
    $boxBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(248, 250, 252))

    $graphics.DrawString($Title, $titleFont, $darkBrush, 50, 36)
    $graphics.DrawString('UML 图（PlantUML 源生成降级预览）', $smallFont, $mutedBrush, 52, 82)
    $graphics.FillRectangle($boxBrush, 50, 125, 1300, 720)
    $graphics.DrawRectangle($linePen, 50, 125, 1300, 720)

    $lines = ($Source -replace "`r", '').Split("`n") |
        Where-Object { $_ -notmatch '^@start|^@end|^!pragma' } |
        Select-Object -First 34
    $y = 150
    foreach ($line in $lines) {
        $graphics.DrawString($line, $bodyFont, $darkBrush, 78, $y)
        $y += 20
    }

    $bitmap.Save($Path, [System.Drawing.Imaging.ImageFormat]::Png)
    $graphics.Dispose()
    $bitmap.Dispose()
}

function Get-PlantUmlJarPath {
    param([string]$ResourceDir)

    $toolDir = Join-Path $ResourceDir 'generated\tools'
    New-Item -ItemType Directory -Force -Path $toolDir | Out-Null
    $jarPath = Join-Path $toolDir 'plantuml.jar'
    if (Test-Path -LiteralPath $jarPath) {
        return $jarPath
    }

    try {
        Invoke-WebRequest `
            -Uri 'https://github.com/plantuml/plantuml/releases/download/v1.2024.8/plantuml-1.2024.8.jar' `
            -OutFile $jarPath `
            -UseBasicParsing `
            -TimeoutSec 60
        if (Test-Path -LiteralPath $jarPath) {
            return $jarPath
        }
    }
    catch {
        Write-Warning "PlantUML jar download failed, fallback PNG renderer will be used: $($_.Exception.Message)"
    }

    return $null
}

function Render-UmlDiagram {
    param(
        [string]$PlantUmlJar,
        [string]$PumlPath,
        [string]$PngPath,
        [string]$Title,
        [string]$Source
    )

    $rendered = $false
    if (-not [string]::IsNullOrWhiteSpace($PlantUmlJar) -and (Test-Path -LiteralPath $PlantUmlJar)) {
        try {
            $process = Start-Process `
                -FilePath 'java' `
                -ArgumentList @('-DPLANTUML_LIMIT_SIZE=8192', '-jar', $PlantUmlJar, '-tpng', $PumlPath) `
                -NoNewWindow `
                -Wait `
                -PassThru
            if ($process.ExitCode -eq 0 -and (Test-Path -LiteralPath $PngPath)) {
                $rendered = $true
            }
        }
        catch {
            Write-Warning "PlantUML render failed for $PumlPath, fallback PNG renderer will be used: $($_.Exception.Message)"
        }
    }

    if (-not $rendered) {
        New-PlainUmlFallbackPng -Path $PngPath -Title $Title -Source $Source
    }
}

function Get-UmlDiagramSpecs {
    $diagramSources = @()

    $diagramSources += @{
        Key = 'uml_02_overall_usecase'
        Figure = '图2'
        Title = 'GrowthTrace 总体用例图'
        Intro = '图2从总体层面描述 GrowthTrace 拟面向学生用户、AI 服务和系统维护人员提供的核心用例。'
        Source = @'
@startuml
!pragma layout smetana
left to right direction
skinparam packageStyle rectangle
actor "学生用户" as Student
actor "系统维护人员" as Maintainer
actor "AI 服务" as AI
rectangle "GrowthTrace" {
  usecase "注册与登录" as UC1
  usecase "建立成长画像" as UC2
  usecase "维护目标与 requirement" as UC3
  usecase "创建执行任务" as UC4
  usecase "记录成长随记" as UC5
  usecase "触发阶段诊断" as UC6
  usecase "查看 Dashboard 行动总览" as UC7
  usecase "配置运行环境" as UC8
}
Student --> UC1
Student --> UC2
Student --> UC3
Student --> UC4
Student --> UC5
Student --> UC6
Student --> UC7
AI --> UC2
AI --> UC5
AI --> UC6
Maintainer --> UC8
@enduml
'@
    }

    $diagramSources += @{
        Key = 'uml_03_growth_loop_activity'
        Figure = '图3'
        Title = 'GrowthTrace 核心成长闭环活动图'
        Intro = '图3描述系统拟支持的“画像、目标、执行、随记、诊断、纠偏”闭环业务流程。'
        Source = @'
@startuml
start
:完成成长画像建档;
:设定阶段目标;
:拆解 requirement;
:生成或维护执行任务;
:执行任务并打卡;
:记录成长随记;
:AI 抽取草稿;
if (用户确认抽取?) then (是)
  :回流画像与目标要求;
else (否)
  :修改或丢弃草稿;
endif
:触发阶段诊断;
:生成总结、问题与建议;
:形成纠偏动作;
:进入下一轮执行;
stop
@enduml
'@
    }

    $diagramSources += @{
        Key = 'uml_04_actor_external'
        Figure = '图4'
        Title = '系统参与者与外部服务关系图'
        Intro = '图4说明学生用户、维护人员、AI 服务与数据库等外部对象之间的交互边界。'
        Source = @'
@startuml
!pragma layout smetana
skinparam componentStyle rectangle
actor "学生用户" as Student
actor "系统维护人员" as Maintainer
cloud "OpenAI-compatible\nAI 服务" as AI
database "MySQL 数据库" as DB
component "浏览器前端" as Web
component "Spring Boot 后端" as API
Student --> Web
Maintainer --> API : 配置与维护
Web --> API : HTTP/JSON
API --> DB : 读写成长数据
API --> AI : AI 抽取/总结请求
@enduml
'@
    }

    $diagramSources += @{
        Key = 'uml_05_auth_usecase'
        Figure = '图5'
        Title = '用户注册与登录用例图'
        Intro = '图5描述认证模块拟提供的注册、登录、获取当前用户和退出等用例。'
        Source = @'
@startuml
!pragma layout smetana
left to right direction
actor "学生用户" as Student
rectangle "认证模块" {
  usecase "注册账号" as UC1
  usecase "登录系统" as UC2
  usecase "获取当前用户" as UC3
  usecase "退出登录" as UC4
  usecase "校验访问令牌" as UC5
}
Student --> UC1
Student --> UC2
Student --> UC3
Student --> UC4
UC2 --> UC5 : <<include>>
@enduml
'@
    }

    $diagramSources += @{
        Key = 'uml_06_auth_activity'
        Figure = '图6'
        Title = '用户登录认证活动图'
        Intro = '图6描述用户登录、服务端校验、令牌签发及后续访问受控接口的活动流程。'
        Source = @'
@startuml
start
:用户输入账号与密码;
:前端提交登录请求;
:后端校验账号密码;
if (校验通过?) then (是)
  :生成 JWT 令牌;
  :返回用户信息与令牌;
  :前端保存令牌;
  :访问受保护页面;
else (否)
  :返回错误提示;
endif
stop
@enduml
'@
    }

    $diagramSources += @{
        Key = 'uml_07_profile_usecase'
        Figure = '图7'
        Title = '成长画像建档用例图'
        Intro = '图7描述成长画像模块拟提供的自然语言建档、AI 草稿生成、草稿确认和画像维护能力。'
        Source = @'
@startuml
!pragma layout smetana
left to right direction
actor "学生用户" as Student
actor "AI 服务" as AI
rectangle "成长画像模块" {
  usecase "输入自我描述" as UC1
  usecase "生成画像草稿" as UC2
  usecase "编辑草稿" as UC3
  usecase "确认入档" as UC4
  usecase "维护技能与经历" as UC5
  usecase "刷新画像完整度" as UC6
}
Student --> UC1
Student --> UC3
Student --> UC4
Student --> UC5
Student --> UC6
AI --> UC2
UC1 --> UC2 : <<include>>
UC4 --> UC6 : <<include>>
@enduml
'@
    }

    $diagramSources += @{
        Key = 'uml_08_profile_activity'
        Figure = '图8'
        Title = '成长画像建档功能活动图'
        Intro = '图8描述学生用户填写自我描述、生成画像草稿、审核草稿并确认入档的功能活动流程。'
        Source = @'
@startuml
start
:用户填写自我描述;
:系统校验文本完整性;
if (是否请求 AI 画像草稿?) then (是)
  :后端调用 AI 服务;
  :生成画像草稿;
else (否)
  :用户手工填写画像字段;
endif
:展示待确认画像内容;
if (用户确认?) then (是)
  :保存正式成长画像;
  :计算画像完整度;
else (否)
  :继续编辑或暂存草稿;
endif
stop
@enduml
'@
    }

    $diagramSources += @{
        Key = 'uml_09_target_usecase'
        Figure = '图9'
        Title = '目标与 requirement 管理用例图'
        Intro = '图9描述目标模块拟提供的目标创建、主目标设置、requirement 维护和状态更新能力。'
        Source = @'
@startuml
!pragma layout smetana
left to right direction
actor "学生用户" as Student
rectangle "目标管理模块" {
  usecase "创建成长目标" as UC1
  usecase "编辑目标信息" as UC2
  usecase "设为主目标" as UC3
  usecase "新增 requirement" as UC4
  usecase "维护 requirement 状态" as UC5
  usecase "查看目标进度" as UC6
}
Student --> UC1
Student --> UC2
Student --> UC3
Student --> UC4
Student --> UC5
Student --> UC6
@enduml
'@
    }

    $diagramSources += @{
        Key = 'uml_10_target_activity'
        Figure = '图10'
        Title = '目标创建与主目标设置活动图'
        Intro = '图10描述用户创建目标、补充 requirement、选择主目标并进入执行拆解的活动流程。'
        Source = @'
@startuml
start
:选择目标类型;
:填写目标标题、说明和截止日期;
:添加 requirement;
if (是否设为主目标?) then (是)
  :取消其他主目标标记;
  :设置当前目标为主目标;
else (否)
  :保持普通目标;
endif
:保存目标信息;
:进入目标详情;
:选择 requirement 生成任务;
stop
@enduml
'@
    }

    $diagramSources += @{
        Key = 'uml_11_requirement_state'
        Figure = '图11'
        Title = 'requirement 状态流转状态图'
        Intro = '图11描述目标要求从待办、推进中到已达成的状态流转，以及随记确认可能带来的状态更新。'
        Source = @'
@startuml
[*] --> TODO
TODO --> IN_PROGRESS : 生成任务或手动推进
IN_PROGRESS --> MET : 用户确认达成
TODO --> MET : 随记抽取确认达成
MET --> IN_PROGRESS : 复核后重新推进
IN_PROGRESS --> TODO : 调整目标要求
MET --> [*]
@enduml
'@
    }

    $diagramSources += @{
        Key = 'uml_12_task_usecase'
        Figure = '图12'
        Title = '执行任务管理用例图'
        Intro = '图12描述执行任务模块拟支持的 AI 草案生成、任务维护、打卡、证据提交和看板查看能力。'
        Source = @'
@startuml
!pragma layout smetana
left to right direction
actor "学生用户" as Student
actor "AI 服务" as AI
rectangle "执行任务模块" {
  usecase "创建任务" as UC1
  usecase "生成 AI 任务草案" as UC2
  usecase "任务打卡" as UC3
  usecase "提交完成证据" as UC4
  usecase "更新任务状态" as UC5
  usecase "查看任务看板" as UC6
}
Student --> UC1
Student --> UC3
Student --> UC4
Student --> UC5
Student --> UC6
AI --> UC2
UC1 --> UC2 : <<extend>>
UC5 --> UC4 : <<include>>
@enduml
'@
    }

    $diagramSources += @{
        Key = 'uml_13_task_draft_sequence'
        Figure = '图13'
        Title = 'AI 任务草案生成时序图'
        Intro = '图13展示用户从目标要求或诊断建议触发任务草案生成时，前端、后端、AI 服务和数据库的交互过程。'
        Source = @'
@startuml
actor "学生用户" as Student
participant "执行页" as Web
participant "任务服务" as API
participant "目标/要求数据" as Target
participant "AI 服务" as AI
Student -> Web : 点击 AI 生成任务
Web -> API : 提交草案请求
API -> Target : 读取目标与 requirement 上下文
API -> AI : 请求生成任务草案
AI --> API : 返回标题、描述、验收标准
API --> Web : 返回草案
Student -> Web : 确认或修改
Web -> API : 创建任务
@enduml
'@
    }

    $diagramSources += @{
        Key = 'uml_14_task_checkin_activity'
        Figure = '图14'
        Title = '任务打卡与完成证据活动图'
        Intro = '图14描述任务执行过程中打卡、投入记录、完成确认和证据补充的活动流程。'
        Source = @'
@startuml
start
:查看任务卡片;
if (今日是否已打卡?) then (否)
  :填写投入时间;
  :记录打卡日期;
  :任务进入推进状态;
endif
if (准备标记完成?) then (是)
  if (已有打卡或完成证据?) then (是)
    :提交完成证据;
    :任务状态变更为 DONE;
  else (否)
    :提示需打卡或填写证据;
  endif
endif
stop
@enduml
'@
    }

    $diagramSources += @{
        Key = 'uml_15_task_state'
        Figure = '图15'
        Title = '任务状态流转状态图'
        Intro = '图15描述执行任务在待办、进行中、已完成和已放弃之间的状态流转。'
        Source = @'
@startuml
[*] --> TODO
TODO --> IN_PROGRESS : 首次打卡
TODO --> DONE : 填写完成证据
IN_PROGRESS --> DONE : 完成并提交证据
TODO --> ABANDONED : 放弃任务
IN_PROGRESS --> ABANDONED : 放弃任务
ABANDONED --> TODO : 恢复任务
DONE --> IN_PROGRESS : 复核后重开
DONE --> [*]
@enduml
'@
    }

    $diagramSources += @{
        Key = 'uml_16_journal_usecase'
        Figure = '图16'
        Title = '成长随记管理用例图'
        Intro = '图16描述成长随记模块拟提供的记录、编辑、抽取、确认和归档等用例。'
        Source = @'
@startuml
!pragma layout smetana
left to right direction
actor "学生用户" as Student
actor "AI 服务" as AI
rectangle "成长随记模块" {
  usecase "编写随记" as UC1
  usecase "编辑随记" as UC2
  usecase "触发 AI 抽取" as UC3
  usecase "确认抽取草稿" as UC4
  usecase "归档成长事件" as UC5
  usecase "查看随记列表" as UC6
}
Student --> UC1
Student --> UC2
Student --> UC4
Student --> UC6
AI --> UC3
UC4 --> UC5 : <<include>>
@enduml
'@
    }

    $diagramSources += @{
        Key = 'uml_17_journal_extract_sequence'
        Figure = '图17'
        Title = '随记 AI 抽取确认时序图'
        Intro = '图17展示随记文本提交、AI 抽取草稿生成、用户确认并回写相关成长数据的时序。'
        Source = @'
@startuml
actor "学生用户" as Student
participant "随记页" as Web
participant "随记服务" as API
participant "AI 服务" as AI
database "数据库" as DB
Student -> Web : 编写成长随记
Web -> API : 保存随记
API -> DB : 写入原始随记
Student -> Web : 触发抽取
Web -> API : 请求 AI 抽取
API -> AI : 发送随记与目标上下文
AI --> API : 返回技能/事件/要求变更草稿
API --> Web : 展示待确认草稿
Student -> Web : 确认抽取
Web -> API : 提交确认
API -> DB : 回写成长事件与要求状态
@enduml
'@
    }

    $diagramSources += @{
        Key = 'uml_18_journal_confirm_activity'
        Figure = '图18'
        Title = '随记抽取结果确认活动图'
        Intro = '图18描述随记抽取草稿由用户审核、修改、确认或丢弃的活动流程。'
        Source = @'
@startuml
start
:打开随记详情;
if (是否存在抽取草稿?) then (否)
  :触发 AI 抽取;
endif
:查看技能、事件、阻塞与 requirement 影响;
if (草稿可信?) then (是)
  :用户确认;
  :写入画像或目标要求;
else (否)
  :修改草稿或丢弃;
endif
:更新随记抽取状态;
stop
@enduml
'@
    }

    $diagramSources += @{
        Key = 'uml_19_diagnosis_usecase'
        Figure = '图19'
        Title = '阶段诊断用例图'
        Intro = '图19描述阶段诊断模块拟支持的触发诊断、查看结果、复盘和历史查询等用例。'
        Source = @'
@startuml
!pragma layout smetana
left to right direction
actor "学生用户" as Student
actor "AI 服务" as AI
rectangle "阶段诊断模块" {
  usecase "选择诊断窗口" as UC1
  usecase "触发阶段诊断" as UC2
  usecase "查看规则指标" as UC3
  usecase "查看 AI 总结" as UC4
  usecase "记录轻复盘" as UC5
  usecase "查看诊断历史" as UC6
}
Student --> UC1
Student --> UC2
Student --> UC3
Student --> UC4
Student --> UC5
Student --> UC6
AI --> UC4
@enduml
'@
    }

    $diagramSources += @{
        Key = 'uml_20_diagnosis_sequence'
        Figure = '图20'
        Title = '阶段诊断生成时序图'
        Intro = '图20展示阶段诊断从用户触发、后端汇总数据、AI 生成总结到诊断结果落库的时序。'
        Source = @'
@startuml
actor "学生用户" as Student
participant "诊断页" as Web
participant "诊断服务" as API
database "数据库" as DB
participant "指标计算器" as Metrics
participant "AI 服务" as AI
Student -> Web : 选择回看窗口
Web -> API : 触发诊断
API -> DB : 读取画像、目标、任务、随记
API -> Metrics : 计算规则指标
API -> AI : 请求阶段总结与建议
AI --> API : 返回总结、问题、建议
API -> DB : 保存诊断记录
API --> Web : 返回诊断结果
@enduml
'@
    }

    $diagramSources += @{
        Key = 'uml_21_correction_activity'
        Figure = '图21'
        Title = '诊断建议驱动纠偏活动图'
        Intro = '图21描述阶段诊断建议如何被用户确认并转化为下一轮目标或任务调整。'
        Source = @'
@startuml
start
:查看阶段诊断结果;
:阅读重点问题和建议;
if (建议需要落地?) then (是)
  :选择建议或纠偏方向;
  :生成任务草案;
  :用户确认任务;
  :进入执行看板;
else (否)
  :记录轻复盘;
endif
:后续随记和打卡继续积累数据;
stop
@enduml
'@
    }

    $diagramSources += @{
        Key = 'uml_22_dashboard_activity'
        Figure = '图22'
        Title = 'Dashboard 行动总览功能活动图'
        Intro = '图22描述 Dashboard 从汇总成长状态到生成下一步行动入口的功能活动流程。'
        Source = @'
@startuml
start
:用户进入 Dashboard;
:系统汇总画像完整度;
:系统读取主目标和 requirement 状态;
:系统统计本周任务节奏;
:系统检查待确认随记和最新诊断;
if (存在待处理事项?) then (是)
  :生成下一步行动入口;
  :用户跳转到目标、任务、随记或诊断页面;
else (否)
  :展示当前成长概览与持续记录提示;
endif
stop
@enduml
'@
    }

    $diagramSources += @{
        Key = 'uml_23_domain_class'
        Figure = '图23'
        Title = 'GrowthTrace 核心领域类图'
        Intro = '图23给出需求阶段的核心领域对象关系，用于说明后续系统设计中的主要业务实体。'
        Source = @'
@startuml
!pragma layout smetana
class User {
  id
  username
}
class GrowthProfile {
  summary
  completeness
}
class Skill
class Experience
class GrowthTarget {
  title
  status
  isPrimary
}
class TargetRequirement {
  reqName
  status
  progress
}
class GrowthTask {
  title
  status
  priority
}
class Journal {
  content
  mood
}
class JournalExtraction {
  status
}
class StageAssessment {
  metrics
  aiStatus
}
User "1" -- "1" GrowthProfile
GrowthProfile "1" -- "*" Skill
GrowthProfile "1" -- "*" Experience
User "1" -- "*" GrowthTarget
GrowthTarget "1" -- "*" TargetRequirement
TargetRequirement "1" -- "*" GrowthTask
User "1" -- "*" Journal
Journal "1" -- "0..1" JournalExtraction
User "1" -- "*" StageAssessment
@enduml
'@
    }

    return $diagramSources
}

function Ensure-UmlAssets {
    param([string]$ResourceDir)

    $umlDir = Join-Path $ResourceDir 'generated\uml'
    New-Item -ItemType Directory -Force -Path $umlDir | Out-Null
    Remove-Item -Path (Join-Path $umlDir '*.puml') -Force -ErrorAction SilentlyContinue
    Remove-Item -Path (Join-Path $umlDir '*.png') -Force -ErrorAction SilentlyContinue
    $plantUmlJar = Get-PlantUmlJarPath -ResourceDir $ResourceDir
    $diagrams = Get-UmlDiagramSpecs

    foreach ($diagram in $diagrams) {
        $pumlPath = Join-Path $umlDir ($diagram.Key + '.puml')
        $pngPath = Join-Path $umlDir ($diagram.Key + '.png')
        Set-Content -LiteralPath $pumlPath -Value $diagram.Source -Encoding UTF8
        Render-UmlDiagram -PlantUmlJar $plantUmlJar -PumlPath $pumlPath -PngPath $pngPath -Title "$($diagram.Figure) $($diagram.Title)" -Source $diagram.Source
        $diagram.PumlPath = $pumlPath
        $diagram.PngPath = $pngPath
    }

    return $diagrams
}

function Replace-UmlPlaceholdersWithImages {
    param(
        $Doc,
        [object[]]$Diagrams
    )

    foreach ($diagram in $Diagrams) {
        $placeholder = "[[UML:$($diagram.Key)]]"
        $index = Get-ParagraphIndexByTextOrNull -Doc $Doc -Text $placeholder
        if ($null -eq $index) {
            continue
        }
        $paragraph = $Doc.Paragraphs.Item($index)
        $range = $paragraph.Range
        $range.Text = ''
        $shape = $Doc.InlineShapes.AddPicture($diagram.PngPath, $false, $true, $range)
        $shape.LockAspectRatio = $true
        if ($shape.Width -gt 430) {
            $shape.Width = 430
        }
        $shape.Range.ParagraphFormat.Alignment = 1
    }
}

function Insert-UmlFigureCatalogBeforeHeading {
    param(
        $Doc,
        [string]$BeforeHeadingText,
        [object[]]$Diagrams
    )

    $index = Get-ParagraphIndexByText -Doc $Doc -Text $BeforeHeadingText
    $headingRange = $Doc.Paragraphs.Item($index).Range
    $insertText = ''
    foreach ($diagram in $Diagrams) {
        $insertText += "$($diagram.Intro)`r"
        $insertText += "[[UML:$($diagram.Key)]]`r"
        $insertText += "$($diagram.Figure) $($diagram.Title)`r"
    }
    $headingRange.InsertBefore($insertText)

    foreach ($diagram in $Diagrams) {
        Set-ParagraphStyleByExactMatch -Doc $Doc -Text $diagram.Intro -Style '正文'
        Set-ParagraphStyleByExactMatch -Doc $Doc -Text "$($diagram.Figure) $($diagram.Title)" -Style '正文'
        $captionIndex = Get-ParagraphIndexByText -Doc $Doc -Text "$($diagram.Figure) $($diagram.Title)"
        $Doc.Paragraphs.Item($captionIndex).Range.ParagraphFormat.Alignment = 1
    }
    Replace-UmlPlaceholdersWithImages -Doc $Doc -Diagrams $Diagrams
}

function Get-DiagramsByKey {
    param(
        [object[]]$Diagrams,
        [string[]]$Keys
    )

    $result = @()
    foreach ($key in $Keys) {
        $diagram = $Diagrams | Where-Object { $_.Key -eq $key } | Select-Object -First 1
        if ($null -eq $diagram) {
            throw "UML diagram not found: $key"
        }
        $result += $diagram
    }
    return $result
}

function New-UmlFigureBlocks {
    param(
        [object[]]$Diagrams
    )

    $blocks = @()
    foreach ($diagram in $Diagrams) {
        $blocks += @{ Style = '正文'; Text = $diagram.Intro }
        $blocks += @{ Style = '正文'; Text = "[[UML:$($diagram.Key)]]" }
        $blocks += @{ Style = '正文'; Text = "$($diagram.Figure) $($diagram.Title)" }
    }
    return $blocks
}

function Format-UmlCaptions {
    param(
        $Doc,
        [object[]]$Diagrams
    )

    foreach ($diagram in $Diagrams) {
        $caption = "$($diagram.Figure) $($diagram.Title)"
        $index = Get-ParagraphIndexByTextOrNull -Doc $Doc -Text $caption
        if ($null -ne $index) {
            $paragraph = $Doc.Paragraphs.Item($index)
            $paragraph.Range.Style = '正文'
            $paragraph.Range.ParagraphFormat.Alignment = 1
        }
    }
}

function Set-DocumentDateLine {
    param(
        $Doc,
        [string]$DateText
    )

    Set-ParagraphTextByExactMatch -Doc $Doc -FindText '二○××年×月' -ReplaceText $DateText
}

function Populate-Document {
    param(
        $Doc,
        [string]$VersionLabel,
        [string]$VersionCode,
        [string]$DateText,
        [string]$RecordDate
    )

    Write-SrsLog "Populate: cover and member tables"
    $coverTable = $Doc.Tables.Item(1)
    Set-TableCell -Table $coverTable -Row 1 -Column 2 -Text 'GT-2026-SEII'
    Set-TableCell -Table $coverTable -Row 2 -Column 2 -Text 'GT-SRS-2026-01'
    Set-TableCell -Table $coverTable -Row 3 -Column 2 -Text '课程内部'

    Set-ParagraphTextByExactMatch -Doc $Doc -FindText '项目名称' -ReplaceText 'GrowthTrace 计算机专业学生成长跟踪与阶段诊断平台'
    Set-ParagraphTextByExactMatch -Doc $Doc -FindText '版本：V1.0' -ReplaceText "版本：$VersionLabel"
    Set-ParagraphTextByExactMatch -Doc $Doc -FindText '软件XXXX班XXXXXX软件公司' -ReplaceText '软件工程课程设计 II GrowthTrace 项目组'
    Set-DocumentDateLine -Doc $Doc -DateText $DateText
    Set-ParagraphTextByExactMatch -Doc $Doc -FindText '“XXXX”软件需求规格说明' -ReplaceText 'GrowthTrace 软件需求规格说明'
    Set-ParagraphStyleByExactMatch -Doc $Doc -Text 'GrowthTrace 软件需求规格说明' -Style '正文'

    $memberTable = $Doc.Tables.Item(2)
    Ensure-TableRows -Table $memberTable -RowCount 8
    $memberRows = @(
        @('', '', '组长', '需求统筹、总体设计、核心闭环规划、联调组织、文档统稿'),
        @('', '', '成员', '画像模块需求分析与页面原型'),
        @('', '', '成员', '目标与 requirement 模块需求分析'),
        @('', '', '成员', '执行任务模块需求分析与交互设计'),
        @('', '', '成员', '成长随记与 AI 抽取场景需求分析'),
        @('', '', '成员', '阶段诊断与 Dashboard 需求分析'),
        @('', '', '成员', '测试方案、验收材料、活动记录整理')
    )
    Set-TableData -Table $memberTable -Rows $memberRows

    $recordTable = $Doc.Tables.Item(3)
    Ensure-TableRows -Table $recordTable -RowCount 6
    $recordRows = @(
        @($VersionCode, '形成课程阶段需求规格说明初稿，明确项目范围、核心闭环与验收基线。', 'GrowthTrace 项目组', '2026-04-21', '初稿'),
        @('V1.0.1', '补充项目背景、业务主线与团队职责说明，统一需求文档口径。', 'GrowthTrace 项目组', '2026-04-22', '需求补充'),
        @('V1.0.2', '完善核心功能需求、参与者、用例一览表与代表性用例说明。', 'GrowthTrace 项目组', '2026-04-23', '功能完善'),
        @('V1.0.3', '补充非功能需求、外部接口、验收标准与课程提交约束。', 'GrowthTrace 项目组', '2026-04-25', '验收补充'),
        @('V1.0.4', '在功能需求章节补充 22 张 UML 图，增强需求分析表达完整性。', 'GrowthTrace 项目组', $RecordDate, 'UML 补充')
    )
    for ($r = 0; $r -lt $recordRows.Count; $r++) {
        for ($c = 0; $c -lt $recordRows[$r].Count; $c++) {
            Set-TableCell -Table $recordTable -Row ($r + 2) -Column ($c + 1) -Text $recordRows[$r][$c]
        }
    }

    Write-SrsLog "Populate: introduction sections"
    Set-SectionContent -Doc $Doc -HeadingText '编写目的' -NextHeadingText '项目资料' -Blocks @(
        @{ Style = '正文'; Text = '本文档用于说明 GrowthTrace 计算机专业学生成长跟踪与阶段诊断平台在课程设计阶段拟满足的功能需求、非功能需求及验收基线。' },
        @{ Style = '正文'; Text = '本文档将作为后续概要设计、详细设计、开发实现、测试验收和课程答辩准备的统一依据，确保项目组成员对系统建设目标和边界形成一致理解。' },
        @{ Style = '正文'; Text = '本文档的预期读者包括项目组成员、指导教师、课程评审人员以及后续参与测试和验收的相关人员。' }
    )

    Set-SectionContent -Doc $Doc -HeadingText '项目资料' -NextHeadingText '术语定义' -Blocks @(
        @{ Style = '列出段落'; Text = '项目名称：GrowthTrace 计算机专业学生成长跟踪与阶段诊断平台；' },
        @{ Style = '列出段落'; Text = '项目编号：GT-2026-SEII；' },
        @{ Style = '列出段落'; Text = '项目性质：软件工程课程设计 II 团队项目；' },
        @{ Style = '列出段落'; Text = '建设目标：面向计算机专业学生构建成长闭环管理系统，用于后续开发、展示与课程验收；' },
        @{ Style = '列出段落'; Text = '拟采用架构：B/S 架构，前后端分离实现；' },
        @{ Style = '列出段落'; Text = '计划阶段提交时间：2026 年 5 月 1 日。' }
    )

    Set-ParagraphTextByExactMatch -Doc $Doc -FindText '本文中用到的专门术语定义见表1。' -ReplaceText '本文中涉及的核心业务术语定义见表1，用于统一需求阶段的业务口径。'
    Set-ParagraphTextByExactMatch -Doc $Doc -FindText '如果没有，本节直接请写“无”。' -ReplaceText '术语表可在后续设计阶段继续补充，本版本先给出核心术语。'
    $termTable = $Doc.Tables.Item(4)
    $termRows = @(
        @('1', '成长画像', '系统拟维护的学生技能、经历、方向与阶段状态信息集合。'),
        @('2', 'requirement', '目标下用于描述阶段性要求、里程碑或衡量标准的条目。'),
        @('3', '成长随记', '学生在学习或实践过程中的阶段记录，用于沉淀成长证据。'),
        @('4', '阶段诊断', '系统依据成长数据与 AI 辅助分析生成的阶段总结、问题识别与行动建议。'),
        @('5', '纠偏动作', '根据诊断结论拟调整的任务、方向或执行策略。')
    )
    Set-TableData -Table $termTable -Rows $termRows

    Set-ParagraphTextByExactMatch -Doc $Doc -FindText '本文件中用到的英文缩写说明见表 2 。如果没有，请写“无”。' -ReplaceText '本文件中使用的主要英文缩写说明见表2。'
    $abbrTable = $Doc.Tables.Item(5)
    $abbrRows = @(
        @('1', 'B/S', 'Browser / Server'),
        @('2', 'JWT', 'JSON Web Token'),
        @('3', 'API', 'Application Programming Interface'),
        @('4', 'ER', 'Entity Relationship'),
        @('5', 'UI', 'User Interface')
    )
    Set-TableData -Table $abbrTable -Rows $abbrRows
    Set-ParagraphTextByExactMatch -Doc $Doc -FindText '如果没有，本节直接请写“无”。' -ReplaceText '缩写表后续可根据设计与测试文档继续扩充。'

    Set-ParagraphTextByExactMatch -Doc $Doc -FindText '本文引用的文档及标准参见表 3。' -ReplaceText '本文引用的课程文件、模板文件及项目内部资料见表3。'
    Set-ParagraphTextByExactMatch -Doc $Doc -FindText '填表说明：' -ReplaceText '表3 填写说明：'
    $referenceTable = $Doc.Tables.Item(6)
    $referenceRows = @(
        @('SEII-2024', '软件工程课程设计 II 课程实施说明2024', 'V1.0', '2024-01', '课程组'),
        @('SRS-TPL-2022', '软件需求规格说明书（SRS）模板', 'V1.0', '2022-01', '课程组'),
        @('GT-SCOPE-2026', 'GrowthTrace SRS 最终功能范围草案', 'V1.0', '2026-04', 'GrowthTrace 项目组')
    )
    Set-TableData -Table $referenceTable -Rows $referenceRows

    Set-SectionContent -Doc $Doc -HeadingText '项目背景' -NextHeadingText '组织机构与职责' -Blocks @(
        @{ Style = '正文'; Text = 'GrowthTrace 拟面向计算机专业学生的日常成长管理场景进行建设，重点解决成长记录分散、阶段目标不清晰、执行过程难追踪以及复盘难沉淀的问题。' },
        @{ Style = '正文'; Text = '项目计划以“画像 -> 目标 -> requirement -> 任务 -> 随记 -> AI 抽取确认 -> 阶段诊断 -> 纠偏动作 -> 再执行”为核心主线，构建可持续迭代的成长闭环。' },
        @{ Style = '正文'; Text = '本项目将作为软件工程课程设计 II 的团队课题展开，后续开发工作需围绕课程提交、阶段验收和答辩展示逐步推进。' }
    )

    Set-SectionContent -Doc $Doc -HeadingText '组织机构与职责' -NextHeadingText '岗位角色' -Blocks @(
        @{ Style = '正文'; Text = '本项目拟由 7 名成员组成课程设计项目组，设组长 1 名，其他成员按模块需求分析、界面设计、后端逻辑、测试与文档等方向协同推进。' },
        @{ Style = '正文'; Text = '组长负责需求统筹、范围控制、总体设计协调、关键模块方案确定、联调组织和文档统稿；其余成员依据分工承担对应模块的需求细化、原型设计、开发准备和测试材料整理。' },
        @{ Style = '正文'; Text = '项目组将在需求阶段、设计阶段、实现阶段和验收阶段分别组织讨论，并通过会议纪要、任务分配和阶段成果物保证项目推进。' }
    )

    $roleTable = $Doc.Tables.Item(7)
    $roleRows = @(
        @('组长', '项目组', '负责需求统筹、总体设计协调、范围控制、关键路径推进与文档统稿。'),
        @('模块成员', '项目组', '负责具体模块的需求细化、界面设计、实现准备与阶段材料补充。'),
        @('测试与文档成员', '项目组', '负责测试方案整理、验收材料汇总、活动记录整理与提交包检查。')
    )
    Set-TableData -Table $roleTable -Rows $roleRows

    Set-ParagraphTextByExactMatch -Doc $Doc -FindText '每个岗位的职责可以进行详细的描述，建议采用表格的形式：' -ReplaceText '本项目的关键岗位角色与职责见表4。'
    Set-ParagraphTextByExactMatch -Doc $Doc -FindText '用户环境中的企业岗位或角色，和组织机构一样，也是分析人员理解企业业务的基础，是需求获取的基础工作，同时也是分析人员提取对象的基础。' -ReplaceText '课程项目虽不涉及企业岗位体系，但仍需在需求阶段明确团队内部职责边界，以支撑后续分工实施。'
    Set-ParagraphTextByExactMatch -Doc $Doc -FindText '对岗位角色的识别也包括使用了计算机系统后的系统管理和维护人员。' -ReplaceText '若后续需要引入更多展示、测试或答辩支持角色，可在迭代版文档中继续补充。'

    Set-SectionContent -Doc $Doc -HeadingText '业务流程' -NextHeadingText '统计报表' -Blocks @(
        @{ Style = '正文'; Text = '本项目拟采用闭环业务流程组织系统功能。学生用户首先完成成长画像建档，在此基础上设定阶段目标与 requirement，并进一步拆解为可执行任务。' },
        @{ Style = '正文'; Text = '在执行过程中，学生用户可持续记录成长随记，系统拟借助 AI 服务完成随记内容与画像信息的辅助抽取，抽取结果需经用户确认后才进入正式数据。' },
        @{ Style = '正文'; Text = '当积累到一定周期后，系统将触发阶段诊断，结合画像、目标、任务与随记信息形成阶段总结、问题识别和下一步行动建议，进而推动用户进入下一轮执行。' }
    )

    Set-SectionContent -Doc $Doc -HeadingText '统计报表' -NextHeadingText '系统体系结构' -Blocks @(
        @{ Style = '正文'; Text = '系统后续拟围绕 Dashboard 页面、周进度统计、诊断历史以及成长趋势视图输出主要统计信息，用于展示用户阶段状态和行动重点。' },
        @{ Style = '正文'; Text = '统计结果将重点覆盖画像完整度、当前主目标、requirement 推进情况、任务完成情况、成长随记数量与阶段诊断结果等关键指标。' },
        @{ Style = '正文'; Text = '详细图表样式、统计口径和导出方式将在概要设计和界面设计阶段进一步细化。' }
    )

    Set-SectionContent -Doc $Doc -HeadingText '物理架构' -NextHeadingText '网络环节' -Blocks @(
        @{ Style = '正文'; Text = '本项目计划采用 B/S 物理架构，由浏览器端承担用户交互与展示逻辑，后端服务负责业务处理、权限校验、数据存储与 AI 服务适配。' },
        @{ Style = '正文'; Text = '系统拟以 Vue 前端、Spring Boot 后端、MySQL 数据库和 OpenAI-compatible AI 服务接口构成基础技术栈。' },
        @{ Style = '正文'; Text = '需求阶段暂不对物理部署图进行细节展开，图1 仅作为后续概要设计阶段补充正式架构图的预留位置。' }
    )
    Set-SectionContent -Doc $Doc -HeadingText '网络环节' -NextHeadingText '运行环境' -Blocks @(
        @{ Style = '正文'; Text = '系统计划在课程实验环境或本地开发环境中部署运行，浏览器端通过 HTTP/HTTPS 与后端服务通信，后端服务再访问数据库和外部 AI 接口。' },
        @{ Style = '正文'; Text = '对于课程展示场景，系统拟优先保证局域网或单机环境下的稳定运行；若后续有需要，可补充云端部署方案。' }
    )

    Set-SectionContent -Doc $Doc -HeadingText '运行环境' -NextHeadingText '功能需求' -Blocks @(
        @{ Style = '正文'; Text = '开发环境拟采用 Windows 11 或同等桌面操作系统，使用 Java 21、Node.js 20 及以上版本、MySQL 8.x 和 Maven / npm 工具链。' },
        @{ Style = '正文'; Text = '运行环境拟支持主流 Chromium 内核浏览器，后端部署在支持 JDK 21 的服务器或实验机上，数据库部署在本地或课程实验数据库环境中。' },
        @{ Style = '正文'; Text = '若后续接入其他兼容模型平台，其接口应满足 OpenAI-compatible 调用规范。' }
    )

    Set-ParagraphTextByExactMatch -Doc $Doc -FindText '本节介绍项目的立项背景、客户的组织机构构成、岗位设置及现有的业务及处理流程。' -ReplaceText '本节用于概述项目背景、团队组织方式、预期业务流程以及后续计划呈现的统计信息。'
    Set-ParagraphTextByExactMatch -Doc $Doc -FindText '本节介绍待开发系统的体系结构及运行环境。' -ReplaceText '本节用于说明系统拟采用的体系结构、网络交互方式与运行环境。'
    Set-ParagraphTextByExactMatch -Doc $Doc -FindText '以下应分节描述XXXX软件各功能的具体需求。' -ReplaceText '以下分节描述 GrowthTrace 在课程设计阶段拟满足的核心功能需求。'
    Set-ParagraphStyleByExactMatch -Doc $Doc -Text '本节用于概述项目背景、团队组织方式、预期业务流程以及后续计划呈现的统计信息。' -Style '正文'
    Set-ParagraphStyleByExactMatch -Doc $Doc -Text '本节用于说明系统拟采用的体系结构、网络交互方式与运行环境。' -Style '正文'
    Set-ParagraphStyleByExactMatch -Doc $Doc -Text '以下分节描述 GrowthTrace 在课程设计阶段拟满足的核心功能需求。' -Style '正文'

    Write-SrsLog "Populate: functional requirements"
    Set-ParagraphTextByExactMatch -Doc $Doc -FindText '参与者（Actor）指与系统产生交互的外部用户或者外部系统。xxxx的系统参与者见表5所示。' -ReplaceText '系统参与者是指与 GrowthTrace 发生直接交互的外部用户或外部服务，主要参与者定义见表5。'
    $actorTable = $Doc.Tables.Item(8)
    $actorRows = @(
        @('学生用户', '主要', '系统的核心使用者，负责建档、设定目标、记录随记、执行任务和查看诊断结果。'),
        @('AI 服务', '次要', '为画像抽取、随记抽取与阶段总结提供辅助生成能力。'),
        @('系统维护人员', '次要', '负责本地环境配置、演示准备和运行维护支持。')
    )
    Set-TableData -Table $actorTable -Rows $actorRows
    Set-ParagraphTextByExactMatch -Doc $Doc -FindText '其中，' -ReplaceText '其中：'

    Set-ParagraphTextByExactMatch -Doc $Doc -FindText '用文字简述描述XXXX软件的总体功能（指第一层软件功能，更详细的子功能在各功能单元介绍，在此不用描述），然后以用例图形式描述软件功能组成，并以表格形式汇总各功能说明。' -ReplaceText 'GrowthTrace 拟由认证、成长画像、目标管理、执行任务、成长随记、阶段诊断和 Dashboard 总览七个一级模块构成。4.2 仅描述总体功能构成和系统边界，4.3 起逐个用例描述具体需求。'
    Set-ParagraphTextByExactMatch -Doc $Doc -FindText '对于中小规模系统，可以直接给出完整系统用例图，并给出用例一览表。对于复杂系统，可采用UML包的机制分层描述。这里仅给出第一层的用例图，在后面各章节中在具体给出对应的用例图和用例一览表。' -ReplaceText '考虑到课程阶段以需求规格说明为主，本版本在 4.2 给出总体用例、核心闭环和外部服务关系图，并在 4.3 至 4.9 中分别给出各用例的具体需求和功能活动图。'
    Set-ParagraphTextByExactMatch -Doc $Doc -FindText 'XXXX软件的功能构成如图2所示，' -ReplaceText 'GrowthTrace 的总体功能构成如图2至图4所示。'
    Set-ParagraphTextByPattern -Doc $Doc -Pattern '^图2.*用例图$' -ReplaceText '总体 UML 图见图2至图4，各用例功能活动图见 4.3 至 4.9。'
    Set-ParagraphTextByExactMatch -Doc $Doc -FindText '填表说明：' -ReplaceText '表6 填写说明：'
    Set-ParagraphTextByExactMatch -Doc $Doc -FindText '用例标识符：XXX_UC_子系统ID_用例序号' -ReplaceText '用例标识符：GT_UC_模块标识_序号。'
    Set-ParagraphTextByExactMatch -Doc $Doc -FindText '需求描述：说明XXXX功能的用途。' -ReplaceText '需求描述：说明该用例在成长闭环中的业务作用。'
    Set-ParagraphTextByExactMatch -Doc $Doc -FindText '如需要，可以活动图的形式进一步描述用户使用XXXX软件的功能进行各业务活动的工作流程，并针对软件业务流程中的活动进行软件总体流程描述。' -ReplaceText '各用例的功能活动图放入对应的用例小节，便于从需求说明直接追踪到业务活动流程。'
    Set-ParagraphTextByExactMatch -Doc $Doc -FindText '下面，逐个用例描述其具体需求' -ReplaceText '下面从 4.3 开始逐个用例描述具体需求。'
    Set-ParagraphStyleByExactMatch -Doc $Doc -Text '下面从 4.3 开始逐个用例描述具体需求。' -Style '正文'

    $useCaseTable = $Doc.Tables.Item(9)
    $useCaseRows = @(
        @('1', '用户注册与登录', 'GT_UC_AUTH_01', '为系统使用建立身份认证基础。'),
        @('2', '新建成长画像并生成画像草稿', 'GT_UC_PROFILE_01', '支持用户通过自然语言建档并生成待确认画像草稿。'),
        @('3', '维护目标与 requirement', 'GT_UC_TARGET_01', '支持用户设定阶段目标、维护 requirement 并标记主目标。'),
        @('4', '创建 AI 指引执行任务并跟踪完成证据', 'GT_UC_EXECUTION_01', '支持围绕目标 requirement 生成任务草案、记录过程证据并跟踪执行状态。'),
        @('5', '记录成长随记并抽取草稿', 'GT_UC_JOURNAL_01', '支持沉淀成长记录并生成待确认抽取结果。'),
        @('6', '触发阶段诊断并查看结果', 'GT_UC_DIAGNOSIS_01', '支持基于阶段数据生成诊断总结和下一步建议。'),
        @('7', '查看 Dashboard 行动总览', 'GT_UC_DASHBOARD_01', '集中展示当前状态、待处理事项与下一步行动入口。')
    )
    Set-TableData -Table $useCaseTable -Rows $useCaseRows

    $generalDiagrams = Get-DiagramsByKey -Diagrams $script:UmlDiagrams -Keys @(
        'uml_02_overall_usecase',
        'uml_03_growth_loop_activity',
        'uml_04_actor_external'
    )

    Set-HeadingText -Doc $Doc -CurrentText 'XXXX（用例标示符）' -NewText '用户注册与登录（GT_UC_AUTH_01）' -Occurrence 1
    Set-ParagraphStyleByExactMatch -Doc $Doc -Text '用户注册与登录（GT_UC_AUTH_01）' -Style '标题 2'
    Insert-UmlFigureCatalogBeforeHeading -Doc $Doc -BeforeHeadingText '用户注册与登录（GT_UC_AUTH_01）' -Diagrams $generalDiagrams

    $authDiagrams = Get-DiagramsByKey -Diagrams $script:UmlDiagrams -Keys @('uml_05_auth_usecase', 'uml_06_auth_activity')
    $profileDiagrams = Get-DiagramsByKey -Diagrams $script:UmlDiagrams -Keys @('uml_07_profile_usecase', 'uml_08_profile_activity')
    $targetDiagrams = Get-DiagramsByKey -Diagrams $script:UmlDiagrams -Keys @('uml_09_target_usecase', 'uml_10_target_activity', 'uml_11_requirement_state')
    $executionDiagrams = Get-DiagramsByKey -Diagrams $script:UmlDiagrams -Keys @('uml_12_task_usecase', 'uml_13_task_draft_sequence', 'uml_14_task_checkin_activity', 'uml_15_task_state')
    $journalDiagrams = Get-DiagramsByKey -Diagrams $script:UmlDiagrams -Keys @('uml_16_journal_usecase', 'uml_17_journal_extract_sequence', 'uml_18_journal_confirm_activity')
    $diagnosisDiagrams = Get-DiagramsByKey -Diagrams $script:UmlDiagrams -Keys @('uml_19_diagnosis_usecase', 'uml_20_diagnosis_sequence', 'uml_21_correction_activity')
    $dashboardDiagrams = Get-DiagramsByKey -Diagrams $script:UmlDiagrams -Keys @('uml_22_dashboard_activity', 'uml_23_domain_class')

    $useCaseDetailBlocks = @(
        @{ Style = '正文'; Text = '本节描述学生用户进入 GrowthTrace 前完成身份建立、登录认证和受保护页面访问所需满足的需求。' },
        @{ Style = '列出段落'; Text = '用例标示符：GT_UC_AUTH_01' },
        @{ Style = '列出段落'; Text = '用例名称：用户注册与登录' },
        @{ Style = '列出段落'; Text = '范围：系统用例' },
        @{ Style = '列出段落'; Text = '级别：用户目标级别' },
        @{ Style = '列出段落'; Text = '主要角色：学生用户' },
        @{ Style = '列出段落'; Text = '涉众：学生用户、项目组、系统维护人员' },
        @{ Style = '列出段落'; Text = '前置条件：用户能够通过浏览器访问系统登录入口；系统后端和数据库处于可用状态。' },
        @{ Style = '列出段落'; Text = '后置条件：系统为认证通过的用户建立会话令牌，并允许其访问画像、目标、任务、随记、诊断和 Dashboard 等受保护功能。' },
        @{ Style = '列出段落'; Text = '主成功场景：用户注册账号；系统校验用户名和密码规则；用户提交登录信息；后端校验账号密码；系统签发访问令牌；前端保存令牌并进入系统首页。' },
        @{ Style = '列出段落'; Text = '扩展（或替代流程）：若账号已存在、密码错误、令牌过期或用户未登录访问受限接口，系统应返回明确提示并引导用户重新注册或登录。' },
        @{ Style = '列出段落'; Text = '特殊需求：认证信息应与业务数据访问权限绑定，不同用户之间的成长画像、目标、任务、随记和诊断数据不得互相越权访问。' },
        @{ Style = '列出段落'; Text = '技术和数据变元表：用户信息应至少包含用户编号、用户名、密码摘要、创建时间等字段；访问令牌应包含用户身份标识和有效期。' }
    ) + (New-UmlFigureBlocks -Diagrams $authDiagrams) + @(
        @{ Style = '标题 2'; Text = '新建成长画像并生成画像草稿（GT_UC_PROFILE_01）' },
        @{ Style = '正文'; Text = '本节描述学生用户首次进入系统时，围绕成长画像建档与 AI 草稿生成所需满足的需求。' },
        @{ Style = '列出段落'; Text = '用例标示符：GT_UC_PROFILE_01' },
        @{ Style = '列出段落'; Text = '用例名称：新建成长画像并生成画像草稿' },
        @{ Style = '列出段落'; Text = '范围：系统用例' },
        @{ Style = '列出段落'; Text = '级别：用户目标级别' },
        @{ Style = '列出段落'; Text = '主要角色：学生用户' },
        @{ Style = '列出段落'; Text = '涉众：学生用户、项目组、指导教师、AI 服务' },
        @{ Style = '列出段落'; Text = '前置条件：用户已完成注册并成功登录系统。' },
        @{ Style = '列出段落'; Text = '后置条件：系统生成待确认的成长画像草稿，用户可选择确认、修改或暂不提交；确认后形成正式成长画像。' },
        @{ Style = '列出段落'; Text = '主成功场景：用户输入个人成长描述；系统校验输入完整性；系统调用 AI 服务生成画像草稿；系统展示草稿内容；用户编辑或确认后形成正式画像。' },
        @{ Style = '列出段落'; Text = '扩展（或替代流程）：若 AI 服务调用失败，系统应提示失败原因并允许用户重新发起抽取或手工录入关键字段。' },
        @{ Style = '列出段落'; Text = '特殊需求：AI 抽取结果不得直接写入正式画像，必须经过用户确认；错误提示应明确区分配置问题、超时问题和服务异常。' },
        @{ Style = '列出段落'; Text = '技术和数据变元表：画像草稿应包含技能、经历、方向、当前状态等核心字段，后续字段可在详细设计阶段扩展。' }
    ) + (New-UmlFigureBlocks -Diagrams $profileDiagrams) + @(
        @{ Style = '标题 2'; Text = '维护目标与 requirement（GT_UC_TARGET_01）' },
        @{ Style = '正文'; Text = '本节描述学生用户围绕阶段目标和 requirement 进行计划拆解、状态维护与后续任务联动所需满足的需求。' },
        @{ Style = '列出段落'; Text = '用例标示符：GT_UC_TARGET_01' },
        @{ Style = '列出段落'; Text = '用例名称：维护目标与 requirement' },
        @{ Style = '列出段落'; Text = '范围：系统用例' },
        @{ Style = '列出段落'; Text = '级别：用户目标级别' },
        @{ Style = '列出段落'; Text = '主要角色：学生用户' },
        @{ Style = '列出段落'; Text = '涉众：学生用户、项目组、指导教师' },
        @{ Style = '列出段落'; Text = '前置条件：用户已登录系统，可进入目标管理功能。' },
        @{ Style = '列出段落'; Text = '后置条件：系统保存目标与 requirement 信息，并为后续任务、随记抽取和阶段诊断提供关联依据。' },
        @{ Style = '列出段落'; Text = '主成功场景：用户创建阶段目标；系统保存目标基本信息；用户拆分 requirement；系统展示 requirement 状态；用户设置主目标；系统在相关页面提供任务生成和查看入口。' },
        @{ Style = '列出段落'; Text = '扩展（或替代流程）：若 requirement 信息不完整，系统应提示用户补全名称、描述或状态；若用户取消编辑，系统不得保存未确认内容。' },
        @{ Style = '列出段落'; Text = '特殊需求：目标和 requirement 应支持与任务、随记抽取结果、诊断建议形成可追踪关系，避免各功能模块孤立。' },
        @{ Style = '列出段落'; Text = '技术和数据变元表：目标应包含标题、描述、是否主目标、状态等字段；requirement 应包含名称、描述、状态和所属目标。' }
    ) + (New-UmlFigureBlocks -Diagrams $targetDiagrams) + @(
        @{ Style = '标题 2'; Text = '创建 AI 指引执行任务并跟踪完成证据（GT_UC_EXECUTION_01）' },
        @{ Style = '正文'; Text = '本节描述系统将目标 requirement 落地为执行任务，并在执行过程中提供 AI 指引、进度跟踪和完成证据记录所需满足的需求。' },
        @{ Style = '列出段落'; Text = '用例标示符：GT_UC_EXECUTION_01' },
        @{ Style = '列出段落'; Text = '用例名称：创建 AI 指引执行任务并跟踪完成证据' },
        @{ Style = '列出段落'; Text = '范围：系统用例' },
        @{ Style = '列出段落'; Text = '级别：用户目标级别' },
        @{ Style = '列出段落'; Text = '主要角色：学生用户' },
        @{ Style = '列出段落'; Text = '涉众：学生用户、项目组、指导教师、AI 服务' },
        @{ Style = '列出段落'; Text = '前置条件：用户已建立目标或 requirement，或手工输入明确的执行任务意图。' },
        @{ Style = '列出段落'; Text = '后置条件：系统保存经用户确认的任务内容、关联上下文、计划时间、执行状态和过程证据。' },
        @{ Style = '列出段落'; Text = '主成功场景：用户从目标或 Dashboard 进入任务创建；系统带入目标上下文；用户请求 AI 生成任务草案；系统生成包含步骤、验收标准和证据建议的任务草案；用户确认后创建任务；用户在执行中记录进展和完成证据；系统更新任务状态。' },
        @{ Style = '列出段落'; Text = '扩展（或替代流程）：若 AI 服务超时，系统应显示进度与失败提示，并允许用户基于当前上下文重新生成或手工创建任务。' },
        @{ Style = '列出段落'; Text = '特殊需求：任务完成不应只依赖单一勾选动作，系统应鼓励用户补充完成说明、产物链接、截图说明或复盘内容。' },
        @{ Style = '列出段落'; Text = '技术和数据变元表：任务应包含标题、正文、状态、优先级、截止时间、目标编号、requirement 编号、进度记录和完成证据摘要。' }
    ) + (New-UmlFigureBlocks -Diagrams $executionDiagrams) + @(
        @{ Style = '标题 2'; Text = '记录成长随记并抽取草稿（GT_UC_JOURNAL_01）' },
        @{ Style = '正文'; Text = '本节描述学生用户记录成长过程、借助 AI 抽取结构化信息并确认回流到画像和目标要求所需满足的需求。' },
        @{ Style = '列出段落'; Text = '用例标示符：GT_UC_JOURNAL_01' },
        @{ Style = '列出段落'; Text = '用例名称：记录成长随记并抽取草稿' },
        @{ Style = '列出段落'; Text = '范围：系统用例' },
        @{ Style = '列出段落'; Text = '级别：用户目标级别' },
        @{ Style = '列出段落'; Text = '主要角色：学生用户' },
        @{ Style = '列出段落'; Text = '涉众：学生用户、项目组、指导教师、AI 服务' },
        @{ Style = '列出段落'; Text = '前置条件：用户已登录系统，可输入随记内容；若需要关联目标或 requirement，系统中应已存在对应数据。' },
        @{ Style = '列出段落'; Text = '后置条件：系统保存原始随记，并在用户确认后保存抽取出的技能、成长事件、阻塞信息或 requirement 状态影响。' },
        @{ Style = '列出段落'; Text = '主成功场景：用户编写随记；系统保存原始文本；用户触发 AI 抽取；系统生成抽取草稿；用户审核并修改草稿；用户确认后系统回写相关成长数据。' },
        @{ Style = '列出段落'; Text = '扩展（或替代流程）：若抽取结果不准确，用户可修改或丢弃草稿；若 AI 调用失败，系统应保留原始随记并允许稍后重试。' },
        @{ Style = '列出段落'; Text = '特殊需求：随记原文和抽取结果应区分保存，系统不得用未经确认的 AI 草稿直接改变正式画像或目标要求。' },
        @{ Style = '列出段落'; Text = '技术和数据变元表：随记应包含标题、正文、日期、情绪或标签；抽取草稿应包含候选技能、事件、阻塞、相关 requirement 和确认状态。' }
    ) + (New-UmlFigureBlocks -Diagrams $journalDiagrams) + @(
        @{ Style = '标题 2'; Text = '触发阶段诊断并形成下一步行动建议（GT_UC_DIAGNOSIS_01）' },
        @{ Style = '正文'; Text = '本节描述系统围绕阶段诊断与行动建议生成所需满足的需求。' },
        @{ Style = '列出段落'; Text = '用例标示符：GT_UC_DIAGNOSIS_01' },
        @{ Style = '列出段落'; Text = '用例名称：触发阶段诊断并形成下一步行动建议' },
        @{ Style = '列出段落'; Text = '范围：系统用例' },
        @{ Style = '列出段落'; Text = '级别：用户目标级别' },
        @{ Style = '列出段落'; Text = '主要角色：学生用户' },
        @{ Style = '列出段落'; Text = '涉众：学生用户、项目组、指导教师、AI 服务' },
        @{ Style = '列出段落'; Text = '前置条件：系统中已存在一定周期内的画像、目标、任务与随记数据。' },
        @{ Style = '列出段落'; Text = '后置条件：系统生成包含规则指标、阶段总结和建议动作的诊断记录，并可供后续查看历史。' },
        @{ Style = '列出段落'; Text = '主成功场景：用户发起阶段诊断；系统汇总阶段数据；系统计算规则指标；系统调用 AI 服务生成总结与建议；系统展示诊断结果并提供后续行动入口。' },
        @{ Style = '列出段落'; Text = '扩展（或替代流程）：若 AI 生成失败，系统至少应保留规则指标和诊断记录状态，并提示用户稍后重试。' },
        @{ Style = '列出段落'; Text = '特殊需求：诊断结果应体现与目标和任务执行的关联，建议动作应可指导用户继续推进下一轮执行。' },
        @{ Style = '列出段落'; Text = '技术和数据变元表：诊断范围、统计周期、建议粒度和后续任务联动方式可在迭代版本中继续细化。' }
    ) + (New-UmlFigureBlocks -Diagrams $diagnosisDiagrams) + @(
        @{ Style = '标题 2'; Text = '查看 Dashboard 行动总览（GT_UC_DASHBOARD_01）' },
        @{ Style = '正文'; Text = '本节描述 Dashboard 作为统一入口汇总成长状态、待处理事项和下一步行动建议所需满足的需求。' },
        @{ Style = '列出段落'; Text = '用例标示符：GT_UC_DASHBOARD_01' },
        @{ Style = '列出段落'; Text = '用例名称：查看 Dashboard 行动总览' },
        @{ Style = '列出段落'; Text = '范围：系统用例' },
        @{ Style = '列出段落'; Text = '级别：用户目标级别' },
        @{ Style = '列出段落'; Text = '主要角色：学生用户' },
        @{ Style = '列出段落'; Text = '涉众：学生用户、项目组、指导教师' },
        @{ Style = '列出段落'; Text = '前置条件：用户已登录系统，系统中可存在画像、目标、任务、随记或诊断等任意阶段数据。' },
        @{ Style = '列出段落'; Text = '后置条件：系统展示用户当前成长状态，并提供可跳转到目标、任务、随记或诊断页面的下一步行动入口。' },
        @{ Style = '列出段落'; Text = '主成功场景：用户进入 Dashboard；系统汇总画像完整度、主目标进度、本周任务节奏、待确认随记和最近诊断；系统生成最多若干条下一步行动建议；用户点击建议进入对应功能页面。' },
        @{ Style = '列出段落'; Text = '扩展（或替代流程）：若用户尚未建档或暂无目标，Dashboard 应优先提示建档或创建目标；若暂无诊断记录，应提示用户在数据积累后发起阶段诊断。' },
        @{ Style = '列出段落'; Text = '特殊需求：Dashboard 不应只展示静态统计，应突出下一步行动，并与目标、任务、随记、诊断形成明确跳转关系。' },
        @{ Style = '列出段落'; Text = '技术和数据变元表：Dashboard 汇总数据应包含画像完整度、主目标状态、任务节奏、待确认随记数量、最近诊断摘要和下一步行动链接。' }
    ) + (New-UmlFigureBlocks -Diagrams $dashboardDiagrams)

    Set-SectionContent -Doc $Doc -HeadingText '用户注册与登录（GT_UC_AUTH_01）' -NextHeadingText '非功能需求' -Blocks $useCaseDetailBlocks

    if ($script:UmlDiagrams -and $script:UmlDiagrams.Count -gt 0) {
        Write-SrsLog "Populate: inserting use-case UML figures"
        Replace-UmlPlaceholdersWithImages -Doc $Doc -Diagrams $script:UmlDiagrams
        Format-UmlCaptions -Doc $Doc -Diagrams $script:UmlDiagrams
        Write-SrsLog "Populate: use-case UML figures inserted"
    }

    Write-SrsLog "Populate: non-functional requirements"
    Set-SectionContent -Doc $Doc -HeadingText '界面需求' -NextHeadingText '外部接口' -Blocks @(
        @{ Style = '正文'; Text = '系统界面应以简洁、清晰、连续的流程体验为目标，优先突出成长闭环中的下一步关键操作。' },
        @{ Style = '列出段落'; Text = '显示风格：采用 Web 图形界面，重点页面包含建档、目标、任务、随记、诊断和 Dashboard。' },
        @{ Style = '列出段落'; Text = '显示方式：适配主流桌面浏览器分辨率，并兼顾课程演示场景下的投屏展示效果。' },
        @{ Style = '列出段落'; Text = '输出格式：系统应提供结构化页面展示，必要时为后续导出报告或周报预留扩展能力。' }
    )

    Set-SectionContent -Doc $Doc -HeadingText '外部接口' -NextHeadingText '性能需求' -Blocks @(
        @{ Style = '正文'; Text = '系统外部接口主要包括数据库接口、AI 服务接口以及浏览器与后端服务之间的 HTTP 接口。' },
        @{ Style = '列出段落'; Text = '与数据库接口：后端拟通过标准数据库访问组件读写 MySQL 数据。' },
        @{ Style = '列出段落'; Text = '与 AI 服务接口：后端拟通过 OpenAI-compatible 接口完成画像抽取、随记抽取和阶段总结请求。' },
        @{ Style = '列出段落'; Text = '与浏览器接口：前后端之间采用 JSON 数据交换格式。' }
    )

    Set-SectionContent -Doc $Doc -HeadingText '性能需求' -NextHeadingText '安全性需求' -Blocks @(
        @{ Style = '正文'; Text = '系统在课程演示和小规模试运行场景下应保持稳定响应。普通查询与列表类接口应在较短时间内返回结果，AI 相关接口可允许更长等待时间但必须给出明确状态提示。' }
    )

    Set-SectionContent -Doc $Doc -HeadingText '安全性需求' -NextHeadingText '可靠性需求' -Blocks @(
        @{ Style = '正文'; Text = '系统应通过登录认证、权限校验和数据隔离保障不同用户的成长数据安全。' },
        @{ Style = '列出段落'; Text = '要求利用 JWT 等身份校验机制保证接口访问受控；' },
        @{ Style = '列出段落'; Text = '要求对用户画像、诊断结果和阶段记录等敏感数据进行访问范围限制；' },
        @{ Style = '列出段落'; Text = '要求对数据库密码、AI 密钥等敏感配置采取本地化和非公开仓库存储策略；' },
        @{ Style = '列出段落'; Text = '要求对关键操作保留必要日志，便于后续联调和问题追踪。' }
    )

    Set-SectionContent -Doc $Doc -HeadingText '可靠性需求' -NextHeadingText '适应性需求' -Blocks @(
        @{ Style = '正文'; Text = '系统应具备基础容错能力。对于普通业务接口，应保证请求失败时能够返回稳定错误信息；对于 AI 相关流程，应在失败时保留必要上下文并允许用户重试。' }
    )

    Set-SectionContent -Doc $Doc -HeadingText '适应性需求' -NextHeadingText '设计约束' -Blocks @(
        @{ Style = '正文'; Text = '系统设计应支持后续在不改变核心闭环的前提下扩展图表展示、报告导出、任务联动和诊断策略等功能。若课程后续要求发生变化，系统应便于对页面文案、字段结构和业务流程进行调整。' }
    )

    Set-SectionContent -Doc $Doc -HeadingText '设计约束' -NextHeadingText '验收标准' -Blocks @(
        @{ Style = '正文'; Text = '本项目设计需遵循课程时间安排、团队协作能力和阶段提交节奏，优先保证需求闭环完整、主要模块清晰、演示链路可落地。' },
        @{ Style = '列出段落'; Text = '系统应保持 B/S 架构，不在本期核心范围内引入管理员后台、多角色复杂权限或微服务拆分；' },
        @{ Style = '列出段落'; Text = '系统需兼容课程实验环境与本地开发环境；' },
        @{ Style = '列出段落'; Text = '系统后续开发应尽量复用现有主流开发工具链与开源组件；' },
        @{ Style = '列出段落'; Text = '所有新增需求若明显扩大范围，应经项目组讨论后再纳入迭代版文档。' }
    )

    Set-SectionContent -Doc $Doc -HeadingText '功能验收' -NextHeadingText '性能验收标准：' -Blocks @(
        @{ Style = '正文'; Text = '功能验收将围绕课程项目的核心闭环展开，重点验证建档、目标、任务、随记、诊断与 Dashboard 总览是否能够形成连续流程。' },
        @{ Style = '列出段落'; Text = '验收项目：用户注册与登录；验收标准：可完成身份建立、登录校验与会话保持。' },
        @{ Style = '列出段落'; Text = '验收项目：成长画像建档与 AI 草稿生成；验收标准：可生成待确认草稿，且未经确认不得直接入正式数据。' },
        @{ Style = '列出段落'; Text = '验收项目：目标、requirement 与任务管理；验收标准：可建立目标并围绕 requirement 组织任务。' },
        @{ Style = '列出段落'; Text = '验收项目：成长随记与抽取确认；验收标准：可形成成长记录并支持抽取结果确认。' },
        @{ Style = '列出段落'; Text = '验收项目：阶段诊断与 Dashboard 总览；验收标准：可形成阶段结果并给出下一步行动入口。' }
    )

    Set-SectionContent -Doc $Doc -HeadingText '余量测试' -NextHeadingText '性能验收（含基准测试）' -Blocks @(
        @{ Style = '正文'; Text = '课程阶段的余量测试将以本地演示环境为基准，重点确认前端页面切换、普通查询和 AI 调用等待期间系统资源占用保持在可接受范围内。' },
        @{ Style = '列出段落'; Text = '计算机 CPU 与内存占用：在正常演示负载下应保持平稳，不影响核心链路操作。' },
        @{ Style = '列出段落'; Text = '网络负荷：在本地或校园网络环境下应满足普通接口与 AI 接口的基础调用需求。' }
    )

    Set-SectionContent -Doc $Doc -HeadingText '性能验收（含基准测试）' -NextHeadingText '各类响应时间测试' -Blocks @(
        @{ Style = '正文'; Text = '性能验收将重点关注易用性、兼容性和展示场景下的稳定性。' },
        @{ Style = '正文'; Text = '（1）易用性测试：系统应具备清晰导航、明确提示和可理解的错误反馈，主要操作路径应在少量步骤内完成。' },
        @{ Style = '列出段落'; Text = '验收项目：界面层次与操作流程；验收标准：符合成长闭环逻辑，用户能够顺利找到下一步入口。' },
        @{ Style = '列出段落'; Text = '验收项目：AI 调用等待反馈；验收标准：应展示处理中状态，并在失败时给出明确原因分类。' },
        @{ Style = '正文'; Text = '（2）兼容性及可扩充性测试：系统应在主流桌面浏览器上完成基本功能展示，并为后续导出、图表和任务联动扩展保留空间。' },
        @{ Style = '列出段落'; Text = '验收项目：浏览器适配；验收标准：在主流 Chromium 内核浏览器中显示正常。' },
        @{ Style = '列出段落'; Text = '验收项目：环境配置可复现性；验收标准：依据文档可完成本地部署与联调。' }
    )

    Set-SectionContent -Doc $Doc -HeadingText '各类响应时间测试' -NextHeadingText '系统健壮性测试' -Blocks @(
        @{ Style = '正文'; Text = '课程验收阶段对响应时间的要求将以可演示、可交互、可理解为主要目标。' },
        @{ Style = '列出段落'; Text = '界面生成与更新速度：普通页面打开与切换应保持流畅。' },
        @{ Style = '列出段落'; Text = '查询速度：列表查询与总览统计应在可接受时间内返回。' },
        @{ Style = '列出段落'; Text = 'AI 调用速度：允许较长等待，但应配合状态提示和重试机制。' },
        @{ Style = '列出段落'; Text = '数据传输与提交：表单提交、状态更新和结果回显应保持基本实时性。' }
    )

    Set-SectionContent -Doc $Doc -HeadingText '系统健壮性测试' -NextHeadingText '系统可靠性测试' -Blocks @(
        @{ Style = '正文'; Text = '系统健壮性测试将重点验证异常输入、网络中断、数据库连接失败和 AI 调用失败等情况下，系统能否给出稳定反馈并避免关键数据损坏。' },
        @{ Style = '正文'; Text = '对所有写入数据库的操作，后续实现应结合事务与校验机制保证数据完整性与一致性。' }
    )

    Set-SectionContent -Doc $Doc -HeadingText '系统可靠性测试' -NextHeadingText '安全保密功能的测试' -Blocks @(
        @{ Style = '正文'; Text = '系统可靠性测试将围绕连续使用、异常恢复和关键业务链路的稳定性展开，重点确认在多次建档、随记、任务和诊断操作后系统仍能保持可用。' }
    )

    Set-SectionContent -Doc $Doc -HeadingText '安全保密功能的测试' -NextHeadingText '产品提交' -Blocks @(
        @{ Style = '正文'; Text = '安全保密功能测试将重点验证登录认证、权限控制、敏感配置保护和关键数据访问范围控制。' },
        @{ Style = '列出段落'; Text = '测试项目：登录认证与令牌校验；测试标准：未登录用户不可访问受限接口。' },
        @{ Style = '列出段落'; Text = '测试项目：用户数据隔离；测试标准：不同用户不可越权读取他人画像、随记和诊断数据。' },
        @{ Style = '列出段落'; Text = '测试项目：敏感配置保护；测试标准：数据库密码与 AI 密钥不得出现在公开提交材料中。' }
    )

    Set-SectionContent -Doc $Doc -HeadingText '产品提交' -NextHeadingText '签字' -Blocks @(
        @{ Style = '正文'; Text = '本项目在课程阶段提交时，拟向指导教师和评审环节提交以下成果物：' },
        @{ Style = '正文'; Text = 'a) 系统源代码与数据库脚本电子版；' },
        @{ Style = '正文'; Text = 'b) 软件需求规格说明书、设计说明、测试与验收材料电子版；' },
        @{ Style = '正文'; Text = 'c) 系统运行与联调说明、演示脚本及答辩展示材料；' },
        @{ Style = '正文'; Text = 'd) 课程阶段如需纸质材料，则按课程要求提交对应打印版本。' }
    )

    Set-ParagraphTextByExactMatch -Doc $Doc -FindText '包括符合技术需求及非技术需求的要求、运行稳定性/安全性/故障率及恢复能力/业务处理能力（峰值/日均）等的要求。' -ReplaceText '本章用于给出课程阶段拟采用的功能、性能、健壮性与安全性验收标准，作为后续测试和答辩展示的依据。'
    Set-ParagraphStyleByExactMatch -Doc $Doc -Text '本章用于给出课程阶段拟采用的功能、性能、健壮性与安全性验收标准，作为后续测试和答辩展示的依据。' -Style '正文'

    Set-ParagraphTextByExactMatch -Doc $Doc -FindText '本《软件需求规格说明》建立在双方对需求的共同理解基础之上，我同意后续的开发工作根据该《软件需求规格说明》开展。如果需求发生变化，我们将按照“变更控制规程”执行。我明白需求的变更将导致双方重新协商成本、资源和进度等。' -ReplaceText '本《软件需求规格说明》建立在项目组对需求范围的共同理解基础之上。后续开发、设计和测试工作拟依据本文档开展；若课程要求或项目范围发生调整，项目组将通过迭代版文档进行同步修订。'
    Set-ParagraphStyleByExactMatch -Doc $Doc -Text '本《软件需求规格说明》建立在项目组对需求范围的共同理解基础之上。后续开发、设计和测试工作拟依据本文档开展；若课程要求或项目范围发生调整，项目组将通过迭代版文档进行同步修订。' -Style '正文'

    $signatureTable = $Doc.Tables.Item(10)
    $signatureTable.Rows.Item(1).Cells.Item(1).Range.Text = '甲方（签章）'
    $signatureTable.Rows.Item(1).Cells.Item(2).Range.Text = ''
    $signatureTable.Rows.Item(1).Cells.Item(3).Range.Text = '乙方（签章）'
    $signatureTable.Rows.Item(2).Cells.Item(1).Range.Text = '单位名称：'
    $signatureTable.Rows.Item(2).Cells.Item(2).Range.Text = '指导教师/课程组（待填写）'
    $signatureTable.Rows.Item(2).Cells.Item(3).Range.Text = ''
    $signatureTable.Rows.Item(2).Cells.Item(4).Range.Text = '单位名称：'
    $signatureTable.Rows.Item(2).Cells.Item(5).Range.Text = 'GrowthTrace 项目组'
    $signatureTable.Rows.Item(3).Cells.Item(1).Range.Text = '负 责 人：'
    $signatureTable.Rows.Item(3).Cells.Item(2).Range.Text = ''
    $signatureTable.Rows.Item(3).Cells.Item(3).Range.Text = ''
    $signatureTable.Rows.Item(3).Cells.Item(4).Range.Text = '负 责 人：'
    $signatureTable.Rows.Item(3).Cells.Item(5).Range.Text = ''
    $signatureTable.Rows.Item(4).Cells.Item(1).Range.Text = '签署日期：'
    $signatureTable.Rows.Item(4).Cells.Item(2).Range.Text = ''
    $signatureTable.Rows.Item(4).Cells.Item(3).Range.Text = ''
    $signatureTable.Rows.Item(4).Cells.Item(4).Range.Text = '签署日期：'
    $signatureTable.Rows.Item(4).Cells.Item(5).Range.Text = ''

    Write-SrsLog "Populate: updating TOC"
    foreach ($toc in $Doc.TablesOfContents) {
        $toc.Update()
    }
    Write-SrsLog "Populate: TOC updated"
}

$root = Split-Path -Parent $PSScriptRoot
$resourceDir = Join-Path $root 'resource'
$generatedDir = Join-Path $resourceDir 'generated'
New-Item -ItemType Directory -Force -Path $generatedDir | Out-Null
$script:SrsLogPath = Join-Path $generatedDir 'srs-generation.log'
Set-Content -LiteralPath $script:SrsLogPath -Encoding UTF8 -Value "[$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')] START"
$templatePath = Join-Path $resourceDir '2022软件需求规格说明书（SRS）模板.doc'
$iterPath = Join-Path $resourceDir 'GrowthTrace-软件需求规格说明书-迭代版-修正版.doc'
$submitPath = Join-Path $resourceDir 'GrowthTrace-软件需求规格说明书-提交版-2026-05-01-修正版.doc'

function Initialize-OutputDocument {
    param(
        [string]$Template,
        [string]$DesiredPath
    )

    $directory = Split-Path -Parent $DesiredPath
    $stem = [System.IO.Path]::GetFileNameWithoutExtension($DesiredPath)
    $extension = [System.IO.Path]::GetExtension($DesiredPath)
    $fileName = [System.IO.Path]::GetFileName($DesiredPath)
    $lockName = if ($fileName.Length -gt 2) { '~$' + $fileName.Substring(2) } else { '~$' + $fileName }
    $lockPath = Join-Path $directory $lockName
    $targetPath = $DesiredPath
    $fileIsLocked = $false

    if (Test-Path -LiteralPath $DesiredPath) {
        $stream = $null
        try {
            $stream = [System.IO.File]::Open($DesiredPath, [System.IO.FileMode]::Open, [System.IO.FileAccess]::ReadWrite, [System.IO.FileShare]::None)
        }
        catch {
            $fileIsLocked = $true
        }
        finally {
            if ($null -ne $stream) {
                $stream.Close()
                $stream.Dispose()
            }
        }
    }

    if ((Test-Path -LiteralPath $lockPath) -and (-not $fileIsLocked)) {
        Remove-Item -LiteralPath $lockPath -Force -ErrorAction SilentlyContinue
    }

    if ($fileIsLocked) {
        $targetPath = Join-Path $directory ($stem + '-继续补全版' + $extension)
    }

    try {
        Copy-Item -LiteralPath $Template -Destination $targetPath -Force
        return $targetPath
    }
    catch {
        if ($_.Exception.Message -match 'being used by another process') {
            $fallback = Join-Path $directory ($stem + '-继续补全版' + $extension)
            if (Test-Path -LiteralPath $fallback) {
                $fallback = Join-Path $directory ($stem + '-继续补全版-' + (Get-Date -Format 'yyyyMMddHHmmss') + $extension)
            }
            Copy-Item -LiteralPath $Template -Destination $fallback -Force
            return $fallback
        }

        throw
    }
}

$script:UmlDiagrams = Ensure-UmlAssets -ResourceDir $resourceDir

$iterOutputPath = Initialize-OutputDocument -Template $templatePath -DesiredPath $iterPath
$submitOutputPath = Initialize-OutputDocument -Template $templatePath -DesiredPath $submitPath

function Process-OutputDocument {
    param(
        [string]$Path,
        [string]$VersionLabel,
        [string]$VersionCode,
        [string]$DateText,
        [string]$RecordDate
    )

    $word = $null
    for ($attempt = 1; $attempt -le 5; $attempt++) {
        try {
            Write-SrsLog "Creating Word application for $Path (attempt $attempt)"
            $word = New-Object -ComObject Word.Application
            Write-SrsLog "Word application created for $Path"
            break
        }
        catch {
            if ($attempt -eq 5) {
                throw
            }
            Start-Sleep -Seconds 3
        }
    }
    $word.Visible = $false
    $word.DisplayAlerts = 0

    try {
        Write-SrsLog "Opening document: $Path"
        $doc = $word.Documents.Open($Path)
        if ($null -eq $doc) {
            throw "Failed to open Word document: $Path"
        }

        Write-SrsLog "Opened document: $Path"
        Write-SrsLog "Populating document: $Path"
        Populate-Document -Doc $doc -VersionLabel $VersionLabel -VersionCode $VersionCode -DateText $DateText -RecordDate $RecordDate
        Write-SrsLog "Saving document: $Path"
        $doc.Save()
        Write-SrsLog "Saved document: $Path"
        $doc.Close()
        Write-SrsLog "Closed document: $Path"
    }
    finally {
        Write-SrsLog "Quitting Word for $Path"
        try {
            if ($null -ne $word) {
                $word.Quit()
            }
        }
        catch {
            Write-SrsLog "Word quit failed for ${Path}: $($_.Exception.Message)"
        }

        try {
            if ($null -ne $word) {
                [System.Runtime.InteropServices.Marshal]::ReleaseComObject($word) | Out-Null
            }
        }
        catch {
            Write-SrsLog "Word release failed for ${Path}: $($_.Exception.Message)"
        }
        Write-SrsLog "Word cleanup finished for $Path"
        Start-Sleep -Seconds 2
    }
}

Process-OutputDocument -Path $iterOutputPath -VersionLabel 'V1.0（迭代版）' -VersionCode 'V1.0' -DateText '二〇二六年四月' -RecordDate '2026-04-26'
Process-OutputDocument -Path $submitOutputPath -VersionLabel 'V1.0（提交版）' -VersionCode 'V1.0' -DateText '二〇二六年五月' -RecordDate '2026-05-01'

Write-Output "ITER_OUTPUT=$iterOutputPath"
Write-Output "SUBMIT_OUTPUT=$submitOutputPath"
