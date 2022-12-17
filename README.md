# Forge APIs for Fabric

A Minecraft mod that adds direct compatibility with Forge APIs.

## ABOUT THE PROJECT

**!!! Forge APIs for Fabric is in no way affiliated with the Minecraft Forge project or contributors !!!**

This is a direct port from Forge, all package names are the same, so you don't even have to readjust imports when
porting from Forge.
As Fabric is a whole different mod loader, there obviously have to be some differences, even though they're quite small.

For more information regarding the licensing of this project check the [LICENSING.md](LICENSING.md) file.

Thanks to Fuzs for porting the Forge Config API. This project is forked from it.

![Fuzs Presents](https://i.imgur.com/bUAnw7w.png "Fuzs Presents: Forge Config API Port")

## DEVELOPER INFORMATION

### Adding to your workspace

This mod is not currently ready for public use. If you'd like to use it as a stepping stone for now,
please clone this repo and use the `publishToMavenLocal` Gradle task.

### Working with Forge APIs for Fabric

#### Registering configs

The recommended point for registering your configs is directly in your `ModInitializer::onInitialize` method.

Registering your configs still works via a class called `net.minecraftforge.api.ModLoadingContext`, though the name is
only for mimicking Forge, as this is really only used for registering configs.

You'll have to provide the mod id of your mod, as there is no context which would be aware of the current mod.

```java
public static void registerConfig(String modId, ModConfig.Type type, IConfigSpec<?> spec)
```

And as on Forge there is also a version which supports a custom file name.

```java
public static void registerConfig(String modId, ModConfig.Type type, IConfigSpec<?> spec, String fileName)
```

#### Config loading

As Forge's mod loading process is split into multiple stages, configs aren't loaded immediately upon being registered.
On Fabric though, no such mod loading stages exist. Therefore, Forge Config API Port loads all registered configs
immediately.

#### Listening for config loading and reloading

Forge's `ModConfigEvent.Loading` and `ModConfigEvent.Reloading` events are both adapted for Fabric's callback event
style. They can be accessed from the `net.minecraftforge.api.fml.event.config.ModConfigEvent` class.

As on Forge, all these events provide is the config that is loading / reloading. But unlike on Forge, when processing
that config, you'll have to make sure it actually comes from your mod. This is important, as there is no mod specific
event bus on Fabric, meaning all events are fired for all mods subscribed to them.

As an example, a complete implementation of the reloading callback looks something like this:

```java
ModConfigEvent.RELOADING.register((ModConfig config) -> {
    if (config.getModId().equals(<modId>)) {
        ...
    }
});
```
