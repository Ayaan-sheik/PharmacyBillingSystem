$jfxUrl = "https://download2.gluonhq.com/openjfx/23.0.1/openjfx-23.0.1_windows-x64_bin-sdk.zip"
$jfxZip = "javafx-sdk.zip"
$jfxDir = "javafx-sdk-23.0.1"

$sqliteUrl = "https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/3.45.3.0/sqlite-jdbc-3.45.3.0.jar"
$sqliteJar = "lib\sqlite-jdbc-3.45.3.0.jar"

$slf4jUrl = "https://repo1.maven.org/maven2/org/slf4j/slf4j-api/1.7.36/slf4j-api-1.7.36.jar"
$slf4jJar = "lib\slf4j-api-1.7.36.jar"

$itextUrl = "https://repo1.maven.org/maven2/com/itextpdf/itextpdf/5.5.13.3/itextpdf-5.5.13.3.jar"
$itextJar = "lib\itextpdf-5.5.13.3.jar"

if (-Not (Test-Path "lib")) { New-Item -ItemType Directory -Path "lib" | Out-Null }

if (-Not (Test-Path "lib\$jfxDir")) {
    Write-Host "Downloading JavaFX SDK 23.0.1..."
    Invoke-WebRequest -Uri $jfxUrl -OutFile $jfxZip
    Write-Host "Extracting JavaFX SDK..."
    Expand-Archive -Path $jfxZip -DestinationPath "lib" -Force
    Remove-Item $jfxZip
}

if (-Not (Test-Path $sqliteJar)) {
    Write-Host "Downloading SQLite JDBC..."
    Invoke-WebRequest -Uri $sqliteUrl -OutFile $sqliteJar
}

if (-Not (Test-Path $slf4jJar)) {
    Write-Host "Downloading SLF4J API..."
    Invoke-WebRequest -Uri $slf4jUrl -OutFile $slf4jJar
}

if (-Not (Test-Path $itextJar)) {
    Write-Host "Downloading iText PDF..."
    Invoke-WebRequest -Uri $itextUrl -OutFile $itextJar
}

$libPath = "lib\$jfxDir\lib"
Write-Host "JavaFX SDK found at $libPath"

Write-Host "Compiling source code..."
if (-Not (Test-Path "out")) { New-Item -ItemType Directory -Path "out" | Out-Null }

# Create required directories in out
if (-Not (Test-Path "out\pharmasync\gui")) { New-Item -ItemType Directory -Path "out\pharmasync\gui" -Force | Out-Null }
if (-Not (Test-Path "out\pharmasync\managers")) { New-Item -ItemType Directory -Path "out\pharmasync\managers" -Force | Out-Null }
if (-Not (Test-Path "out\pharmasync\models")) { New-Item -ItemType Directory -Path "out\pharmasync\models" -Force | Out-Null }
if (-Not (Test-Path "out\pharmasync\threads")) { New-Item -ItemType Directory -Path "out\pharmasync\threads" -Force | Out-Null }

# Copy resources
Copy-Item "src\main\resources\pharmasync\gui\*" -Destination "out\pharmasync\gui\" -Force

# Find all java files
$javaFiles = Get-ChildItem -Path "src\main\java" -Recurse -Filter "*.java" | Select-Object -ExpandProperty FullName
$javaFilesArgs = $javaFiles | ForEach-Object { "`"$_`"" }

$cp = "out;$sqliteJar;$slf4jJar;$itextJar"
$cmd = "javac -cp `"$cp`" --module-path `"$libPath`" --add-modules javafx.controls,javafx.fxml -d `"out`" $($javaFilesArgs -join ' ')"

Write-Host "Executing javac..."
Invoke-Expression $cmd
if ($LASTEXITCODE -ne 0) {
    Write-Host "Compilation failed."
    exit $LASTEXITCODE
}

Write-Host "Compilation successful. Starting PharmaSync Application..."

# Run
# Run non-blocking
Start-Process -FilePath "java" -ArgumentList "-cp `"$cp`" --module-path `"$libPath`" --add-modules javafx.controls,javafx.fxml pharmasync.gui.PharmaSyncApp"
