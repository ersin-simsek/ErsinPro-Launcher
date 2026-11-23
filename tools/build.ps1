param(
    [string]$SdkRoot = $env:ANDROID_HOME,
    [switch]$Lint,
    [string]$LintJar
)

$ErrorActionPreference = 'Stop'
if (-not $SdkRoot) {
    $SdkRoot = $env:ANDROID_SDK_ROOT
}
if (-not $SdkRoot) {
    $SdkRoot = Join-Path $env:LOCALAPPDATA 'Android\Sdk'
}

function Invoke-Checked {
    param([string]$Program, [string[]]$Arguments)
    & $Program @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "$Program failed with exit code $LASTEXITCODE"
    }
}

$root = Split-Path $PSScriptRoot
$buildTools = Join-Path $SdkRoot 'build-tools/34.0.0'
$platform = Join-Path $SdkRoot 'platforms/android-34/android.jar'
$exe = if ($IsLinux -or $IsMacOS) { '' } else { '.exe' }
$batch = if ($IsLinux -or $IsMacOS) { '' } else { '.bat' }
$aapt = Join-Path $buildTools "aapt2$exe"
$d8 = Join-Path $buildTools "d8$batch"
$align = Join-Path $buildTools "zipalign$exe"
$signer = Join-Path $buildTools "apksigner$batch"
foreach ($file in @($platform, $aapt, $d8, $align, $signer)) {
    if (-not (Test-Path -LiteralPath $file -PathType Leaf)) {
        throw "Android SDK component missing or inaccessible: $file"
    }
}

$output = Join-Path $root 'build'
$work = Join-Path $output ([guid]::NewGuid().ToString())
$classes = Join-Path $work 'classes'
New-Item -ItemType Directory -Path $classes | Out-Null
$sources = Get-ChildItem (Join-Path $root 'launcher/src') -Filter '*.java' -Recurse
$manifest = Join-Path $root 'launcher/AndroidManifest.xml'
$resources = Join-Path $root 'launcher/res'
$compiled = Join-Path $work 'resources.zip'
$jar = Join-Path $work 'classes.jar'
$unsigned = Join-Path $work 'unsigned.apk'
$aligned = Join-Path $work 'aligned.apk'
$signed = Join-Path $work 'signed.apk'
$key = Join-Path $output 'debug.keystore'

Invoke-Checked $aapt @('compile', '--dir', $resources, '-o', $compiled)
Invoke-Checked $aapt @('link', '-I', $platform, '--manifest', $manifest, '--debug-mode',
    '--java', $work, '-o', $unsigned, $compiled)
$generated = Join-Path $work 'org/ersinpro/launcher/R.java'
Invoke-Checked javac (@('--release', '8', '-encoding', 'UTF-8', '-classpath', $platform,
    '-d', $classes, $generated) + @($sources.FullName))
if ($Lint) {
    $env:ANDROID_HOME = $SdkRoot
    $lintArgs = @('--exitcode', '--classpath', $classes,
        '--check', 'NewApi,InlinedApi,MissingTranslation,ExtraTranslation,HardcodedText,ExportedReceiver',
        (Join-Path $root 'launcher'))
    if ($LintJar) {
        Invoke-Checked java (@('-cp', $LintJar, 'com.android.tools.lint.Main') + $lintArgs)
    } else {
        Invoke-Checked lint $lintArgs
    }
}
Invoke-Checked jar @('--create', '--file', $jar, '-C', $classes, '.')
Invoke-Checked $d8 @('--min-api', '28', '--lib', $platform, '--output', $work, $jar)
Invoke-Checked jar @('--update', '--file', $unsigned, '-C', $work, 'classes.dex')
Invoke-Checked $align @('-f', '4', $unsigned, $aligned)
if (-not (Test-Path -LiteralPath $key)) {
    Invoke-Checked keytool @('-genkeypair', '-keystore', $key, '-storepass', 'android',
        '-keypass', 'android', '-alias', 'androiddebugkey', '-dname', 'CN=Android Debug',
        '-keyalg', 'RSA', '-keysize', '2048', '-validity', '10000')
}
Invoke-Checked $signer @('sign', '--ks', $key, '--ks-pass', 'pass:android',
    '--ks-key-alias', 'androiddebugkey', '--out', $signed, $aligned)
Invoke-Checked $signer @('verify', '--min-sdk-version', '28', '--verbose', $signed)
Invoke-Checked $align @('-c', '4', $signed)
$apk = Join-Path $output 'ersinpro-debug.apk'
Copy-Item -LiteralPath $signed -Destination $apk -Force
Get-Item -LiteralPath $apk | Select-Object FullName, Length

$testFolder = Join-Path $root 'tests/instrumentation'
$testClasses = Join-Path $work 'test-classes'
$testDex = Join-Path $work 'test-dex'
New-Item -ItemType Directory $testClasses, $testDex | Out-Null
$testJar = Join-Path $work 'test.jar'
$testUnsigned = Join-Path $work 'test-unsigned.apk'
$testAligned = Join-Path $work 'test-aligned.apk'
$testApk = Join-Path $output 'ersinpro-instrumentation.apk'
$classpath = "$platform$([IO.Path]::PathSeparator)$classes"
$testSources = Get-ChildItem -LiteralPath $testFolder -Filter '*.java'
Invoke-Checked javac (@('--release', '8', '-encoding', 'UTF-8', '-classpath', $classpath,
    '-d', $testClasses) + @($testSources.FullName))
Invoke-Checked jar @('--create', '--file', $testJar, '-C', $testClasses, '.')
Invoke-Checked $d8 @('--min-api', '28', '--lib', $platform, '--classpath', $jar, '--output', $testDex, $testJar)
Invoke-Checked $aapt @('link', '-I', $platform, '--manifest',
    (Join-Path $testFolder 'AndroidManifest.xml'), '-o', $testUnsigned)
Invoke-Checked jar @('--update', '--file', $testUnsigned, '-C', $testDex, 'classes.dex')
Invoke-Checked $align @('-f', '4', $testUnsigned, $testAligned)
Invoke-Checked $signer @('sign', '--ks', $key, '--ks-pass', 'pass:android',
    '--ks-key-alias', 'androiddebugkey', '--out', $testApk, $testAligned)
