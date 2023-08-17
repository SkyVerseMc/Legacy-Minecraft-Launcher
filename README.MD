<p><h1>Legacy Minecraft Launcher
<img align="right" src="https://raw.githubusercontent.com/devicons/devicon/master/icons/linux/linux-original.svg" alt="Linux" height="50" width="50"/>
<img align="right" src="https://raw.githubusercontent.com/devicons/devicon/master/icons/windows8/windows8-original.svg" alt="Windows" height="50" width="50"/>
</h1></p>

![Downloads](https://img.shields.io/github/downloads/SkyVerseMc/Legacy-Minecraft-Launcher/total?label=Downloads&color=%2300B900)


![screenshot](https://github.com/SkyVerseMc/Legacy-Minecraft-Launcher/assets/95189671/66d7c439-5e25-469c-9ce9-3b6441ec8bb8)

Do you remember the good old Minecraft Launcher that was used from the beginning of Minecraft until the Microsoft Account Migration?
Its last version was [1.6.93](https://minecraft.fandom.com/wiki/Launcher_1.6.93  "Launcher 1.6.93") and was released on 12 September 2019.

As Minecraft Wiki said:

> The legacy Minecraft Launcher is incompatible with Microsoft accounts and was discontinued in Q2 2021 due to account migration.

Today, this launcher is obsolete, you simply can't use it because of 2 main problems:
- The Microsoft authentication system (Mojang's Authlib is now deprecated);
- The new Minecraft version API (assets, libraries, natives...) has changed;
However, existing accounts will still be able to launch old versions, because their token is already saved and does not require to authenticate again.

It seems easy to fix, but it's really more complex (plus the launcher code wasn't really optimised...).
So I modded this launcher and now it works fine.

## How to install
There are two important files:
- The MinecraftLauncher.exe/.jar (this is the bootstrap that checks for updates and starts the launcher);
- The launcher.lzma (the compressed launcher, you don't really need to download it because the bootstrap does it automatically).

Although most of the code can be found in this repo, I highly recommend downloading the [**latest version of the launcher**](https://github.com/SkyVerseMc/Legacy-Minecraft-Launcher/releases/latest), because all dependencies are not published here, as their code is not mine:

- com.google.common
- com.google.gson
- com.google.thirdparty
- javax.annotation
- joptsimple
- org.apache.commons
- org.apache.logging.log4j

## How ??? (Using magic)

- I had to use another dependency: [Litarvan's OpenAuth](https://github.com/Litarvan/OpenAuth) (thanks a lot to him) to solve the authentication problem, and I modified some classes (a bit) to better fit the launcher code.
- I rewrote a bit the launcher's code to remove some parts of useless code (but not all) and change API call and some stuff.
- I spend a lot of time on it.

## What changed ?
Go to the end, [TODO section](#todo)

## Disclaimer
- I've resurrected the old launcher with some modifications and optimised certain parts, but not all of it, which means that there may be crashes and so on. Don't hesitate to let me know if you have any problems with the launcher. I plan to optimize it further as time goes on.
- Don't cry when you read the code, 90% of it was made by Mojang.
- For some reasons (= idk) textures can be glitched on latest versions.
- If there is a problem, try restarting the launcher before opening an issue.

- If you have this crash report:
```
Exception in thread "main" java.lang.ClassCastException: class jdk.internal.loader.ClassLoaders$AppClassLoader cannot be cast to class java.net.URLClassLoader (jdk.internal.loader.ClassLoaders$AppClassLoader and java.net.URLClassLoader are in module java.base of loader 'bootstrap')
at net.minecraft.launchwrapper.Launch.<init>(Launch.java:24)
at net.minecraft.launchwrapper.Launch.main(Launch.java:18)
```
It's because you are trying to launch old releases < 1.6, this crash is caused by the new java version.
You can simply fix that by using java 8 instead, by changing the java path in profile editor.

## Todo
- [x] Microsoft Authentication Implementation
- [x] New Minecraft Version API
- [x] New Minecraft assets
- [x] Patch Log4shell
- [x] Change default JVM arguments to make them up to date
- [x] Add "Releases", "Modded versions" and "Installed versions" filter in profile popup's version selector
- [x] Add "Latest Snapshot" version in profile popup's version selector
- [x] SquidHQ option
- [ ] Add mods support (fabric, etc...), but you can launch versions that does not need external libraries
