# Basic

## Table of Content
* [Full Video Tutorial](#full-video-tutorial)
* [Rigging](#rigging)
    - [Grouping](#grouping)
    - [Parenting and Pivots](#parenting-and-pivots)
* [Animating a Model in Blockbench](#animating-a-model-in-blockbench)
* [Exporting the Model](#exporting-model)

## Full Video Tutorial

For a complete walkthrough of model creation, check out this YouTube playlist: [click](https://www.youtube.com/watch?v=dsax5p4brN8&list=PLvULVkjBtg2SezfUA8kHcPUGpxIS26uJR)

## Rigging

Rigging is the process of preparing your model for animation — essentially creating a skeleton for it. Spending time on proper rigging makes animating much easier later.

### Grouping

Models are made up of cubes and groups. Only groups can be animated, so make sure to place all cubes inside groups.

<details>
<summary>Click</summary>

<img width="632" height="687" alt="изображение" src="https://github.com/user-attachments/assets/c5f8a550-9095-41a4-a212-d40c55a2d857" />

</details>

### Parenting and Pivots

Think of your model's rig as a skeleton:
* Groups = bones
* Pivots = joints
* Cubes = flesh

<details>
<summary>MasterianoX Tip!</summary>
<img width="1000" height="1000" alt="изображение" src="https://github.com/user-attachments/assets/5a666591-b039-4d9f-b802-ecb9a7ab2621" />

</details>
If your model has multiple moving parts (like a school of fish), you might want multiple root groups. Otherwise, a single root with nested child groups works best. When a parent group moves, all its children move with it.

Pivot points control the rotation origin of each group. They can be set with the **Pivot Tool**.
<details>
<summary>MasterianoX Tip!</summary>
<img width="1000" height="1000" alt="изображение" src="https://github.com/user-attachments/assets/96311bd9-3f42-43c5-8cd5-531ed5e2fdc4" />

</details>


## Animating a Model in Blockbench

Animations are created in the Animation tab on the right.

PulseLib animations follow the same principles as Bedrock entity animations, so most Bedrock animation tutorials also apply here.

## Exporting model

Once your model is ready, export it as **`.glb`** or **`.gltf`**. A `.glb` is preferred for a self-contained binary asset; a `.gltf` can be useful when its JSON source should remain inspectable.
Recommended Blockbench export settings:

<img width="479" height="395" alt="изображение" src="https://github.com/user-attachments/assets/cd2c5d2e-99cf-43c8-af1f-dc5f57ef772f" />
<img width="479" height="287" alt="изображение" src="https://github.com/user-attachments/assets/ebb8f0bf-843f-4b2e-831f-454823aad96f" />

After exporting, your model is ready to be used with PulseLib.
