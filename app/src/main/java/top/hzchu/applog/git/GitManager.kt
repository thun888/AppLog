package top.hzchu.applog.git

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.ResetCommand
import org.eclipse.jgit.lib.PersonIdent
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.transport.RefSpec
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import org.eclipse.jgit.treewalk.TreeWalk
import org.eclipse.jgit.treewalk.filter.PathFilter
import java.io.File

data class CommitInfo(
    val id: String,
    val shortId: String,
    val message: String,
    val author: String,
    val timestamp: Long,
    val tags: List<String> = emptyList()
)

class GitManager(private val context: Context) {

    companion object {
        const val SNAPSHOT_FILE = "apps_snapshot.txt"
        const val REPO_DIR = "git_repo"
        const val DEFAULT_BRANCH = "main"
    }

    private val repoDir: File
        get() = File(context.filesDir, REPO_DIR)

    private val git: Git?
        get() = if (repoDir.exists() && File(repoDir, ".git").exists()) {
            try { Git.open(repoDir) } catch (_: Exception) { null }
        } else null

    suspend fun init(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!repoDir.exists()) repoDir.mkdirs()
            val existingGit = git
            if (existingGit != null) {
                existingGit.close()
                return@withContext Result.success(Unit)
            }
            Git.init()
                .setDirectory(repoDir)
                .setInitialBranch(DEFAULT_BRANCH)
                .call()
                .close()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun commitSnapshot(
        content: String,
        message: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val g = git ?: return@withContext Result.failure(Exception("Repo not initialized"))
            val snapshotFile = File(repoDir, SNAPSHOT_FILE)
            snapshotFile.writeText(content)
            g.add().addFilepattern(SNAPSHOT_FILE).call()
            val status = g.status().call()
            if (!status.hasUncommittedChanges()) {
                return@withContext Result.success("")
            }
            val person = PersonIdent("AppLog", "applog@local")
            val commit = g.commit()
                .setMessage(message)
                .setAuthor(person)
                .setCommitter(person)
                .setAllowEmpty(false)
                .call()
            Result.success(commit.name)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCommitHistory(maxCount: Int = 100): Result<List<CommitInfo>> = withContext(Dispatchers.IO) {
        try {
            val g = git ?: return@withContext Result.success(emptyList())
            val commits = mutableListOf<CommitInfo>()
            val logIter = g.log().setMaxCount(maxCount).call()
            val tagMap = getTagMap(g)

            for (revCommit in logIter) {
                val tags = tagMap[revCommit.name] ?: emptyList()
                commits.add(
                    CommitInfo(
                        id = revCommit.name,
                        shortId = revCommit.abbreviate(7).name(),
                        message = revCommit.shortMessage,
                        author = revCommit.authorIdent.name,
                        timestamp = revCommit.commitTime.toLong() * 1000,
                        tags = tags
                    )
                )
            }
            Result.success(commits)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSnapshotContent(commitId: String? = null): Result<String> = withContext(Dispatchers.IO) {
        try {
            val g = git ?: return@withContext Result.success("")
            if (commitId == null) {
                val snapshotFile = File(repoDir, SNAPSHOT_FILE)
                return@withContext if (snapshotFile.exists()) {
                    Result.success(snapshotFile.readText())
                } else {
                    Result.success("")
                }
            }

            val repo = g.repository
            val objectId = repo.resolve(commitId)
                ?: return@withContext Result.failure(Exception("Commit not found: $commitId"))

            val revWalk = RevWalk(repo)
            val commit = revWalk.parseCommit(objectId)

            val treeWalk = TreeWalk(repo)
            treeWalk.addTree(commit.tree)
            treeWalk.setRecursive(true)
            treeWalk.filter = PathFilter.create(SNAPSHOT_FILE)

            val content = if (treeWalk.next()) {
                val blobId = treeWalk.getObjectId(0)
                val loader = repo.open(blobId)
                String(loader.bytes)
            } else {
                ""
            }
            treeWalk.close()
            revWalk.close()
            Result.success(content)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createTag(commitId: String, tagName: String, message: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val g = git ?: return@withContext Result.failure(Exception("Repo not initialized"))
            val objectId = g.repository.resolve(commitId)
                ?: return@withContext Result.failure(Exception("Commit not found"))
            val revWalk = RevWalk(g.repository)
            val revObject = revWalk.parseAny(objectId)
            g.tag()
                .setName(tagName)
                .setObjectId(revObject)
                .setMessage(message)
                .call()
            revWalk.close()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun pushToRemote(
        remoteUrl: String,
        username: String,
        password: String,
        force: Boolean = false
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val g = git ?: return@withContext Result.failure(Exception("Repo not initialized"))
            val remoteName = "origin"
            val remoteList = g.remoteList().call()
            if (remoteList.any { it.name == remoteName }) {
                g.remoteSetUrl()
                    .setRemoteName(remoteName)
                    .setRemoteUri(org.eclipse.jgit.transport.URIish(remoteUrl))
                    .call()
            } else {
                g.remoteAdd()
                    .setName(remoteName)
                    .setUri(org.eclipse.jgit.transport.URIish(remoteUrl))
                    .call()
            }

            val pushCommand = g.push()
                .setRemote(remoteName)
                .setCredentialsProvider(UsernamePasswordCredentialsProvider(username, password))

            val spec = if (force) {
                "+refs/heads/${DEFAULT_BRANCH}:refs/heads/${DEFAULT_BRANCH}"
            } else {
                "refs/heads/${DEFAULT_BRANCH}:refs/heads/${DEFAULT_BRANCH}"
            }
            pushCommand.setRefSpecs(RefSpec(spec))
            pushCommand.call()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun pullFromRemote(
        remoteUrl: String,
        username: String,
        password: String,
        force: Boolean = false
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val g = git ?: return@withContext Result.failure(Exception("Repo not initialized"))
            val remoteName = "origin"
            val remoteList = g.remoteList().call()
            if (remoteList.any { it.name == remoteName }) {
                g.remoteSetUrl()
                    .setRemoteName(remoteName)
                    .setRemoteUri(org.eclipse.jgit.transport.URIish(remoteUrl))
                    .call()
            } else {
                g.remoteAdd()
                    .setName(remoteName)
                    .setUri(org.eclipse.jgit.transport.URIish(remoteUrl))
                    .call()
            }

            if (force) {
                g.fetch()
                    .setRemote(remoteName)
                    .setCredentialsProvider(UsernamePasswordCredentialsProvider(username, password))
                    .call()
                g.reset()
                    .setMode(ResetCommand.ResetType.HARD)
                    .setRef("origin/${DEFAULT_BRANCH}")
                    .call()
            } else {
                g.pull()
                    .setRemote(remoteName)
                    .setCredentialsProvider(UsernamePasswordCredentialsProvider(username, password))
                    .call()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun generateCommitMessage(added: Int, removed: Int, updated: Int): String {
        val parts = mutableListOf<String>()
        if (added > 0) parts.add("+$added apps")
        if (removed > 0) parts.add("-$removed apps")
        if (updated > 0) parts.add("$updated updated")
        val summary = parts.joinToString(", ").ifEmpty { "no changes" }
        return "[AutoCommit] $summary"
    }

    private fun getTagMap(git: Git): Map<String, List<String>> {
        return try {
            val tags = git.tagList().call()
            val map = mutableMapOf<String, MutableList<String>>()
            val revWalk = RevWalk(git.repository)

            for (ref in tags) {
                val tagName = ref.name.removePrefix("refs/tags/")
                val peeledRef = git.repository.refDatabase.peel(ref)
                val objectId = peeledRef?.peeledObjectId ?: peeledRef?.objectId ?: continue
                map.getOrPut(objectId.name) { mutableListOf() }.add(tagName)
            }
            revWalk.close()
            map
        } catch (_: Exception) {
            emptyMap()
        }
    }

    fun close() {
        try {
            git?.close()
        } catch (_: Exception) {
            // ignore
        }
    }
}
