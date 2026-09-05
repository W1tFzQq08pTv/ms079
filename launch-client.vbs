Option Explicit

Dim fileSystem, shell, scriptDirectory, guardScript, powerShell, command

Set fileSystem = CreateObject("Scripting.FileSystemObject")
Set shell = CreateObject("WScript.Shell")

If WScript.Arguments.Count > 0 Then
    guardScript = fileSystem.GetAbsolutePathName(WScript.Arguments(0))
Else
    guardScript = fileSystem.BuildPath( _
        fileSystem.GetParentFolderName(WScript.ScriptFullName), _
        "ms079-launch-guard.ps1")
End If
scriptDirectory = fileSystem.GetParentFolderName(guardScript)

If Not fileSystem.FileExists(guardScript) Then
    MsgBox "Launcher guard not found:" & vbCrLf & guardScript, vbCritical, "MapleStory 079 Launcher"
    WScript.Quit 1
End If

powerShell = shell.ExpandEnvironmentStrings("%SystemRoot%\System32\WindowsPowerShell\v1.0\powershell.exe")
command = Quote(powerShell) & _
    " -NoProfile -ExecutionPolicy Bypass -WindowStyle Hidden -Command " & _
    Quote("& { try { & '" & EscapePowerShellLiteral(guardScript) & _
    "' } catch { Add-Type -AssemblyName PresentationFramework; " & _
    "[System.Windows.MessageBox]::Show($_.Exception.Message, 'MapleStory 079 Launcher', 'OK', 'Error') | Out-Null; exit 1 } }")

shell.CurrentDirectory = scriptDirectory
shell.Run command, 0, False

Function Quote(value)
    Quote = Chr(34) & Replace(value, Chr(34), Chr(34) & Chr(34)) & Chr(34)
End Function

Function EscapePowerShellLiteral(value)
    EscapePowerShellLiteral = Replace(value, "'", "''")
End Function
