param([string]$directory)
$directoryList = Get-Childitem $directory
$directoryListClean = @()
foreach($userDirectory in $directoryList) {
$newDirectoryObject = New-Object System.Object
$newDirectoryObject | Add-Member -type NoteProperty -name Name -value "$($userDirectory.Name)"
$newDirectoryObject | Add-Member -type NoteProperty -name LastUpdated -value "$($userDirectory.LastWriteTime)"
$newDirectoryObject.Name = $newDirectoryObject.Name -replace "`n|`r|`t",""
$newDirectoryObject.LastUpdated = $newDirectoryObject.LastUpdated -replace "`n|`r|`t",""
$newDirectoryObjectString = "$($newDirectoryObject.Name)" + "`t" + "$($newDirectoryObject.LastUpdated)"
$directoryListClean += $newDirectoryObjectString
}
$directoryListClean += "EndOfScriptGetDirectoryList"
$directoryListClean