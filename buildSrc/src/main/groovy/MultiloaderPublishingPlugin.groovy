/**
 * @author ArcAnc
 * Created at: 21.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

import com.modrinth.minotaur.ModrinthExtension
import groovy.transform.CompileStatic
import net.darkhax.curseforgegradle.Constants
import net.darkhax.curseforgegradle.TaskPublishCurseForge
import net.darkhax.curseforgegradle.UploadArtifact
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.gradle.jvm.tasks.Jar

/** Configures loader-specific Modrinth and CurseForge releases. */
@CompileStatic
class MultiloaderPublishingPlugin implements Plugin<Project>
{
	@Override
	void apply(Project project)
	{
		project.pluginManager.apply('com.modrinth.minotaur')
		project.pluginManager.apply('net.darkhax.curseforgegradle')

		String loaderName = project.name
		String minecraftVersion = project.property('minecraft_version').toString()
		String modVersion = project.property('mod_version').toString()
		String modName = project.property('mod_name').toString()
		String releaseType = project.property('mod_release_type').toString()
		TaskProvider<Jar> gameJar = project.tasks.named('jar', Jar)
		TaskProvider<Jar> sourceJar = project.tasks.named('sourcesJar', Jar)

		ModrinthExtension modrinth = project.extensions.getByType(ModrinthExtension)
		modrinth.token.set(project.providers.gradleProperty('modrinthToken').
				orElse(project.providers.environmentVariable('MODRINTH_TOKEN')))
		modrinth.projectId.set('73TaZXLZ')
		modrinth.versionType.set(releaseType)
		modrinth.versionNumber.set("${minecraftVersion}-${modVersion}-${loaderName}".toString())
		modrinth.versionName.set("${modName}-${minecraftVersion}-${modVersion}-${loaderName}".toString())
		modrinth.uploadFile.set(gameJar)
		modrinth.additionalFiles.set([(Object)sourceJar])
		modrinth.changelog.set(project.rootProject.file('changelog.md').getText('UTF-8'))
		modrinth.gameVersions.set([minecraftVersion])
		modrinth.loaders.set([loaderName])
		if (loaderName == 'fabric')
			modrinth.required.project('fabric-api')

		project.tasks.register('publishCurseForge', TaskPublishCurseForge,
		{ TaskPublishCurseForge task ->
			task.apiToken = project.providers.gradleProperty('curseforgeToken').
					orElse(project.providers.environmentVariable('CURSEFORGE_TOKEN')).orNull
			task.dependsOn(gameJar, sourceJar)
			task.disableVersionDetection()
			UploadArtifact mainFile = task.upload('1484554', gameJar)
			mainFile.changelog = project.rootProject.file('changelog.md')
			mainFile.changelogType = Constants.CHANGELOG_MARKDOWN
			mainFile.displayName = "${modName}-${minecraftVersion}-${modVersion}-${loaderName}".toString()
			mainFile.releaseType = releaseType
			mainFile.addGameVersion(minecraftVersion)
			mainFile.addModLoader(loaderName == 'fabric' ? 'Fabric' : 'NeoForge')
			mainFile.addEnvironment('Client')
			mainFile.addEnvironment('Server')
			mainFile.addJavaVersion('Java 25')
			if (loaderName == 'fabric')
				mainFile.addRequirement('fabric-api')
			mainFile.withAdditionalFile(sourceJar)
		})
	}
}
