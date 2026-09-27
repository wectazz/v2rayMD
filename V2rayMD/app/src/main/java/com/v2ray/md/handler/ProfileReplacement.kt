package com.v2ray.md.handler

import com.v2ray.md.dto.ProfileDiffEntry
import com.v2ray.md.dto.entities.ProfileItem

internal object ProfileReplacement {

    /**
     * Compares profile snapshots taken before and after a subscription update.
     *
     * GUIDs are regenerated on every import, so identity is the profile content
     * (remarks, server, port, password). Duplicates are matched as a multiset:
     * only the surplus on either side is reported.
     *
     * @return added entries and deleted entries with display names.
     */
    fun diffProfiles(
        subscriptionName: String,
        before: List<ProfileItem>,
        after: List<ProfileItem>,
    ): Pair<List<ProfileDiffEntry>, List<ProfileDiffEntry>> {
        fun ProfileItem.identityKey() = listOf(
            remarks.orEmpty(), server.orEmpty(), serverPort.orEmpty(), password.orEmpty()
        ).joinToString("\u0001")

        fun ProfileItem.displayName() = remarks.ifBlank {
            if (server.isNullOrBlank()) "" else "$server:${serverPort.orEmpty()}".trimEnd(':')
        }

        val beforeCounts = before.groupingBy { it.identityKey() }.eachCount().toMutableMap()
        val added = mutableListOf<ProfileDiffEntry>()
        for (profile in after) {
            val remaining = beforeCounts[profile.identityKey()] ?: 0
            if (remaining > 0) {
                beforeCounts[profile.identityKey()] = remaining - 1
            } else {
                added.add(ProfileDiffEntry(subscriptionName, profile.displayName()))
            }
        }
        val afterCounts = after.groupingBy { it.identityKey() }.eachCount().toMutableMap()
        val deleted = mutableListOf<ProfileDiffEntry>()
        for (profile in before) {
            val remaining = afterCounts[profile.identityKey()] ?: 0
            if (remaining > 0) {
                afterCounts[profile.identityKey()] = remaining - 1
            } else {
                deleted.add(ProfileDiffEntry(subscriptionName, profile.displayName()))
            }
        }
        return added to deleted
    }

    /**
     * Finds the profile that should become selected after publishing a replacement batch.
     * The first profile becomes selected when the store has no current selection.
     */
    fun findSelectedReplacement(
        profiles: Map<String, ProfileItem>,
        currentSelection: String?,
        selectedProfile: ProfileItem?,
    ): String? {
        if (profiles.isEmpty()) return null
        if (currentSelection.isNullOrBlank()) return profiles.keys.first()
        if (selectedProfile == null) return null

        if (selectedProfile.remarks.isNotBlank()) {
            profiles.entries.firstOrNull { (_, candidate) ->
                isSameText(candidate.remarks, selectedProfile.remarks) &&
                        isSameText(candidate.server, selectedProfile.server) &&
                        isSameText(candidate.serverPort, selectedProfile.serverPort) &&
                        isSameText(candidate.password, selectedProfile.password)
            }?.key?.let { return it }

            profiles.entries.firstOrNull { (_, candidate) ->
                isSameText(candidate.remarks, selectedProfile.remarks)
            }?.key?.let { return it }
        }

        profiles.entries.firstOrNull { (_, candidate) ->
            isSameText(candidate.server, selectedProfile.server) &&
                    isSameText(candidate.serverPort, selectedProfile.serverPort) &&
                    isSameText(candidate.password, selectedProfile.password)
        }?.key?.let { return it }

        profiles.entries.firstOrNull { (_, candidate) ->
            isSameText(candidate.server, selectedProfile.server) &&
                    isSameText(candidate.serverPort, selectedProfile.serverPort)
        }?.key?.let { return it }

        profiles.entries.firstOrNull { (_, candidate) ->
            isSameText(candidate.server, selectedProfile.server)
        }?.key?.let { return it }

        return profiles.keys.firstOrNull()
    }

    /**
     * Finds replaced payloads that are safe to remove.
     *
     * A null cross-group reference set means that at least one raw group index could not
     * be read. In that case deletion fails closed.
     */
    fun findRemovablePayloads(
        replacedServers: Collection<String>,
        replacementServers: Set<String>,
        protectedServer: String?,
        serversReferencedByOtherGroups: Set<String>?,
    ): Set<String> {
        if (serversReferencedByOtherGroups == null) return emptySet()

        return replacedServers.filterTo(linkedSetOf()) { guid ->
            guid != protectedServer &&
                    guid !in replacementServers &&
                    guid !in serversReferencedByOtherGroups
        }
    }

    private fun isSameText(left: String?, right: String?): Boolean {
        if (left.isNullOrBlank() || right.isNullOrBlank()) return false
        return left.trim().equals(right.trim(), ignoreCase = true)
    }
}
