param(
    [Parameter(Mandatory)][ValidatePattern('^emulator-\d+$')][string]$Serial,
    [string]$Adb = 'adb',
    [switch]$Capture,
    [switch]$InstrumentationOnly
)

$ErrorActionPreference = 'Stop'
$package = 'org.ersinpro.launcher'
$root = Split-Path $PSScriptRoot
$apk = Join-Path $root 'build/ersinpro-debug.apk'

function Device {
    & $Adb -s $Serial @args
    if ($LASTEXITCODE) { throw "ADB failed: $args" }
}

function Key([string]$Name) {
    Device shell input keyevent "KEYCODE_$Name" | Out-Null
}

function Snapshot {
    $result = & $Adb -s $Serial shell uiautomator dump /sdcard/ersinpro-test.xml 2>$null
    if ($LASTEXITCODE -eq 0 -and "$result" -match 'dumped to:') {
        $xml = & $Adb -s $Serial shell cat /sdcard/ersinpro-test.xml 2>$null
        if ($LASTEXITCODE -eq 0 -and "$xml" -match '^<\?xml') { return [xml]($xml -join "`n") }
    }
    return $null
}

function Wait-Text([string]$Label, [switch]$Focused) {
    for ($try = 0; $try -lt 10; $try++) {
        $snapshot = Snapshot
        if (!$snapshot) { continue }
        $matching = $snapshot.SelectNodes('//node') | Where-Object {
            $_.GetAttribute('text') -ceq $Label -and (!$Focused -or
                $_.GetAttribute('focused') -eq 'true' -or $_.GetAttribute('selected') -eq 'true')
        }
        if ($matching) { return }
    }
    $visibleLabels = if ($snapshot) {
        ($snapshot.SelectNodes('//node[@text!=""]') |
            ForEach-Object { $_.GetAttribute('text') }) -join ' | '
    }
    $resumed = Device shell dumpsys activity activities | Select-String 'topResumedActivity|mResumedActivity'
    throw "UI text or focus missing: $Label (focused=$Focused). Visible labels: $visibleLabels. Resumed: $resumed"
}

function Start-Home {
    Device shell am start -W -n "$package/.HomeActivity" | Out-Null
}

function Open-Settings {
    Wait-Text 'Settings' -Focused
    Key DPAD_CENTER
    Wait-Text 'Manage applications'
}

function Confirm-MenuItem([string]$Label) {
    for ($attempt = 0; $attempt -lt 10; $attempt++) {
        $current = Snapshot
        if (!$current) { continue }
        $rows = @($current.SelectNodes('//node[@class="android.widget.ListView"]//node[@text!=""]'))
        $labels = @($rows | ForEach-Object { $_.GetAttribute('text') })
        $target = [Array]::IndexOf($labels, $Label)
        if ($target -lt 0) { continue }
        $active = $rows | Where-Object { $_.GetAttribute('selected') -eq 'true' } | Select-Object -First 1
        if ($active -and $active.GetAttribute('text') -ceq $Label) {
            Key DPAD_CENTER
            return
        }
        $position = if ($active) { [Array]::IndexOf($labels, $active.GetAttribute('text')) } else { -1 }
        if ($position -gt $target) { Key DPAD_UP } else { Key DPAD_DOWN }
    }
    throw "Cannot focus menu item: $Label"
}

