import {themes as prismThemes} from 'prism-react-renderer';

/** @type {import('@docusaurus/types').Config} */
const config = {
  title: 'PulseLib',
  tagline: 'Animation library for Minecraft mods',
  favicon: 'img/logo.svg',
  url: 'https://arcanc.github.io',
  baseUrl: '/PulseLib/',
  organizationName: 'ArcAnc',
  projectName: 'PulseLib',
  trailingSlash: true,
  headTags: [
    {
      tagName: 'meta',
      attributes: {
        name: 'algolia-site-verification',
        content: 'FDB67755A669D6DD',
      },
    },
  ],
  onBrokenLinks: 'throw',
  markdown: {hooks: {onBrokenMarkdownLinks: 'warn'}},
  i18n: {defaultLocale: 'en', locales: ['en']},
  presets: [[
    'classic',
    {
      docs: {
        path: './docs',
        routeBasePath: '/',
        sidebarPath: './sidebars.js',
        lastVersion: 'current',
        versions: {
          current: {label: '26.2', path: '26.2'},
          '26.1': {banner: 'none'},
          '1.21.1': {banner: 'none'},
        },
      },
      blog: false,
      theme: {customCss: './src/css/custom.css'},
    },
  ]],
  themeConfig: {
    image: 'img/logo.svg',
    colorMode: {
      respectPrefersColorScheme: true,
    },
    navbar: {
      title: 'PulseLib',
      logo: {alt: 'PulseLib logo', src: 'img/logo.svg', href: '/26.2/'},
      items: [
        {type: 'docSidebar', sidebarId: 'docs', position: 'left', label: 'Documentation'},
        {type: 'docsVersionDropdown', position: 'left'},
        {type: 'search', position: 'right'},
        {href: 'https://github.com/ArcAnc/PulseLib', label: 'GitHub', position: 'right'},
      ],
    },
    footer: {
      style: 'dark',
      links: [
        {title: 'Community', items: [{label: 'Discord', href: 'https://discord.gg/cwydhvYb2M'}]},
        {title: 'More', items: [{label: 'GitHub', href: 'https://github.com/ArcAnc/PulseLib'}]},
      ],
      copyright: `Copyright © ${new Date().getFullYear()} ArcAnc. Built with Docusaurus.`,
    },
    prism: {theme: prismThemes.github, darkTheme: prismThemes.dracula, additionalLanguages: ['java', 'json']},
  },
};

export default config;
