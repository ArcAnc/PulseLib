/** @type {import('@docusaurus/plugin-content-docs').SidebarsConfig} */
module.exports = {
  docs: [
    {type: 'category', label: 'Start', items: ['index', 'installation', 'getting-started', 'basic']},
    {
      type: 'category', label: 'Core API', items: [
        'modeldata', 'animatables-and-controllers', 'animation-events', 'molang-animations',
        'renderers', 'render-types-and-queue', 'resources',
      ],
    },
    {
      type: 'category', label: 'Guides', items: [
        'pulselib-entities', 'pulselib-blocks', 'pulselib-items', 'entity-render-layers',
        'armor-and-attachments', 'player-animations', 'mesh-deformers', 'gui-rendering',
      ],
    },
    {type: 'category', label: 'Advanced', items: ['model-loaders', 'api-reference']},
  ],
};
