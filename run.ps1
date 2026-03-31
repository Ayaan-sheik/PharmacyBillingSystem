$jfxUrl = "https://download2.gluonhq.com/openjfx/23.0.1/openjfx-23.0.1_windows-x64_bin-sdk.zip"
$jfxZip = "javafx-sdk.zip"
$jfxDir = "javafx-sdk-23.0.1"

if (-Not (Test-Path "lib\$jfxDir")) {
    Write-Host "Downloading JavaFX SDK 23.0.1..."
    if (-Not (Test-Path "lib")) { New-Item -ItemType Directory -Path "lib" | Out-Null }
    Invoke-WebRequest -Uri $jfxUrl -OutFile $jfxZip
    Write-Host "Extracting JavaFX SDK..."
    Expand-Archive -Path $jfxZip -DestinationPath "lib" -Force
    Remove-Item $jfxZip
}

$libPath = "lib\$jfxDir\lib"
Write-Host "JavaFX SDK found at $libPath"

Write-Host "Compiling source code..."
if (-Not (Test-Path "out")) { New-Item -ItemType Directory -Path "out" | Out-Null }

# Create required directories in out
if (-Not (Test-Path "out\pharmasync\gui")) { New-Item -ItemType Directory -Path "out\pharmasync\gui" -Force | Out-Null }

# Copy CSS resource
Copy-Item "src\pharmasync\gui\styles.css" -Destination "out\pharmasync\gui\styles.css" -Force

# Find all java files
$javaFiles = Get-ChildItem -Path "src" -Recurse -Filter "*.java" | Select-Object -ExpandProperty FullName

# Find all java files and quote them to handle spaces in paths
$javaFilesArgs = $javaFiles | ForEach-Object { "`"$_`"" }
$cmd = "javac --module-path `"$libPath`" --add-modules javafx.controls,javafx.fxml -d `"out`" $($javaFilesArgs -join ' ')"

# Compile
Write-Host "Executing javac..."
Invoke-Expression $cmd
if ($LASTEXITCODE -ne 0) {
    Write-Host "Compilation failed."
    exit $LASTEXITCODE
}

Write-Host "Compilation successful. Starting PharmaSync Application..."

# Run
java --module-path "$libPath" --add-modules javafx.controls,javafx.fxml -cp "out" pharmasync.gui.PharmaSyncApp
