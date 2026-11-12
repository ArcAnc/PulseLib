# Getting Started

These examples target Minecraft 26.1.2 with Java 25. You need basic Java and familiarity with your loader's mod setup: Fabric or NeoForge. Install the matching PulseLib artifact using [Installation](installation.md).

## Integration order

1. Export a glTF/GLB or Gecko model and its animations. See [Model authoring](basic.md) and [Model loaders](model-loaders.md).
2. Put models and textures under `assets/<modid>/`. Declare their canonical IDs and texture mappings in an automatically discovered `@PulsePlugin` implementing `IPulseClientPlugin`. See [Plugins](plugins.md) and [Resources](resources.md).
3. Implement `PAnimatable<T>` and register controller factories. Entities and block entities normally own an instance manager; items resolve managers per stack key. See [Animatables and controllers](animatables-and-controllers.md).
4. Implement a renderer with its render-state type, then register it in your loader's client setup. Choose [Blocks](pulselib-blocks.md), [Entities](pulselib-entities.md), or [Items](pulselib-items.md).
5. For existing vanilla living renderers, use [Armor and attachments](armor-and-attachments.md) or [Player animations](player-animations.md) instead of replacing the renderer.

Examples omit most imports and use mod-owned placeholders such as `MyItems` and `MyEntities`. Renderer classes and their states are client code; keep the native types and generic parameters shown in the examples.

## MultiLoader boundaries

`common/` owns the model, animation, plugin and rendering APIs. `fabric/` and `neoforge/` own loader discovery, lifecycle and platform registration. A PulseLib plugin registers PulseLib content on both loaders; ordinary Minecraft blocks, items, entities and renderer factories still use each loader's API.

Keep client plugins and renderer classes out of dedicated-server initialization. A shared or server plugin implements `IPulseCommonPlugin` and must not initialize client classes. Rendering uses `Identifier`, native render states, and `SubmitNodeCollector`; old BEWLR-based item tutorials need adaptation.

## Creating models with Blockbench

Blockbench can author skeletons and animation clips for PulseLib. Start with the [model authoring guide](basic.md); the bundled [player rig](player-animations.md#pulselib-player-action-rig) is available for player actions.

## Marking your project

You can use this image on your CurseForge or Modrinth project page to indicate a PulseLib dependency:

<img width="500" height="168" alt="pulselib" src="https://github.com/user-attachments/assets/562e54a1-1b90-4eff-9ca2-c8677426aee0" />
