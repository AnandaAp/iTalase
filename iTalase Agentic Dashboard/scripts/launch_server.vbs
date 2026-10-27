Set WshShell = CreateObject("WScript.Shell")
WshShell.CurrentDirectory = "C:\Users\anand\StudioProjects\iTalase\iTalase Agentic Dashboard"
WshShell.Run "cmd.exe /c npm start", 0, False
