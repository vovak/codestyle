package codestyle.miner

import com.github.gumtreediff.tree.ITree
import java.io.File

data class BlobId(val id: String)

data class ChangeEntry(
        val id: Int,
        val commitId: String,
        val authorName: String,
        val authorEmail: String,
        val committerName: String,
        val committerEmail: String,
        val authorTime: Long,
        val committerTime: Long,
        val changeType: Char,
        val oldContentId: BlobId?,
        val newContentId: BlobId?,
        val oldPath: String?,
        val newPath: String?
)

fun ChangeEntry.toCsvLine(): String {
    return "$id,$commitId,$authorName,$authorEmail,$committerName,$committerEmail,$authorTime,$committerTime,$changeType,${oldContentId?.id},${newContentId?.id},$oldPath,$newPath"
}

const val CHANGE_ENTRY_CSV_HEADER = "id,commitId,authorName,authorEmail,committerName,committerEmail,authorTime,committerTime,changeType,oldContentId,newContentId,oldPath,newPath"

data class MethodChangeInfo(val methodIdBefore: MethodId?, val methodIdAfter: MethodId?,
                            val pathsCountBefore: Int, val pathsCountAfter: Int,
                            val pathsBefore: String, val pathsAfter: String)

data class FileChangeInfo(val changeEntryId: Int, val methodChanges: List<MethodChangeInfo>)

data class MethodId(val enclosingClassName: String, val methodName: String, val argTypes: Set<String>)

data class MethodInfo(val node: ITree, val id: MethodId)

fun isChanged(before: MethodInfo?, after: MethodInfo?): Boolean {
    if (before == null && after == null) return false
    if (before == null || after == null) return true
    return before.node.hash != after.node.hash || before.id != after.id
}

class MethodMapping(val before: MethodInfo?, val after: MethodInfo?) {
    val isChanged = isChanged(before, after)
}

fun dumpData(entries: List<ChangeEntry>, changes: List<FileChangeInfo>, pathStorage: PathStorage) {
    saveChangeEntries("change_metadata.csv", entries)
    saveFileChanges(changes)
    dumpPathStorage(pathStorage)
}

fun saveFileChanges(changes: List<FileChangeInfo>) {
    val chunkSize = 100_000
    val chunkedChanges = changes.chunked(chunkSize)
    val methodIdStorage: IncrementalIdStorage<MethodId> = IncrementalIdStorage()
    chunkedChanges.forEachIndexed { i, list ->
        println("Saving file change data: chunk ${i + 1} of ${chunkedChanges.size}... ")
        saveFileChangesChunk("file_changes_$i.csv", list, methodIdStorage)
        println("Done")
    }
    dumpMethodIdStorage(methodIdStorage, "method_ids.csv")
}

fun dumpMethodIdStorage(storage: IncrementalIdStorage<MethodId>, filename: String) {
    val header = "id,enclosingClass,methodName,argTypes"
    val lines = mutableListOf(header)
    storage.map.forEach {
        val id = it.value
        val methodId = it.key
        val line = "$id,${methodId.enclosingClassName},${methodId.methodName},${methodId.argTypes}"
        lines.add(line)
    }
    writeLinesToFile(filename, lines)
}

fun dumpStringIdStorage(storage: IncrementalIdStorage<String>, filename: String) {
    val header = "id,value"
    val lines = mutableListOf(header)
    storage.map.forEach {
        val id = it.value
        val stringValue = it.key
        lines.add("$id,$stringValue")
    }
    writeLinesToFile(filename, lines)
}

fun dumpNodeTypeStorage(storage: IncrementalIdStorage<NodeType>, filename: String) {
    val header = "id,type,direction"
    val lines = mutableListOf(header)
    storage.map.forEach {
        val id = it.value
        val nodeType = it.key
        lines.add("$id,${nodeType.type},${nodeType.direction}")
    }
    writeLinesToFile(filename, lines)
}

fun dumpPathIdStorage(storage: IncrementalIdStorage<List<Long>>, filename: String) {
    val header = "id,nodeTypes"
    val lines = mutableListOf(header)
    storage.map.forEach { entry ->
        val id = entry.value
        val nodeTypeIds = entry.key
        lines.add("$id,${nodeTypeIds.joinToString(" ")}")
    }
    writeLinesToFile(filename, lines)
}

fun dumpPathStorage(storage: PathStorage) {
    dumpStringIdStorage(storage.tokenIds, "tokens.csv")
    dumpNodeTypeStorage(storage.nodeTypeIds, "node_types.csv")
    dumpPathIdStorage(storage.pathIds, "path_ids.csv")
}

fun saveFileChangesChunk(filename: String, fileChanges: List<FileChangeInfo>, methodIdStorage: IncrementalIdStorage<MethodId>) {
    val header = "changeId,methodBeforeId,methodAfterId,pathsCountBefore,pathsCountAfter,pathsBefore,pathsAfter"
    val lines = mutableListOf(header)
    fileChanges.forEach { fileChange ->
        fileChange.methodChanges.forEach {
            val idBefore = if (it.methodIdBefore == null) -1 else methodIdStorage.record(it.methodIdBefore)
            val idAfter = if (it.methodIdAfter == null) -1 else methodIdStorage.record(it.methodIdAfter)
            val line = "${fileChange.changeEntryId},$idBefore,$idAfter,${it.pathsCountBefore},${it.pathsCountAfter},${it.pathsBefore},${it.pathsAfter}"
            lines.add(line)
        }
    }
    writeLinesToFile(filename, lines)
}

fun saveChangeEntries(filename: String, changeEntries: List<ChangeEntry>) {
    val lines = mutableListOf(CHANGE_ENTRY_CSV_HEADER)
    changeEntries.forEach { lines.add(it.toCsvLine()) }
    writeLinesToFile(filename, lines)
}

fun writeLinesToFile(filename: String, lines: List<String>) {
    val dirName = "out"
    val dir = File(dirName)
    dir.mkdirs()
    File("$dirName/$filename").printWriter().use { out ->
        lines.forEach { out.println(it) }
    }
}
