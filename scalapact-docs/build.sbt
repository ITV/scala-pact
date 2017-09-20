name := "scalapact-docs"

enablePlugins(ParadoxSitePlugin)

sourceDirectory in Paradox := sourceDirectory.value / "main" / "paradox"

ghpages.settings

git.remoteRepo := "git@github.com:ITV/scala-pact.git"
