package com.github.saloed.diff

import name.fraser.neil.plaintext.DiffMatchPatch
import name.fraser.neil.plaintext.diff_match_patch

fun twoWayDiffFromDMPDiff(
    diffs: List<diff_match_patch.Diff>,
    leftName: String,
    rightName: String,
    mode: DiffMode
): TwoWayDiff {
    val dmp = DiffMatchPatch()
    val offsetDiffs = makeOffsetDiff(diffs, mode)
    val leftContentLines = dmp.diff_text1(diffs).lines()
    val rightContentLines = dmp.diff_text2(diffs).lines()
    val leftFile = DiffFile(leftName, leftContentLines)
    val rightFile = DiffFile(rightName, rightContentLines)
    return TwoWayDiff(leftFile, rightFile, offsetDiffs, mode)
}

private fun DiffRange.nextOffset(lines: List<String>) = when (lines.size) {
    0 -> DiffRange(endLine, endLineOffset, endLine, endLineOffset)
    1 -> DiffRange(endLine, endLineOffset, endLine, endLineOffset + lines.last().length)
    else -> DiffRange(endLine, endLineOffset, endLine + lines.lastIndex, lines.last().length)
}

private fun DiffRange.emptyOffset() = DiffRange(endLine, endLineOffset, endLine, endLineOffset)

private fun makeOffsetDiff(diffs: List<diff_match_patch.Diff>, mode: DiffMode): List<Diff> {
    val result = mutableListOf<Diff>()
    var currentLeftOffset = DiffRange(0, 0, 0, 0)
    var currentRightOffset = DiffRange(0, 0, 0, 0)

    for (diff in diffs) {
        val lines = diff.text.lines()
        val type = diff.operation.diffType()
        when (type) {
            DiffType.EQUAL -> {
                currentLeftOffset = currentLeftOffset.nextOffset(lines)
                currentRightOffset = currentRightOffset.nextOffset(lines)
            }
            DiffType.DELETE -> {
                currentLeftOffset = currentLeftOffset.nextOffset(lines)
                currentRightOffset = currentRightOffset.emptyOffset()
            }
            DiffType.INSERT -> {
                currentRightOffset = currentRightOffset.nextOffset(lines)
                currentLeftOffset = currentLeftOffset.emptyOffset()
            }
        }
        check(currentLeftOffset.isCorrectInMode(mode) && currentRightOffset.isCorrectInMode(mode)) { "Incorrect range" }
        result += Diff(type, currentLeftOffset, currentRightOffset)
    }
    return result
}

private fun diff_match_patch.Operation.diffType() = when (this) {
    diff_match_patch.Operation.DELETE -> DiffType.DELETE
    diff_match_patch.Operation.INSERT -> DiffType.INSERT
    diff_match_patch.Operation.EQUAL -> DiffType.EQUAL
}
