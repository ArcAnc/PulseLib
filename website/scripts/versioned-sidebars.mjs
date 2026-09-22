const common = [
  {type: 'category', label: 'Start', items: ['index', 'installation', 'getting-started', 'basic']},
  {
    type: 'category', label: 'Guides', items: [
      'pulselib-entities', 'pulselib-blocks', 'pulselib-items', 'entity-render-layers',
      'armor-and-attachments', 'player-animations', 'mesh-deformers', 'gui-rendering',
    ],
  },
];

export const versionedSidebars = {
  '26.1': [
    common[0],
    {
      type: 'category', label: 'Core API', items: [
        'modeldata', 'animatables-and-controllers', 'animation-events', 'molang-animations',
        'renderers', 'render-types-and-queue', 'resources',
      ],
    },
    common[1],
    {type: 'category', label: 'Advanced', items: ['model-loaders', 'api-reference']},
  ],
  '1.21.1': [
    common[0],
    {
      type: 'category', label: 'Core API', items: [
        'modeldata', 'animatables-and-controllers', 'animation-events', 'renderers',
        'render-types-and-queue', 'shaders', 'resources',
      ],
    },
    common[1],
    {type: 'category', label: 'Advanced', items: ['render-backend', 'model-loaders', 'api-reference']},
  ],
};
