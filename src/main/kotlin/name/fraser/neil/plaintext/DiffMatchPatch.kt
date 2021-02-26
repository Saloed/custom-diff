package name.fraser.neil.plaintext

import java.util.*

class DiffMatchPatch : diff_match_patch() {

    fun diffLineLevel(left: String, right: String): List<Diff> {
        val a = diff_linesToChars(left, right)
        val lineText1 = a.chars1
        val lineText2 = a.chars2
        val lineArray = a.lineArray
        val diffs = diff_main(lineText1, lineText2, false)
        diff_charsToLines(diffs, lineArray)
        return diffs
    }

    fun diffCharLevel(left: String, right: String): List<Diff> = diff_main(left, right)

    fun makePatch(diff: List<Diff>): List<Patch> = patch_make(LinkedList(diff))

    fun patchToText(patch: List<Patch>): String = patch_toText(patch)
}
