param([string]$directory)
$fileSize = ls -force -r -ea SilentlyContinue $directory|measure -s Length|select-object -expand Sum
$endOfScript = "EndOfScriptGetFolderSize"
$fileSize
$endOfScript