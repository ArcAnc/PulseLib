import {execFileSync} from 'node:child_process';
import {mkdir, readdir, readFile, rm, writeFile} from 'node:fs/promises';
import {fileURLToPath} from 'node:url';
import path from 'node:path';
import {versionedSidebars} from './versioned-sidebars.mjs';

const websiteDir = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const repositoryDir = path.resolve(websiteDir, '..');
const currentDocsRef = process.env.PULSELIB_CURRENT_DOCS_REF ?? 'HEAD';
const docSources = [
  {ref: currentDocsRef, destination: 'docs'},
  {ref: 'origin/26.1', destination: 'versioned_docs/version-26.1'},
  {ref: 'origin/1.21.1', destination: 'versioned_docs/version-1.21.1'},
];

function extractWiki(ref, destination) {
  const archive = execFileSync('git', ['archive', `${ref}:wiki`], {cwd: repositoryDir, maxBuffer: 32 * 1024 * 1024});
  execFileSync('tar', ['-x', '-C', destination], {cwd: websiteDir, input: archive});
}

async function markdownFiles(directory) {
  const entries = await readdir(directory, {withFileTypes: true});
  return (await Promise.all(entries.map(async (entry) => {
    const entryPath = path.join(directory, entry.name);
    return entry.isDirectory() ? markdownFiles(entryPath) : entry.name.endsWith('.md') ? [entryPath] : [];
  }))).flat();
}

async function normalizeMarkdown(directory, ref) {
  const sourceRef = ref === currentDocsRef ? 'master' : ref.replace(/^origin\//, '');
  await rm(path.join(directory, 'mkdocs.yml'), {force: true});
  for (const file of await markdownFiles(directory)) {
    let content = await readFile(file, 'utf8');
    content = content.
        replace(/<br>/g, '<br />').
        replace(/<img\b([^>]*?)(?<!\/)>/g, '<img$1 />').
        replace('(Entity-Render-Layers)', '(entity-render-layers.md)').
        replace(/\]\(\.\.\/(src\/[^)\s]+)\)/g, `](https://github.com/ArcAnc/PulseLib/blob/${sourceRef}/$1)`);
    if (path.basename(file) === 'index.md' && !content.startsWith('---\n')) content = `---\ntitle: PulseLib\nhide_title: true\nslug: /\n---\n\n${content}`;
    await writeFile(file, content);
  }
}

await Promise.all(['docs', 'versioned_docs', 'versioned_sidebars'].map((directory) => rm(path.join(websiteDir, directory), {recursive: true, force: true})));
for (const source of docSources) {
  const destination = path.join(websiteDir, source.destination);
  await mkdir(destination, {recursive: true});
  extractWiki(source.ref, destination);
  await normalizeMarkdown(destination, source.ref);
}
await mkdir(path.join(websiteDir, 'versioned_sidebars'), {recursive: true});
for (const [version, sidebar] of Object.entries(versionedSidebars)) {
  await writeFile(path.join(websiteDir, 'versioned_sidebars', `version-${version}-sidebars.json`), `${JSON.stringify({docs: sidebar}, null, 2)}\n`);
}
