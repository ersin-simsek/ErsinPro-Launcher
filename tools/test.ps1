$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot
$source = Join-Path $root 'launcher/src/org/ersinpro/launcher'
$output = Join-Path $root 'build/unit'
New-Item -ItemType Directory -Path $output -Force | Out-Null
& javac --release 8 -encoding UTF-8 -Xlint:all -Werror -d $output (Join-Path $source AppEntry.java) (Join-Path $source LauncherState.java) (Join-Path $source HomeContent.java) (Join-Path $source HomeLayout.java) (Join-Path $root tests/LauncherStateTest.java)
if ($LASTEXITCODE) { throw 'Unit test compilation failed' }
& java -cp $output org.ersinpro.launcher.LauncherStateTest
if ($LASTEXITCODE) { throw 'Unit tests failed' }

$res = Join-Path $root 'launcher/res'
$baseline = @{}
foreach ($locale in @('values', 'values-tr', 'values-de')) {
    $path = Join-Path $res "$locale/strings.xml"
    [xml]$document = Get-Content -LiteralPath $path -Raw -Encoding UTF8
    $seen = @{}
    foreach ($item in $document.resources.string) {
        $name = $item.GetAttribute('name')
        if ([string]::IsNullOrWhiteSpace($item.InnerText) -or $seen.ContainsKey($name)) {
            throw "Empty or duplicate resource $name in $locale"
        }
        $format = [regex]::Matches($item.InnerText, '%\d+\$[a-zA-Z]').Value | Sort-Object
        $seen[$name] = $format -join ','
    }
    if ($locale -eq 'values') { $baseline = $seen; continue }
    if ($seen.Count -ne $baseline.Count) { throw "Resource count mismatch in $locale" }
    foreach ($name in $baseline.Keys) {
        if (!$seen.ContainsKey($name) -or $seen[$name] -cne $baseline[$name]) {
            throw "Missing or incompatible string $name in $locale"
        }
    }
}
if ($baseline.Count -eq 0) { throw 'No default localizations found' }
"PASS: $($baseline.Count) strings and format placeholders match in English, Turkish and German"
