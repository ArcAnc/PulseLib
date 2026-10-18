# Getting Started

PulseLib, and this wiki, assumes you already have a basic understanding of the following:

* Core Java concepts
* General Minecraft mechanics
* Modding Minecraft 1.21.1 using Fabric or NeoForge, with Java 21

If any of these are new to you, it's recommended to learn them first through some [modding tutorials](https://wiki.mcjty.eu/modding/index.php?title=YouTube-Tutorials)

Start with [Installation](installation.md), then add a [client plugin](plugins.md). Plugins register the models and texture references that PulseLib loads on resource reload; merely placing a model in a resource directory does not load it.

## First animated model

1. Export a [supported model](basic.md) and place it under the [resource paths](resources.md) for its loader.
2. Declare its [model data](modeldata.md) and register it with `PModelRegistrar` in your plugin.
3. Implement an [animatable and its controllers](animatables-and-controllers.md).
4. Connect an [item](pulselib-items.md), [entity](pulselib-entities.md), or [block entity](pulselib-blocks.md) renderer. For additions to vanilla models, use [attachments](armor-and-attachments.md) or [player animations](player-animations.md).

The animation and rendering API lives in `common`; loader adapters provide discovery and Minecraft lifecycle hooks. Keep renderer classes and client plugins out of dedicated-server initialization.

## Creating Models with Blockbench

PulseLib uses Blockbench for creating models and animations.

For guidance on using Blockbench with PulseLib, check out the official [Blockbench Quickstart](https://www.blockbench.net/quickstart/) page.

## Marking Your Project

Want to let others know that your mod depends on PulseLib?

You can use the image below on your ****CurseForge**** or ****Modrinth**** project page:

<img width="500" height="168" alt="pulselib" src="https://github.com/user-attachments/assets/562e54a1-1b90-4eff-9ca2-c8677426aee0" />
