<p align="center">
<img width="256" height="256" alt="logo" src="https://github.com/user-attachments/assets/9946d4dc-16b5-4440-a235-8db6d48c3118" />
<br>
<a href="https://discord.gg/cwydhvYb2M">
<img alt="Discord" src="https://img.shields.io/discord/483238590214701057?style=for-the-badge&logo=discord">
</a>
<a href="https://patreon.com/ArcAnc?utm_medium=unknown&utm_source=join_link&utm_campaign=creatorshare_creator&utm_content=copyLink">
<img alt="Static Badge" src="https://img.shields.io/badge/Patreon-_?style=for-the-badge&logo=patreon&color=21252B">
</a>
<a href="https://ko-fi.com/arcanc">
<img alt="Static Badge" src="https://img.shields.io/badge/support_me_on_ko--fi-_?style=for-the-badge&logo=ko-fi&logoColor=white&color=F16061">
</a>
<a href="https://buymeacoffee.com/arcanc">
<img alt="Static Badge" src="https://img.shields.io/badge/buymeacoffee-_?style=for-the-badge&logo=buymeacoffee&logoColor=white&color=F16061">
</a>
</p>

<p align="center">
PulseLib is a modern animation library for Minecraft designed to deliver high performance while giving you the freedom to animate virtually any shape, model, or system.
</p>

---

This wiki documents **Minecraft 26.1.2 · PulseLib 1.1.5.3 · Java 25**, on the `26.1.2-multiloader` branch. The shared animation API follows `1.21.1-multiloader`; rendering uses the native 26.1 extract/submit pipeline. Fabric and NeoForge have separate platform adapters around the same Common code.

The port is still in progress. Current verification coverage and remaining work are recorded in the [migration journal](../docs/porting/26.1.2-multiloader.md) and [verification report](../docs/porting/26.1.2-multiloader-report.md). A successful check in one scene is not a promise of complete visual or performance coverage.

Installation instructions are available [here](installation.md).

## Documentation

* Setup: [Installation](installation.md), [Getting started](getting-started.md), [Model authoring](basic.md).
* Resources: [Plugins](plugins.md), [Resources and textures](resources.md), [Model data](modeldata.md), [Model loaders](model-loaders.md).
* Animation: [Animatables and controllers](animatables-and-controllers.md), [Molang](molang-animations.md), [Animation events](animation-events.md).
* Renderers: [Overview](renderers.md), [Blocks](pulselib-blocks.md), [Entities](pulselib-entities.md), [Items](pulselib-items.md), [Entity layers](entity-render-layers.md), [GUI](gui-rendering.md).
* Player features: [Player animations](player-animations.md), [Armor and attachments](armor-and-attachments.md), [Mesh deformers](mesh-deformers.md).
* Advanced rendering: [Render types and queue](render-types-and-queue.md), [Shaders](shaders.md), [API reference](api-reference.md).

---

To get started and explore the documentation, check it out [here](getting-started.md).

---

If you run into any issues or have questions while setting things up, feel free to join our [Discord](https://discord.com/invite/cwydhvYb2M) community for help and discussion.

---

Pull requests are always welcome! For larger changes, please open an issue first so we can discuss the proposed improvements.

If you would like to support the development of PulseLib, you can do so through the donation platforms listed above.

---

All code and artwork are distributed under the  
[![Static Badge](https://img.shields.io/badge/license-Arc's_License_of_Common_Sense-black?style=for-the-badge)](https://github.com/ArcAnc/PulseLib/blob/26.1.2-multiloader/LICENSE.txt)