if ((Device shell getprop ro.kernel.qemu) -ne '1') {
    throw 'The UI suite requires a disposable Android emulator'
}
Device install --no-incremental -r $apk
Device install --no-incremental -r (Join-Path $root 'build/ersinpro-instrumentation.apk')
$instrument = @('shell', 'am', 'instrument', '-w')
if ($Capture) { $instrument += @('-e', 'capture', 'true') }
$checks = Device @instrument 'org.ersinpro.launcher.test/org.ersinpro.launcher.RegressionInstrumentation'
$checks
if (($checks -join ' ') -notmatch 'ERSINPRO_INSTRUMENTATION_PASS') {
    throw 'Android instrumentation failed'
}
if ($Capture) {
    Device pull "/sdcard/Android/data/$package/files/ersinpro-home.png" (Join-Path $root 'build/ersinpro-home.png') | Out-Null
}
if ($InstrumentationOnly) { return }
$homeCandidates = Device shell cmd package query-activities --brief -a android.intent.action.MAIN -c android.intent.category.HOME
if (($homeCandidates -join ' ') -notmatch [regex]::Escape($package)) {
    throw 'ErsinPro is missing from Android HOME candidates'
}
Device shell pm clear $package | Out-Null
Device shell input keyevent KEYCODE_WAKEUP | Out-Null
Device shell wm dismiss-keyguard | Out-Null
Start-Home
Wait-Text 'ErsinPro'
Wait-Text 'Settings' -Focused
Wait-Text 'Entertainment'
Wait-Text 'Apps & Tools'

# Exercise a real external task whose only exit is the ordinary Back action.
Device shell am force-stop $package | Out-Null
Start-Home
Wait-Text 'Settings' -Focused
Key DPAD_DOWN
$homeFocus = Snapshot
if (!$homeFocus -or !$homeFocus.SelectSingleNode('//node[@content-desc="A ErsinPro Test Player" and @focused="true"]')) {
    Key DPAD_DOWN
}
Key DPAD_CENTER
Wait-Text 'Test playback'
Key BACK
Wait-Text 'ErsinPro'
$restored = $false
for ($i = 0; $i -lt 5; $i++) {
    $returned = Snapshot
    if ($returned -and $returned.SelectSingleNode('//node[@content-desc="A ErsinPro Test Player" and (@selected="true" or @focused="true")]')) {
        $restored = $true
        break
    }
}
if (!$restored) {
    throw 'External task return did not restore the player card'
}

# Restart from a fresh home selection for each independent settings scenario.
Device shell pm clear $package | Out-Null
Device shell am force-stop $package | Out-Null
Start-Home
Open-Settings
Confirm-MenuItem 'Turn on Focus Mode'
Wait-Text 'No visible applications'
for ($i = 0; $i -lt 5; $i++) {
    $emptyHome = Snapshot
    $emptyRows = if ($emptyHome) { $emptyHome.SelectNodes('//node[@text="No visible applications"]').Count } else { 0 }
    if ($emptyRows -eq 2) { break }
}
if ($emptyRows -ne 2) {
    throw 'Focus Mode must leave both category headings and empty messages visible'
}
Open-Settings
Confirm-MenuItem 'Language'
Confirm-MenuItem 'German'
Wait-Text 'Einstellungen'
Wait-Text 'Apps und Werkzeuge'
Wait-Text 'Keine sichtbaren Apps'
Device shell am force-stop $package | Out-Null
Start-Home
Wait-Text 'Einstellungen'
Wait-Text 'Keine sichtbaren Apps'

Device shell am force-stop $package | Out-Null
Device shell run-as $package mkdir -p shared_prefs | Out-Null
'<map><string name="configuration">broken</string></map>' |
    & $Adb -s $Serial shell -T "run-as $package sh -c 'cat > shared_prefs/launcher.xml'"
if ($LASTEXITCODE) { throw 'Could not inject corrupt configuration into the test application' }
Start-Home
Wait-Text 'Settings' -Focused
Open-Settings
Confirm-MenuItem 'Android settings'
for ($i = 0; $i -lt 5; $i++) {
    $active = Device shell dumpsys activity activities | Select-String 'topResumedActivity|mResumedActivity'
    if ("$active" -match 'com.android.settings') { break }
    Start-Sleep -Milliseconds 500
}
if ("$active" -notmatch 'com.android.settings') { throw 'Android settings did not open' }
Key BACK
Wait-Text 'ErsinPro'
Device shell rm -f /sdcard/ersinpro-test.xml | Out-Null
'PASS: startup, remote navigation, favorites, focus mode, locale persistence, corrupt configuration and external activity'
