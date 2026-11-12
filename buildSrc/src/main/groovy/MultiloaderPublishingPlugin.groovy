/**
 * @author ArcAnc
 * Created at: 12.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

import com.modrinth.minotaur.ModrinthExtension

import net.darkhax.curseforgegradle.Constants
import net.darkhax.curseforgegradle.TaskPublishCurseForge

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Configures Modrinth and CurseForge publishing for each loader's game and source JARs.
 */
class MultiloaderPublishingPlugin implements Plugin<Project>
{
	@Override
	void apply(Project project)
	{
		project.pluginManager.apply('com.modrinth.minotaur')
		project.pluginManager.apply('net.darkhax.curseforgegradle')

		def loaderName = project.name
		def minecraftVersion = project.property('minecraft_version').toString()
		def modVersion = project.property('mod_version').toString()
		def modName = project.property('mod_name').toString()
		def releaseType = project.property('mod_release_type').toString()
		def gameJar = project.tasks.named(loaderName == 'fabric' ? 'remapJar' : 'jar')
		def sourceJar = project.tasks.named(loaderName == 'fabric' ? 'remapSourcesJar' : 'sourcesJar')

		ModrinthExtension modrinth = project.extensions.getByType(ModrinthExtension)
		modrinth.token.set(project.providers.gradleProperty('modrinthToken').
				orElse(project.providers.environmentVariable('MODRINTH_TOKEN')))
		modrinth.projectId.set('73TaZXLZ')
		modrinth.versionType.set(releaseType)
		modrinth.versionNumber.set("${minecraftVersion}-${modVersion}-${loaderName}")
		modrinth.versionName.set("${modName}-${minecraftVersion}-${modVersion}-${loaderName}")
		modrinth.uploadFile.set(gameJar)
		modrinth.additionalFiles.set([sourceJar])
		modrinth.changelog.set(project.rootProject.file('changelog.md').text)
		modrinth.gameVersions.set([minecraftVersion])
		modrinth.loaders.set([loaderName])
		if (loaderName == 'fabric')
		{
			modrinth.required.project('fabric-api')
		}

		project.tasks.register('publishCurseForge', TaskPublishCurseForge)
		{ TaskPublishCurseForge task ->
			task.apiToken = project.providers.gradleProperty('curseforgeToken').
					orElse(project.providers.environmentVariable('CURSEFORGE_TOKEN')).orNull
			task.dependsOn(gameJar, sourceJar)
			task.disableVersionDetection()
			def mainFile = task.upload('1484554', gameJar)
			mainFile.changelog = project.rootProject.file('changelog.md')
			mainFile.changelogType = Constants.CHANGELOG_MARKDOWN
			mainFile.displayName = "${modName}-${minecraftVersion}-${modVersion}-${loaderName}"
			mainFile.releaseType = releaseType
			mainFile.addGameVersion(minecraftVersion)
			mainFile.addModLoader(loaderName == 'fabric' ? 'Fabric' : 'NeoForge')
			mainFile.addEnvironment('Client')
			mainFile.addEnvironment('Server')
			mainFile.addJavaVersion('Java 21')
			if (loaderName == 'fabric')
			{
				mainFile.addRequirement('fabric-api')
			}
			mainFile.withAdditionalFile(sourceJar)
		}
	}
}
