# Legacy Minecraft Launcher
Do you remember the good old Minecraft Launcher that was used from the beginning of Minecraft until the Microsoft Account Migration?
Its last version was [1.6.93](https://minecraft.fandom.com/wiki/Launcher_1.6.93 "Launcher 1.6.93") and was released on 12 September 2019.

Today, this launcher is obsolete, you simply can't use it because of 2 main problems:
- The Microsoft authentication system (Mojang's Authlib is now deprecated);
- The new Minecraft version API (assets, libraries, natives...) has changed;

It seems easy to fix, but it's really more complex (plus the launcher code wasn't really optimised...).

However, I modded this launcher and now it works fine.

So I proudly present the [**Legacy Minecraft Launcher 1.6.94**](https://github.com/SkyVerseMc/LegacyMinecraftLauncher/releases/latest).

## How to install
Although most of the code can be found in this repo, I highly recommend downloading the [**latest version of the launcher**](https://github.com/SkyVerseMc/LegacyMinecraftLauncher/releases/latest), as all dependencies are not published here, as their code is not mine:
 - com.google.common
 - com.google.gson
 - com.google.thirdparty
 - javax.annotation
 - joptsimple
 - org.apache.commons
 - org.apache.logging.log4j
 
However, you can download the [libraries](https://github.com/SkyVerseMc/LegacyMinecraftLauncher/releases/latest/) and add them to your projects.

## How ??? (Using magic)
To fix the various problems I encountered, I had to use another dependency: the [Litarvan's OpenAuth](https://github.com/Litarvan/OpenAuth) (thanks a lot to him) to solve the authentication problem, and I modified some classes (a bit) to better fit the launcher code.

## What changed ?

In addition to the things already mentioned, I have:
- Added another version filter to enable or disable releases.
- Added a SquidHQ setting to allow access to blacklisted servers.

## Disclaimer
I've resurrected the old launcher with some modifications and optimised certain parts, but not all of it, which means that there may be crashes and so on. Don't hesitate to let me know if you have any problems with the launcher. I plan to optimize it further as time goes on. Also, don't cry when you read the code, 99% was made by the Mojang team.

And if you have this crash report:
```
Exception in thread "main" java.lang.ClassCastException: class jdk.internal.loader.ClassLoaders$AppClassLoader cannot be cast to class java.net.URLClassLoader (jdk.internal.loader.ClassLoaders$AppClassLoader and java.net.URLClassLoader are in module java.base of loader 'bootstrap')
	at net.minecraft.launchwrapper.Launch.<init>(Launch.java:24)
	at net.minecraft.launchwrapper.Launch.main(Launch.java:18)
```
It's because you are trying to launch old releases < 1.6, this crash is caused by the new java version.
(Will be fixed in future)

## Todo

 - [x] Microsoft Authentication
 - [x] New Minecraft Version API
 - [x] New Minecraft assets
 - [x] Patch Log4shell
 - [x] Change default JVM arguments to make them up to date
 - [x] Releases" filter in version selector
 - [x] SquidHQ option
 - [ ] Prevent releases 1.0 to 1.5.2 from crashing
