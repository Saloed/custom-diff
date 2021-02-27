package com.github.saloed.diff

import name.fraser.neil.plaintext.DiffMatchPatch
import name.fraser.neil.plaintext.diff_match_patch
import org.junit.jupiter.api.Test
import java.nio.file.Paths


class TwoWayDiffTest {

    @Test
    fun testConverterFromDMP() {
        val leftFile = "D:\\projects\\kex\\refinements\\.z3-trace-bad".let { Paths.get(it) }
        val rightFile = "D:\\projects\\kex\\refinements\\.z3-trace-good".let { Paths.get(it) }
        val resultFile = "D:\\projects\\kex\\refinements\\trace_cmp.jdiff".let { Paths.get(it) }

        val lhsSections = makeSections(leftFile.toFile().readLines(Charsets.UTF_8))
        val rhsSections = makeSections(rightFile.toFile().readLines(Charsets.UTF_8))

        val leftHeaders = lhsSections.joinToString("\n") { it.header }
        val rightHeaders = rhsSections.joinToString("\n") { it.header }
        val linesDiff = DiffMatchPatch().diffLineLevel(leftHeaders, rightHeaders)
        val headerDiff = makeHeaderDiff(linesDiff)

        val sectionGroups = groupSections(headerDiff, lhsSections, rhsSections)
        check(sectionGroups.filter { it.left != null }.map { it.left!! } == lhsSections) { "Left groups mismatch" }
        check(sectionGroups.filter { it.right != null }.map { it.right!! } == rhsSections) { "Right groups mismatch" }
        val diffs = sectionGroups.flatMap { it.asDiff() }
        val twoWay = twoWayDiffFromDMPDiff(diffs, "left", "right", DiffMode.LINE)

        val leftText = twoWay.diff.map { it.left }.joinToString("") { it.apply(twoWay.left.contentLines) }
        val rightText = twoWay.diff.map { it.right }.joinToString("") { it.apply(twoWay.right.contentLines) }
        val originalLeftText = leftFile.toFile().readLines(Charsets.UTF_8).joinToString("\n")
        val originalRightText = rightFile.toFile().readLines(Charsets.UTF_8).joinToString("\n")
        val (leftCommon, leftDelta, leftOriginDelta) = stringDelta(leftText, originalLeftText)
        val (rightCommon, rightDelta, rightOriginDelta) = stringDelta(rightText, originalRightText)
        val jsonStr = twoWay.saveToJson()
        resultFile.toFile().writeText(jsonStr)
    }

}

fun stringDelta(left: String, right: String): Triple<String, String, String> {
    val common = left.commonPrefixWith(right)
    val leftDelta = left.substring(common.length)
    val rightDelta = right.substring(common.length)
    return Triple(common, leftDelta, rightDelta)
}

data class HeaderDiff(val left: String?, val right: String?)

data class FileSection(val header: String, val content: List<String>) {
    fun contentString() = content.joinToString(separator = "") { "$it\n" }
    fun toDiff(operation: diff_match_patch.Operation) = diff_match_patch.Diff(operation, contentString())
}

fun contentDiff(left: FileSection, right: FileSection): List<diff_match_patch.Diff> {
    val leftContent = left.contentString()
    val rightContent = right.contentString()
    return DiffMatchPatch().diffLineLevel(leftContent, rightContent)
}

data class SectionGroup(val left: FileSection?, val right: FileSection?) {
    fun asDiff(): List<diff_match_patch.Diff> = when {
        left != null && right != null -> contentDiff(left, right)
        left == null && right != null -> listOf(right.toDiff(diff_match_patch.Operation.INSERT))
        left != null && right == null -> listOf(left.toDiff(diff_match_patch.Operation.DELETE))
        else -> emptyList()
    }
}

private val headerLineRegex = Regex("^-------- \\[[\\w\\d_-]+\\] .* ---------$")

private fun String.isHeaderLine() = headerLineRegex.matches(this)

private fun diff_match_patch.Diff.lines() = text.lines().dropLastWhile { it.isEmpty() }

private fun makeHeaderDiff(diff: List<diff_match_patch.Diff>): List<HeaderDiff> {
    val result = mutableListOf<HeaderDiff>()
    for (diffItem in diff) {
        val headers = diffItem.lines()
        result += headers.map {
            when (diffItem.operation) {
                diff_match_patch.Operation.EQUAL -> HeaderDiff(it, it)
                diff_match_patch.Operation.DELETE -> HeaderDiff(it, null)
                diff_match_patch.Operation.INSERT -> HeaderDiff(null, it)
            }
        }
    }
    return result
}

private fun groupSections(
    diff: List<HeaderDiff>,
    leftSections: List<FileSection>,
    rightSections: List<FileSection>
): List<SectionGroup> {
    val leftIter = leftSections.iterator()
    val rightIter = rightSections.iterator()
    return diff.map { item ->
        val left = item.left?.let { header ->
            leftIter.next().also { check(it.header == header) { "Left header mismatch: $header | ${it.header}" } }
        }
        val right = item.right?.let { header ->
            rightIter.next().also { check(it.header == header) { "Right header mismatch: $header | ${it.header}" } }
        }
        SectionGroup(left, right)
    }
}

private fun makeSections(lines: List<String>): List<FileSection> {
    val sections = mutableListOf<FileSection>()
    var currentHeader = ""
    val currentSectionContent = mutableListOf<String>()
    var firstSection = true
    for (line in lines) {
        val isHeader = line.isHeaderLine()
        if (isHeader && !firstSection) {
            sections += FileSection(currentHeader, currentSectionContent.toList())
            currentHeader = line
            currentSectionContent.clear()
        }
        if (isHeader && firstSection) {
            firstSection = false
            currentHeader = line
            currentSectionContent.clear()
        }
        currentSectionContent += line
    }
    if (currentSectionContent.isNotEmpty()) {
        sections += FileSection(currentHeader, currentSectionContent.toList())
    }
    return sections
}
