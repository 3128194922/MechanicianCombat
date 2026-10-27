param(
	[string]$Task = 'build'
)

$projectRoot = Split-Path -Parent $PSScriptRoot
$gradleWrapper = Join-Path $projectRoot 'gradlew.bat'
$previousGradleHome = $env:GRADLE_USER_HOME
$previousJavaToolOptions = $env:JAVA_TOOL_OPTIONS
$buildExitCode = 1

try {
	$env:GRADLE_USER_HOME = Join-Path $projectRoot '.gradle'
	$certificateCheckOption = '-Dnet.minecraftforge.gradle.check.certs=false'
	$env:JAVA_TOOL_OPTIONS = if ([string]::IsNullOrWhiteSpace($previousJavaToolOptions)) {
		$certificateCheckOption
	} else {
		"$previousJavaToolOptions $certificateCheckOption"
	}

	& $gradleWrapper --offline --no-daemon --console=plain $Task
	$buildExitCode = $LASTEXITCODE
} finally {
	if ($null -eq $previousGradleHome) {
		Remove-Item Env:GRADLE_USER_HOME -ErrorAction SilentlyContinue
	} else {
		$env:GRADLE_USER_HOME = $previousGradleHome
	}
	if ($null -eq $previousJavaToolOptions) {
		Remove-Item Env:JAVA_TOOL_OPTIONS -ErrorAction SilentlyContinue
	} else {
		$env:JAVA_TOOL_OPTIONS = $previousJavaToolOptions
	}
}

exit $buildExitCode
